package com.healthdev.fisiovital.treatment.application;

import com.healthdev.fisiovital.scheduling.domain.model.AppointmentSlot;
import com.healthdev.fisiovital.scheduling.domain.model.SlotStatus;
import com.healthdev.fisiovital.scheduling.infrastructure.persistence.AppointmentSlotRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.Patient;
import com.healthdev.fisiovital.treatment.domain.model.Physiotherapist;
import com.healthdev.fisiovital.treatment.domain.model.PlanStatus;
import com.healthdev.fisiovital.treatment.domain.model.SessionStatus;
import com.healthdev.fisiovital.treatment.domain.model.TreatmentPlan;
import com.healthdev.fisiovital.treatment.domain.model.TreatmentSession;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.PatientRepository;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.PhysiotherapistNotificationRepository;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.PhysiotherapistRepository;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.TreatmentPlanRepository;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.TreatmentSessionRepository;
import com.healthdev.fisiovital.treatment.interfaces.rest.AgendaResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.SessionResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PhysiotherapistRepository physiotherapistRepository;

    @Mock
    private AppointmentSlotRepository slotRepository;

    @Mock
    private TreatmentPlanRepository planRepository;

    @Mock
    private TreatmentSessionRepository sessionRepository;

    @Mock
    private PhysiotherapistNotificationRepository notificationRepository;

    @InjectMocks
    private TreatmentService treatmentService;

    private TreatmentPlan plan;
    private AppointmentSlot slot;

    @BeforeEach
    void setUp() {
        plan = new TreatmentPlan(1L, 2L, "Lesión de rodilla derecha", 12, 2);
        plan.setId(3L);
        plan.setStatus(PlanStatus.ACTIVE);
        slot = new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 19, 8, 0), 60);
        slot.setId(4L);
    }

    @Test
    void returnsExactMessageWhenPatientHasNoPlan() {
        BusinessException exception = assertThrows(BusinessException.class, () -> treatmentService.progress(99L, 1L));

        assertEquals("plan.not-found", exception.getMessageKey());
    }

    @Test
    void rejectsCreatingPlanWithZeroSessions() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient("Ana")));
        when(physiotherapistRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(new Physiotherapist("Lic. Rojas")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> treatmentService.createPlan(1L, 2L, "Lesión de rodilla derecha", 0, 2));

        assertEquals("plan.sessions.range", exception.getMessageKey());
    }

    @Test
    void returnsConflictWhenAnActivePlanAlreadyExists() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient("Ana")));
        when(physiotherapistRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(new Physiotherapist("Lic. Rojas")));
        when(planRepository.existsByPatientIdAndPhysiotherapistIdAndStatus(1L, 2L, PlanStatus.ACTIVE))
                .thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> treatmentService.createPlan(1L, 2L, "Lesión de rodilla derecha", 12, 2));

        assertEquals("plan.active.exists", exception.getMessageKey());
    }

    @Test
    void createsAnActiveTreatmentPlan() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient("Ana")));
        when(physiotherapistRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(new Physiotherapist("Lic. Rojas")));
        when(planRepository.existsByPatientIdAndPhysiotherapistIdAndStatus(1L, 2L, PlanStatus.ACTIVE))
                .thenReturn(false);
        when(planRepository.save(any(TreatmentPlan.class))).thenAnswer(invocation -> {
            TreatmentPlan saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        var result = treatmentService.createPlan(1L, 2L, "Lesión de rodilla derecha", 12, 2);

        assertEquals("Activo", result.status());
        assertEquals(12, result.totalSessions());
        assertEquals(2, result.sessionsPerWeek());
    }

    @Test
    void bookingReservesFreeSlotAndUsesNextPlanSessionNumber() {
        when(planRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(plan));
        TreatmentSession first = new TreatmentSession(3L, 10L, 1);
        TreatmentSession second = new TreatmentSession(3L, 11L, 2);
        when(sessionRepository.findByPlanIdOrderBySessionNumberAsc(3L)).thenReturn(List.of(first, second));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
        when(sessionRepository.saveAndFlush(any(TreatmentSession.class))).thenAnswer(invocation -> {
            TreatmentSession session = invocation.getArgument(0);
            session.setId(5L);
            return session;
        });
        when(slotRepository.save(slot)).thenReturn(slot);

        SessionResource result = treatmentService.bookSession(3L, 1L, 4L);

        assertEquals(3, result.sessionNumber());
        assertEquals("Reservada", result.status());
        assertEquals(SlotStatus.RESERVED, slot.getStatus());
        verify(slotRepository).save(slot);
    }

    @Test
    void rejectsAConcurrentlyReservedSlotWithTheStoryMessage() {
        slot.setStatus(SlotStatus.RESERVED);
        when(planRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(plan));
        when(sessionRepository.findByPlanIdOrderBySessionNumberAsc(3L)).thenReturn(List.of());
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> treatmentService.bookSession(3L, 1L, 4L));

        assertEquals("slot.just.booked", exception.getMessageKey());
    }

    @Test
    void rejectsBookingAfterAllPlanSessionsHaveBeenReserved() {
        List<TreatmentSession> sessions = IntStream.rangeClosed(1, 12)
                .mapToObj(number -> new TreatmentSession(3L, (long) number, number))
                .toList();
        when(planRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(plan));
        when(sessionRepository.findByPlanIdOrderBySessionNumberAsc(3L)).thenReturn(sessions);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> treatmentService.bookSession(3L, 1L, 4L));

        assertEquals("plan.all-sessions-booked", exception.getMessageKey());
    }

    @Test
    void agendaShowsOrderedSessionAndCancellationRelease() {
        slot.setStatus(SlotStatus.RESERVED);
        AppointmentSlot cancelled = new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 19, 10, 0), 60);
        cancelled.setId(6L);
        cancelled.setReleasedByCancellation(true);
        when(physiotherapistRepository.findById(2L)).thenReturn(Optional.of(new Physiotherapist("Lic. Rojas")));
        when(slotRepository
                .findByPhysiotherapistIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        2L, LocalDate.of(2026, 10, 19).atStartOfDay(), LocalDate.of(2026, 10, 20).atStartOfDay()))
                .thenReturn(List.of(slot, cancelled));
        TreatmentSession session = new TreatmentSession(3L, 4L, 3);
        session.setStatus(SessionStatus.SCHEDULED);
        when(sessionRepository.findBySlotId(4L)).thenReturn(List.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(new Patient("Carlos Mendoza")));

        AgendaResource result = treatmentService.agenda(2L, LocalDate.of(2026, 10, 19));

        assertEquals("08:00 — Carlos Mendoza — sesión 3 de 12", result.slots().get(0).display());
        assertEquals("10:00 — Libre por cancelación", result.slots().get(1).display());
    }

    @Test
    void agendaExplainsWhenThereAreNoSessions() {
        when(physiotherapistRepository.findById(2L)).thenReturn(Optional.of(new Physiotherapist("Lic. Rojas")));
        when(slotRepository
                .findByPhysiotherapistIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        2L, LocalDate.of(2026, 10, 19).atStartOfDay(), LocalDate.of(2026, 10, 20).atStartOfDay()))
                .thenReturn(List.of());

        AgendaResource result = treatmentService.agenda(2L, LocalDate.of(2026, 10, 19));

        assertEquals("No tienes sesiones programadas para hoy", result.message());
        assertEquals(List.of(), result.slots());
    }

    @Test
    void cancellationWithin24HoursMarksSessionLateAndFreesSlot() {
        slot.setStartsAt(LocalDateTime.now().plusHours(12));
        TreatmentSession session = new TreatmentSession(3L, 4L, 1);
        session.setId(5L);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
        when(sessionRepository.save(session)).thenReturn(session);
        when(slotRepository.save(slot)).thenReturn(slot);

        SessionResource result = treatmentService.cancel(5L, 1L);

        assertEquals("Cancelada tarde", result.status());
        assertEquals(SlotStatus.FREE, slot.getStatus());
        assertEquals(true, slot.isReleasedByCancellation());
        verify(notificationRepository).save(any());
    }

    @Test
    void cancellationTwoDaysAheadIsNotLate() {
        slot.setStartsAt(LocalDateTime.now().plusDays(2));
        TreatmentSession session = new TreatmentSession(3L, 4L, 1);
        session.setId(5L);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
        when(sessionRepository.save(session)).thenReturn(session);
        when(slotRepository.save(slot)).thenReturn(slot);

        SessionResource result = treatmentService.cancel(5L, 1L);

        assertEquals("Cancelada", result.status());
        assertEquals(SlotStatus.FREE, slot.getStatus());
        assertEquals(true, slot.isReleasedByCancellation());
    }

    @Test
    void cancellationRejectsASessionThatAlreadyOccurred() {
        slot.setStartsAt(LocalDateTime.now().minusDays(1));
        TreatmentSession session = new TreatmentSession(3L, 4L, 1);
        session.setId(5L);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> treatmentService.cancel(5L, 1L));

        assertEquals("session.already-occurred", exception.getMessageKey());
    }

    @Test
    void reschedulingMovesSessionAndNotifiesPhysiotherapist() {
        slot.setStatus(SlotStatus.RESERVED);
        AppointmentSlot target = new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 23, 18, 0), 60);
        target.setId(6L);
        TreatmentSession session = new TreatmentSession(3L, 4L, 1);
        session.setId(5L);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
        when(slotRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(target));

        SessionResource result = treatmentService.reschedule(5L, 1L, 6L);

        assertEquals(6L, result.slotId());
        assertEquals("Reservada", result.status());
        assertEquals(SlotStatus.FREE, slot.getStatus());
        assertEquals(SlotStatus.RESERVED, target.getStatus());
        verify(notificationRepository).save(any());
    }

    @Test
    void reschedulingWithin24HoursIsMarkedLate() {
        slot.setStatus(SlotStatus.RESERVED);
        slot.setStartsAt(LocalDateTime.now().plusHours(12));
        AppointmentSlot target = new AppointmentSlot(2L, LocalDateTime.now().plusDays(2), 60);
        target.setId(6L);
        TreatmentSession session = new TreatmentSession(3L, 4L, 1);
        session.setId(5L);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
        when(slotRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(target));

        SessionResource result = treatmentService.reschedule(5L, 1L, 6L);

        assertEquals(true, result.rescheduledLate());
    }

    @Test
    void rejectsReschedulingToAnOccupiedSlot() {
        slot.setStatus(SlotStatus.RESERVED);
        TreatmentSession session = new TreatmentSession(3L, 10L, 1);
        session.setId(5L);
        AppointmentSlot previous = new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 21, 18, 0), 60);
        previous.setId(10L);
        previous.setStatus(SlotStatus.RESERVED);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(slotRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(slot));
        when(slotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(previous));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> treatmentService.reschedule(5L, 1L, 4L));

        assertEquals("slot.unavailable", exception.getMessageKey());
    }

    @Test
    void progressReportsCompletedSessionsPercentageAndUpcomingAppointments() {
        when(planRepository.findByIdAndPatientId(3L, 1L)).thenReturn(Optional.of(plan));
        TreatmentSession first = completedSession(1, 10L, "Primera observación");
        TreatmentSession second = completedSession(2, 11L, "Segunda observación");
        TreatmentSession third = completedSession(3, 12L, "Última observación");
        TreatmentSession upcoming = new TreatmentSession(3L, 13L, 4);
        upcoming.setId(14L);
        when(sessionRepository.findByPlanIdOrderBySessionNumberAsc(3L))
                .thenReturn(List.of(first, second, third, upcoming));
        when(slotRepository.findById(10L)).thenReturn(Optional.of(
                new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 1, 8, 0), 60)));
        when(slotRepository.findById(11L)).thenReturn(Optional.of(
                new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 2, 8, 0), 60)));
        when(slotRepository.findById(12L)).thenReturn(Optional.of(
                new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 3, 8, 0), 60)));
        when(slotRepository.findById(13L)).thenReturn(Optional.of(
                new AppointmentSlot(2L, LocalDateTime.of(2026, 10, 19, 8, 0), 60)));

        var result = treatmentService.progress(3L, 1L);

        assertEquals(3, result.completedSessions());
        assertEquals(25, result.percentage());
        assertEquals("3 de 12 sesiones realizadas (25 %)", result.message());
        assertEquals("Última observación", result.lastObservation());
        assertEquals(1, result.upcomingSessions().size());
    }

    @Test
    void completingAllPlanSessionsFinishesThePlanAndKeepsTheLastObservation() {
        plan.setTotalSessions(1);
        TreatmentSession session = new TreatmentSession(3L, 4L, 1);
        session.setId(5L);
        when(sessionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(session));
        when(planRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(plan));
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionRepository.countByPlanIdAndStatus(3L, SessionStatus.COMPLETED)).thenReturn(1L);
        when(slotRepository.findById(4L)).thenReturn(Optional.of(slot));

        SessionResource result = treatmentService.complete(5L, 2L, "Movilidad mejorada");

        assertEquals("Realizada", result.status());
        assertEquals("Movilidad mejorada", session.getObservation());
        assertEquals(PlanStatus.FINISHED, plan.getStatus());
        verify(planRepository).save(plan);
    }

    private TreatmentSession completedSession(int sessionNumber, Long slotId, String observation) {
        TreatmentSession session = new TreatmentSession(3L, slotId, sessionNumber);
        session.setStatus(SessionStatus.COMPLETED);
        session.setObservation(observation);
        return session;
    }
}
