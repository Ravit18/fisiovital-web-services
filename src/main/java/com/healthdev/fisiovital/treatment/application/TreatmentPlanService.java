package com.healthdev.fisiovital.treatment.application;

import com.healthdev.fisiovital.clinical.infrastructure.persistence.ClinicalNoteRepository;
import com.healthdev.fisiovital.profiles.application.ProfileLookupService;
import com.healthdev.fisiovital.profiles.domain.model.Patient;
import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.scheduling.application.AvailabilityService;
import com.healthdev.fisiovital.scheduling.domain.model.AvailabilitySlot;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.*;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.SessionRepository;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.TreatmentPlanRepository;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.CreateTreatmentPlanResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.ProgressResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.SessionResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.TreatmentPlanResource;
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
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final ProfileLookupService profileLookupService;
    private final AvailabilityService availabilityService;

    /** US09 - Registrar plan de tratamiento. */
    @Transactional
    public TreatmentPlanResource create(CreateTreatmentPlanResource request) {
        Patient patient = profileLookupService.currentPatient();
        Physiotherapist physio = profileLookupService.activePhysiotherapist(request.physiotherapistId());

        planRepository.findFirstByPatientIdAndPhysiotherapistIdAndStatus(patient.getId(), physio.getId(),
                PlanStatus.ACTIVE).ifPresent(existing -> {
            throw BusinessException.conflict("plan.active.exists", existing.getId());
        });

        TreatmentPlan plan = planRepository.save(new TreatmentPlan(patient, physio, request.diagnosis(),
                request.totalSessions(), request.frequencyPerWeek()));
        return TreatmentPlanResource.from(plan, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<TreatmentPlanResource> findMine() {
        Patient patient = profileLookupService.currentPatient();
        return planRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::toResource)
                .toList();
    }

    /** US10 - Reservar una sesion del plan en un bloque libre del mismo fisioterapeuta. */
    @Transactional
    public SessionResource bookSession(Long planId, Long slotId) {
        TreatmentPlan plan = getOwnedPlan(planId);
        if (!plan.isActive()) {
            throw BusinessException.unprocessable("plan.not.active");
        }
        long activeSessions = sessionRepository.countByPlanIdAndStatusIn(planId, SessionStatus.ACTIVE);
        if (!plan.canBookMore(activeSessions)) {
            throw BusinessException.unprocessable("plan.completed");
        }

        AvailabilitySlot slot = availabilityService.getSlot(slotId);
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
        return SessionResource.from(session);
    }

    /** US16 - Ver el progreso del plan. Si se completaron todas las sesiones, el plan pasa a FINISHED. */
    @Transactional
    public ProgressResource progress(Long planId) {
        TreatmentPlan plan = getOwnedPlan(planId);
        long completed = sessionRepository.countByPlanIdAndStatus(planId, SessionStatus.COMPLETED);
        if (plan.isActive() && completed >= plan.getTotalSessions()) {
            plan.finish();
        }
        List<SessionResource> upcoming = sessionRepository
                .findByPlanIdAndStatusAndSlotStartTimeAfterOrderBySlotStartTimeAsc(
                        planId, SessionStatus.RESERVED, LocalDateTime.now())
                .stream().map(SessionResource::from).toList();
        String lastObservation = clinicalNoteRepository.findFirstBySessionPlanIdOrderByCreatedAtDesc(planId)
                .map(note -> note.getObservations())
                .orElse(null);
        return new ProgressResource(plan.getId(), plan.getStatus().name(), plan.getTotalSessions(), completed,
                plan.progressPercent(completed), upcoming, lastObservation);
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

    private TreatmentPlanResource toResource(TreatmentPlan plan) {
        long booked = sessionRepository.countByPlanIdAndStatusIn(plan.getId(), SessionStatus.ACTIVE);
        long completed = sessionRepository.countByPlanIdAndStatus(plan.getId(), SessionStatus.COMPLETED);
        return TreatmentPlanResource.from(plan, booked, completed);
    }
}
