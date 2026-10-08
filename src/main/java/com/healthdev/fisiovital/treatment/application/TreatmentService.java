package com.healthdev.fisiovital.treatment.application;

import com.healthdev.fisiovital.scheduling.domain.model.AppointmentSlot;
import com.healthdev.fisiovital.scheduling.domain.model.SlotStatus;
import com.healthdev.fisiovital.scheduling.infrastructure.persistence.AppointmentSlotRepository;
import com.healthdev.fisiovital.scheduling.interfaces.rest.SlotResource;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.Patient;
import com.healthdev.fisiovital.treatment.domain.model.Physiotherapist;
import com.healthdev.fisiovital.treatment.domain.model.PhysiotherapistNotification;
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
import com.healthdev.fisiovital.treatment.interfaces.rest.AgendaSlotResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.IdentityResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.NotificationResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.PlanProgressResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.PlanResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.SessionResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TreatmentService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PatientRepository patientRepository;
    private final PhysiotherapistRepository physiotherapistRepository;
    private final AppointmentSlotRepository slotRepository;
    private final TreatmentPlanRepository planRepository;
    private final TreatmentSessionRepository sessionRepository;
    private final PhysiotherapistNotificationRepository notificationRepository;

    @Transactional
    public IdentityResource createPatient(String name) {
        Patient patient = patientRepository.save(new Patient(name.trim()));
        return new IdentityResource(patient.getId(), patient.getName());
    }

    @Transactional
    public IdentityResource createPhysiotherapist(String name) {
        Physiotherapist physiotherapist = physiotherapistRepository.save(new Physiotherapist(name.trim()));
        return new IdentityResource(physiotherapist.getId(), physiotherapist.getName());
    }

    @Transactional
    public SlotResource createSlot(Long physiotherapistId, LocalDateTime startsAt, int durationMinutes) {
        requirePhysiotherapist(physiotherapistId);
        LocalDateTime from = startsAt.minusMinutes(240);
        LocalDateTime to = startsAt.plusMinutes(240);
        boolean overlaps = slotRepository
                .findByPhysiotherapistIdAndStartsAtBetweenOrderByStartsAtAsc(physiotherapistId, from, to)
                .stream()
                .anyMatch(slot -> slot.getStartsAt().isBefore(startsAt.plusMinutes(durationMinutes))
                        && slot.getStartsAt().plusMinutes(slot.getDurationMinutes()).isAfter(startsAt));
        if (overlaps) {
            throw BusinessException.conflict("slot.overlap");
        }

        AppointmentSlot slot = slotRepository.save(new AppointmentSlot(physiotherapistId, startsAt, durationMinutes));
        return toSlotResource(slot);
    }

    @Transactional
    public PlanResource createPlan(Long patientId, Long physiotherapistId, String diagnosis, int totalSessions,
                                   int sessionsPerWeek) {
        requirePatient(patientId);
        requirePhysiotherapistForUpdate(physiotherapistId);
        if (totalSessions < 1 || totalSessions > 30) {
            throw BusinessException.badRequest("plan.sessions.range");
        }
        if (sessionsPerWeek < 1 || sessionsPerWeek > totalSessions) {
            throw BusinessException.badRequest("plan.sessions-per-week.invalid");
        }
        if (planRepository.existsByPatientIdAndPhysiotherapistIdAndStatus(
                patientId, physiotherapistId, PlanStatus.ACTIVE)) {
            throw BusinessException.conflict("plan.active.exists");
        }

        TreatmentPlan plan = planRepository.save(new TreatmentPlan(patientId, physiotherapistId, diagnosis.trim(),
                totalSessions, sessionsPerWeek));
        return toPlanResource(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanResource> listPlans(Long patientId) {
        requirePatient(patientId);
        return planRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toPlanResource)
                .toList();
    }

    @Transactional
    public SessionResource bookSession(Long planId, Long patientId, Long slotId) {
        TreatmentPlan plan = planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> BusinessException.notFound("plan.not-found"));
        if (!plan.getPatientId().equals(patientId)) {
            throw BusinessException.notFound("plan.not-found");
        }
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            throw BusinessException.conflict("plan.finished");
        }

        List<TreatmentSession> planSessions = sessionRepository.findByPlanIdOrderBySessionNumberAsc(planId);
        List<TreatmentSession> activeSessions = planSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED || s.getStatus() == SessionStatus.COMPLETED)
                .toList();
        if (activeSessions.size() >= plan.getTotalSessions()) {
            throw BusinessException.conflict("plan.all-sessions-booked");
        }
        Set<Integer> usedSessionNumbers = activeSessions.stream()
                .map(TreatmentSession::getSessionNumber)
                .collect(Collectors.toSet());
        int nextSessionNumber = 1;
        while (usedSessionNumbers.contains(nextSessionNumber)) {
            nextSessionNumber++;
        }

        AppointmentSlot slot = findSlotForUpdate(slotId);
        if (!slot.getPhysiotherapistId().equals(plan.getPhysiotherapistId())
                || slot.getStatus() != SlotStatus.FREE) {
            throw BusinessException.conflict("slot.just.booked");
        }

        slot.setStatus(SlotStatus.RESERVED);
        slot.setReleasedByCancellation(false);
        TreatmentSession session = sessionRepository.saveAndFlush(
                new TreatmentSession(planId, slot.getId(), nextSessionNumber));
        slotRepository.save(slot);
        return toSessionResource(session, slot);
    }

    @Transactional
    public SessionResource reschedule(Long sessionId, Long patientId, Long newSlotId) {
        TreatmentSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> BusinessException.notFound("session.not-found"));
        TreatmentPlan plan = planRepository.findById(session.getPlanId())
                .orElseThrow(() -> BusinessException.notFound("plan.not-found"));
        if (!plan.getPatientId().equals(patientId)) {
            throw BusinessException.notFound("session.not-found");
        }
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw BusinessException.conflict("session.not-reschedulable");
        }

        Long previousSlotId = session.getSlotId();
        AppointmentSlot target;
        AppointmentSlot previous;
        if (newSlotId.equals(previousSlotId)) {
            target = findSlotForUpdate(newSlotId);
            previous = target;
        } else if (newSlotId < previousSlotId) {
            target = findSlotForUpdate(newSlotId);
            previous = findSlotForUpdate(previousSlotId);
        } else {
            previous = findSlotForUpdate(previousSlotId);
            target = findSlotForUpdate(newSlotId);
        }
        if (!target.getPhysiotherapistId().equals(plan.getPhysiotherapistId())
                || target.getStatus() != SlotStatus.FREE) {
            throw BusinessException.conflict("slot.unavailable");
        }
        target.setStatus(SlotStatus.RESERVED);
        target.setReleasedByCancellation(false);
        previous.setStatus(SlotStatus.FREE);
        previous.setReleasedByCancellation(false);
        session.setSlotId(target.getId());
        session.setRescheduledLate(LocalDateTime.now().plusHours(24).isAfter(previous.getStartsAt()));
        sessionRepository.save(session);
        slotRepository.saveAll(List.of(previous, target));
        notificationRepository.save(new PhysiotherapistNotification(plan.getPhysiotherapistId(), session.getId(),
                "La sesión " + session.getSessionNumber() + " fue reprogramada."));
        return toSessionResource(session, target);
    }

    @Transactional
    public SessionResource cancel(Long sessionId, Long patientId) {
        TreatmentSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> BusinessException.notFound("session.not-found"));
        TreatmentPlan plan = planRepository.findById(session.getPlanId())
                .orElseThrow(() -> BusinessException.notFound("plan.not-found"));
        if (!plan.getPatientId().equals(patientId)) {
            throw BusinessException.notFound("session.not-found");
        }

        AppointmentSlot slot = findSlotForUpdate(session.getSlotId());
        if (slot.getStartsAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("session.already-occurred");
        }
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw BusinessException.conflict("session.not-cancellable");
        }

        boolean late = LocalDateTime.now().plusHours(24).isAfter(slot.getStartsAt());
        session.setStatus(late ? SessionStatus.CANCELLED_LATE : SessionStatus.CANCELLED);
        slot.setStatus(SlotStatus.FREE);
        slot.setReleasedByCancellation(true);
        sessionRepository.save(session);
        slotRepository.save(slot);
        notificationRepository.save(new PhysiotherapistNotification(plan.getPhysiotherapistId(), session.getId(),
                "La sesión " + session.getSessionNumber() + (late ? " fue cancelada tarde." : " fue cancelada.")));
        return toSessionResource(session, slot);
    }

    @Transactional
    public SessionResource complete(Long sessionId, Long physiotherapistId, String observation) {
        TreatmentSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> BusinessException.notFound("session.not-found"));
        TreatmentPlan plan = planRepository.findByIdForUpdate(session.getPlanId())
                .orElseThrow(() -> BusinessException.notFound("plan.not-found"));
        if (!plan.getPhysiotherapistId().equals(physiotherapistId)) {
            throw BusinessException.notFound("session.not-found");
        }
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw BusinessException.conflict("session.not-completable");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setObservation(observation == null || observation.isBlank() ? null : observation.trim());
        sessionRepository.save(session);
        if (sessionRepository.countByPlanIdAndStatus(plan.getId(), SessionStatus.COMPLETED)
                == plan.getTotalSessions()) {
            plan.setStatus(PlanStatus.FINISHED);
            planRepository.save(plan);
        }
        return toSessionResource(session, slotRepository.findById(session.getSlotId())
                .orElseThrow(() -> BusinessException.notFound("slot.not-found")));
    }

    @Transactional(readOnly = true)
    public PlanProgressResource progress(Long planId, Long patientId) {
        TreatmentPlan plan = planRepository.findByIdAndPatientId(planId, patientId)
                .orElseThrow(() -> BusinessException.notFound("plan.not-found"));
        List<TreatmentSession> sessions = sessionRepository.findByPlanIdOrderBySessionNumberAsc(planId);
        int completed = (int) sessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        String lastObservation = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED && s.getObservation() != null)
                .sorted((first, second) -> slotRepository.findById(first.getSlotId())
                        .orElseThrow(() -> BusinessException.notFound("slot.not-found")).getStartsAt()
                        .compareTo(slotRepository.findById(second.getSlotId())
                                .orElseThrow(() -> BusinessException.notFound("slot.not-found")).getStartsAt()))
                .map(TreatmentSession::getObservation)
                .reduce((first, second) -> second)
                .orElse(null);
        List<SessionResource> upcoming = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                .map(s -> toSessionResource(s, slotRepository.findById(s.getSlotId())
                        .orElseThrow(() -> BusinessException.notFound("slot.not-found"))))
                .filter(s -> !s.startsAt().isBefore(LocalDateTime.now()))
                .sorted((first, second) -> first.startsAt().compareTo(second.startsAt()))
                .toList();
        int percentage = completed * 100 / plan.getTotalSessions();
        String message = plan.getStatus() == PlanStatus.FINISHED
                ? "Finalizado"
                : completed + " de " + plan.getTotalSessions() + " sesiones realizadas (" + percentage + " %)";
        return new PlanProgressResource(plan.getId(), planStatusLabel(plan.getStatus()), completed, plan.getTotalSessions(),
                percentage, message, lastObservation, upcoming);
    }

    @Transactional(readOnly = true)
    public AgendaResource agenda(Long physiotherapistId, LocalDate date) {
        requirePhysiotherapist(physiotherapistId);
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        List<AppointmentSlot> daySlots = slotRepository
                .findByPhysiotherapistIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        physiotherapistId, from, to);
        List<AgendaSlotResource> agendaSlots = daySlots.stream()
                .map(this::toAgendaSlot)
                .toList();
        boolean hasSessions = daySlots.stream().anyMatch(slot -> slot.getStatus() == SlotStatus.RESERVED);
        return new AgendaResource(date, hasSessions ? null : "No tienes sesiones programadas para hoy", agendaSlots);
    }

    @Transactional(readOnly = true)
    public List<NotificationResource> notifications(Long physiotherapistId) {
        requirePhysiotherapist(physiotherapistId);
        return notificationRepository.findByPhysiotherapistIdOrderByCreatedAtDesc(physiotherapistId).stream()
                .map(notification -> new NotificationResource(notification.getId(), notification.getSessionId(),
                        notification.getMessage(), notification.getCreatedAt()))
                .toList();
    }

    private AgendaSlotResource toAgendaSlot(AppointmentSlot slot) {
        String time = slot.getStartsAt().format(TIME_FORMAT);
        if (slot.getStatus() == SlotStatus.FREE) {
            String status = slot.isReleasedByCancellation() ? "FREE_AFTER_CANCELLATION" : "FREE";
            String display = slot.isReleasedByCancellation()
                    ? time + " — Libre por cancelación"
                    : time + " — Libre";
            return new AgendaSlotResource(slot.getStartsAt(), status, null, null, null, display);
        }

        TreatmentSession session = sessionRepository.findBySlotId(slot.getId())
                .stream().filter(s -> s.getStatus() == SessionStatus.SCHEDULED
                        || s.getStatus() == SessionStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("session.not-found"));
        TreatmentPlan plan = planRepository.findById(session.getPlanId())
                .orElseThrow(() -> BusinessException.notFound("plan.not-found"));
        Patient patient = requirePatient(plan.getPatientId());
        String status = sessionStatusLabel(session.getStatus());
        String display = time + " — " + patient.getName() + " — sesión " + session.getSessionNumber()
                + " de " + plan.getTotalSessions();
        return new AgendaSlotResource(slot.getStartsAt(), status, patient.getName(),
                session.getSessionNumber(), plan.getTotalSessions(), display);
    }

    private AppointmentSlot findSlotForUpdate(Long slotId) {
        return slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> BusinessException.notFound("slot.not-found"));
    }

    private Patient requirePatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> BusinessException.notFound("patient.not-found"));
    }

    private Physiotherapist requirePhysiotherapist(Long physiotherapistId) {
        return physiotherapistRepository.findById(physiotherapistId)
                .orElseThrow(() -> BusinessException.notFound("physiotherapist.not-found"));
    }

    private Physiotherapist requirePhysiotherapistForUpdate(Long physiotherapistId) {
        return physiotherapistRepository.findByIdForUpdate(physiotherapistId)
                .orElseThrow(() -> BusinessException.notFound("physiotherapist.not-found"));
    }

    private PlanResource toPlanResource(TreatmentPlan plan) {
        return new PlanResource(plan.getId(), plan.getPatientId(), plan.getPhysiotherapistId(), plan.getDiagnosis(),
                plan.getTotalSessions(), plan.getSessionsPerWeek(), planStatusLabel(plan.getStatus()));
    }

    private SlotResource toSlotResource(AppointmentSlot slot) {
        String status = slot.getStatus() == SlotStatus.FREE ? "Libre" : "Reservado";
        return new SlotResource(slot.getId(), slot.getStartsAt(), slot.getDurationMinutes(), status);
    }

    private SessionResource toSessionResource(TreatmentSession session, AppointmentSlot slot) {
        return new SessionResource(session.getId(), session.getPlanId(), slot.getId(), session.getSessionNumber(),
                slot.getStartsAt(), sessionStatusLabel(session.getStatus()), session.isRescheduledLate());
    }

    private String planStatusLabel(PlanStatus status) {
        return status == PlanStatus.ACTIVE ? "Activo" : "Finalizado";
    }

    private String sessionStatusLabel(SessionStatus status) {
        return switch (status) {
            case SCHEDULED -> "Reservada";
            case COMPLETED -> "Realizada";
            case CANCELLED -> "Cancelada";
            case CANCELLED_LATE -> "Cancelada tarde";
        };
    }
}
