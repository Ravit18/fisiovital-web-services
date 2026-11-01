package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.AgendaItemResponseDTO;
import com.healthdev.fisiovital.dto.AgendaResponseDTO;
import com.healthdev.fisiovital.dto.RescheduleSessionRequestDTO;
import com.healthdev.fisiovital.dto.SessionResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.SessionMapper;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.NotificationType;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.model.SessionStatus;
import com.healthdev.fisiovital.repository.AvailabilitySlotRepository;
import com.healthdev.fisiovital.repository.SessionRepository;
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
    private final SessionMapper sessionMapper;
    private final MessageSource messageSource;

    /** US11 - Reprogramar una sesion a otro bloque libre del mismo fisioterapeuta. */
    @Transactional
    public SessionResponseDTO reschedule(Long sessionId, RescheduleSessionRequestDTO request) {
        Session session = getOwnedSession(sessionId);
        LocalDateTime now = LocalDateTime.now();
        boolean lateChange = session.isLateChange(now);

        AvailabilitySlot newSlot = availabilityService.getSlot(request.newSlotId());
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
        return sessionMapper.toResponse(session, lateChange);
    }

    /** US12 - Cancelar una sesion reservada. */
    @Transactional
    public SessionResponseDTO cancel(Long sessionId) {
        Session session = getOwnedSession(sessionId);
        LocalDateTime now = LocalDateTime.now();
        boolean lateChange = session.isLateChange(now);
        session.cancel(now);
        sessionRepository.saveAndFlush(session);

        notificationService.notify(session.getPlan().getPhysiotherapist().getUser(), session,
                NotificationType.SESSION_CANCELLED, "notification.session.cancelled",
                session.getPlan().getPatient().getFullName(), session.getStartTime().format(FORMAT));
        return sessionMapper.toResponse(session, lateChange);
    }

    /** US08 - Agenda diaria del fisioterapeuta autenticado. */
    @Transactional(readOnly = true)
    public AgendaResponseDTO agenda(LocalDate date) {
        Physiotherapist physio = profileLookupService.currentPhysiotherapist();
        LocalDate day = date != null ? date : LocalDate.now();
        List<AvailabilitySlot> slots = slotRepository.findByPhysiotherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
                physio.getId(), day.atStartOfDay(), day.plusDays(1).atStartOfDay());

        List<AgendaItemResponseDTO> items = slots.stream().map(this::toAgendaItem).toList();
        boolean hasSessions = items.stream().anyMatch(item -> "BOOKED".equals(item.status()));
        String message = hasSessions ? null
                : messageSource.getMessage("agenda.empty", null, LocaleContextHolder.getLocale());
        return new AgendaResponseDTO(day, items, message);
    }

    /** Sesiones reservadas y futuras del paciente autenticado, de la mas proxima a la mas lejana. */
    @Transactional(readOnly = true)
    public List<SessionResponseDTO> upcomingForCurrentPatient() {
        Patient patient = profileLookupService.currentPatient();
        return sessionRepository.findByPlanPatientIdAndStatusAndSlotStartTimeAfterOrderBySlotStartTimeAsc(
                        patient.getId(), SessionStatus.RESERVED, LocalDateTime.now()).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    private AgendaItemResponseDTO toAgendaItem(AvailabilitySlot slot) {
        Optional<Session> active = sessionRepository.findFirstBySlotIdAndStatusInOrderByIdDesc(
                slot.getId(), SessionStatus.ACTIVE);
        if (active.isPresent()) {
            return sessionMapper.toBookedAgendaItem(slot, active.get());
        }
        boolean cancelled = sessionRepository.findFirstBySlotIdAndStatusInOrderByIdDesc(
                slot.getId(), SessionStatus.CANCELLED_STATES).isPresent();
        return sessionMapper.toFreeAgendaItem(slot, cancelled ? "FREE_CANCELLED" : "FREE");
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
