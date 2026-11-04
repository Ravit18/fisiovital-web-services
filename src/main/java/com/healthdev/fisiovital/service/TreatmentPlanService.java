package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.request.BookSessionRequestDTO;
import com.healthdev.fisiovital.dto.request.CreateTreatmentPlanRequestDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.dto.response.TreatmentPlanResponseDTO;
import com.healthdev.fisiovital.dto.request.UpdateTreatmentPlanRequestDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.SessionMapper;
import com.healthdev.fisiovital.mapper.TreatmentPlanMapper;
import com.healthdev.fisiovital.model.*;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.repository.SessionRepository;
import com.healthdev.fisiovital.repository.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreatmentPlanService {

    private final TreatmentPlanRepository planRepository;
    private final SessionRepository sessionRepository;
    private final ProfileLookupService profileLookupService;
    private final AvailabilityService availabilityService;
    private final TreatmentPlanMapper treatmentPlanMapper;
    private final SessionMapper sessionMapper;

    /** US09 - Registrar plan de tratamiento. */
    @Transactional
    public TreatmentPlanResponseDTO create(CreateTreatmentPlanRequestDTO request) {
        Patient patient = profileLookupService.currentPatient();
        Physiotherapist physio = profileLookupService.activePhysiotherapist(request.physiotherapistId());

        planRepository.findFirstByPatientIdAndPhysiotherapistIdAndStatus(patient.getId(), physio.getId(),
                PlanStatus.ACTIVE).ifPresent(existing -> {
            throw BusinessException.conflict("plan.active.exists", existing.getId());
        });

        TreatmentPlan plan = planRepository.save(treatmentPlanMapper.toEntity(request, patient, physio));
        return treatmentPlanMapper.toResponse(plan, 0, 0);
    }

    @Transactional
    public List<TreatmentPlanResponseDTO> findMine() {
        Patient patient = profileLookupService.currentPatient();
        return planRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TreatmentPlanResponseDTO findById(Long planId) {
        return toResponse(getOwnedPlan(planId));
    }

    /** Editar un plan activo; el total no puede quedar por debajo de las sesiones ya reservadas. */
    @Transactional
    public TreatmentPlanResponseDTO update(Long planId, UpdateTreatmentPlanRequestDTO request) {
        TreatmentPlan plan = getOwnedPlan(planId);
        if (!plan.isActive()) {
            throw BusinessException.unprocessable("plan.not.active");
        }
        long activeSessions = sessionRepository.countByPlanIdAndStatusIn(planId, SessionStatus.ACTIVE);
        if (request.totalSessions() < activeSessions) {
            throw BusinessException.unprocessable("plan.sessions.below.booked", activeSessions);
        }
        plan.update(request.diagnosis(), request.totalSessions(), request.frequencyPerWeek());
        return toResponse(plan);
    }

    /** Eliminar solo planes sin sesiones (ni reservadas ni canceladas) para no perder el historial. */
    @Transactional
    public void delete(Long planId) {
        TreatmentPlan plan = getOwnedPlan(planId);
        if (sessionRepository.existsByPlanId(planId)) {
            throw BusinessException.conflict("plan.has.sessions");
        }
        planRepository.delete(plan);
    }

    /** US10 - Reservar una sesion del plan en un bloque libre del mismo fisioterapeuta. */
    @Transactional
    public SessionResponseDTO bookSession(Long planId, BookSessionRequestDTO request) {
        TreatmentPlan plan = getOwnedPlan(planId);
        if (!plan.isActive()) {
            throw BusinessException.unprocessable("plan.not.active");
        }
        long activeSessions = sessionRepository.countByPlanIdAndStatusIn(planId, SessionStatus.ACTIVE);
        if (!plan.canBookMore(activeSessions)) {
            throw BusinessException.unprocessable("plan.completed");
        }

        AvailabilitySlot slot = availabilityService.getSlot(request.slotId());
        if (!slot.belongsTo(plan.getPhysiotherapist().getId())) {
            throw BusinessException.badRequest("slot.other.physio");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw BusinessException.conflict("slot.not.available");
        }
        if (!slot.isFree()) {
            throw BusinessException.conflict("slot.just.booked");
        }

        Session session = sessionRepository.saveAndFlush(new Session(plan, slot, (int) activeSessions + 1));
        return sessionMapper.toResponse(session);
    }

    private TreatmentPlan getOwnedPlan(Long planId) {
        Patient patient = profileLookupService.currentPatient();
        TreatmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("plan.not.found"));
        if (!plan.belongsToPatient(patient.getId())) {
            throw BusinessException.forbidden("auth.forbidden");
        }
        return plan;
    }

    /** Si ya se realizaron todas las sesiones, el plan pasa a FINISHED. */
    private TreatmentPlanResponseDTO toResponse(TreatmentPlan plan) {
        long booked = sessionRepository.countByPlanIdAndStatusIn(plan.getId(), SessionStatus.ACTIVE);
        long completed = sessionRepository.countByPlanIdAndStatusInAndSlotEndTimeBefore(
                plan.getId(), SessionStatus.ACTIVE, LocalDateTime.now());
        if (plan.isActive() && completed >= plan.getTotalSessions()) {
            plan.finish();
        }
        return treatmentPlanMapper.toResponse(plan, booked, completed);
    }
}
