package com.healthdev.fisiovital.treatment.application;

import com.healthdev.fisiovital.notifications.application.NotificationService;
import com.healthdev.fisiovital.notifications.domain.model.NotificationType;
import com.healthdev.fisiovital.profiles.application.ProfileLookupService;
import com.healthdev.fisiovital.profiles.domain.model.Patient;
import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.scheduling.application.AvailabilityService;
import com.healthdev.fisiovital.scheduling.domain.model.AvailabilitySlot;
import com.healthdev.fisiovital.scheduling.infrastructure.persistence.AvailabilitySlotRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import com.healthdev.fisiovital.treatment.domain.model.SessionStatus;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.SessionRepository;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.AgendaItemResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.AgendaResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.SessionResource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SessionRepository sessionRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final AvailabilityService availabilityService;
    private final ProfileLookupService profileLookupService;
    private final NotificationService notificationService;
    private final MessageSource messageSource;

    /** US11 - Reprogramar una sesion a otro bloque libre del mismo fisioterapeuta. */
    @Transactional
    public SessionResource reschedule(Long sessionId, Long newSlotId) {
        Session session = getOwnedSession(sessionId);
        LocalDateTime now = LocalDateTime.now();
        boolean lateChange = session.isLateChange(now);

        AvailabilitySlot newSlot = availabilityService.getSlot(newSlotId);
        if (!newSlot.belongsTo(session.getPlan().getPhysiotherapist().getId())
                || !newSlot.isFree() || !newSlot.getStartTime().isAfter(now)) {
            throw BusinessException.conflict("slot.not.available");
        }
        session.reschedule(newSlot, now);
        sessionRepository.saveAndFlush(session);

        notificationService.notify(session.getPlan().getPhysiotherapist().getUser(), session,
                NotificationType.SESSION_RESCHEDULED, "notification.session.rescheduled",
                session.getPlan().getPatient().getFullName(), session.getSessionNumber(),
                newSlot.getStartTime().format(FORMAT));
        return SessionResource.from(session, lateChange);
    }

    /** US12 - Cancelar una sesion reservada. */
    @Transactional
    public SessionResource cancel(Long sessionId) {
        Session session = getOwnedSession(sessionId);
        LocalDateTime now = LocalDateTime.now();
        boolean lateChange = session.isLateChange(now);
        session.cancel(now);
        sessionRepository.saveAndFlush(session);

        notificationService.notify(session.getPlan().getPhysiotherapist().getUser(), session,
                NotificationType.SESSION_CANCELLED, "notification.session.cancelled",
                session.getPlan().getPatient().getFullName(), session.getStartTime().format(FORMAT));
        return SessionResource.from(session, lateChange);
    }

    /** US08 - Agenda diaria del fisioterapeuta autenticado. */
    @Transactional(readOnly = true)
    public AgendaResource agenda(LocalDate date) {
        Physiotherapist physio = profileLookupService.currentPhysiotherapist();
        LocalDate day = date != null ? date : LocalDate.now();
        List<AvailabilitySlot> slots = slotRepository.findByPhysiotherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
                physio.getId(), day.atStartOfDay(), day.plusDays(1).atStartOfDay());

        List<AgendaItemResource> items = slots.stream().map(this::toAgendaItem).toList();
        boolean hasSessions = items.stream().anyMatch(item -> "BOOKED".equals(item.status()));
        String message = hasSessions ? null
                : messageSource.getMessage("agenda.empty", null, LocaleContextHolder.getLocale());
        return new AgendaResource(day, items, message);
    }

    private AgendaItemResource toAgendaItem(AvailabilitySlot slot) {
        Optional<Session> active = sessionRepository.findFirstBySlotIdAndStatusInOrderByIdDesc(
                slot.getId(), SessionStatus.ACTIVE);
        if (active.isPresent()) {
            Session s = active.get();
            return new AgendaItemResource(slot.getId(), slot.getStartTime(), slot.getEndTime(), "BOOKED",
                    s.getId(), s.getPlan().getPatient().getFullName(), s.getSessionNumber(),
                    s.getPlan().getTotalSessions(), s.getStatus().name());
        }
        boolean cancelled = sessionRepository.findFirstBySlotIdAndStatusInOrderByIdDesc(
                slot.getId(), SessionStatus.CANCELLED_STATES).isPresent();
        return new AgendaItemResource(slot.getId(), slot.getStartTime(), slot.getEndTime(),
                cancelled ? "FREE_CANCELLED" : "FREE", null, null, null, null, null);
    }

    private Session getOwnedSession(Long sessionId) {
        Patient patient = profileLookupService.currentPatient();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound("session.not.found"));
        if (!session.getPlan().belongsToPatient(patient.getId())) {
            throw BusinessException.forbidden("auth.forbidden");
        }
        return session;
    }
}
