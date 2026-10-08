package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.TreatmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TreatmentController {

    private final TreatmentService treatmentService;

    @PostMapping("/treatment-plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResource createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return treatmentService.createPlan(request.patientId(), request.physiotherapistId(), request.diagnosis(),
                request.totalSessions(), request.sessionsPerWeek());
    }

    @GetMapping("/patients/{patientId}/treatment-plans")
    public List<PlanResource> listPlans(@PathVariable Long patientId) {
        return treatmentService.listPlans(patientId);
    }

    @PostMapping("/treatment-plans/{planId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResource bookSession(@PathVariable Long planId, @Valid @RequestBody BookSessionRequest request) {
        return treatmentService.bookSession(planId, request.patientId(), request.slotId());
    }

    @PatchMapping("/sessions/{sessionId}/reschedule")
    public SessionResource reschedule(@PathVariable Long sessionId,
                                      @Valid @RequestBody RescheduleSessionRequest request) {
        return treatmentService.reschedule(sessionId, request.patientId(), request.slotId());
    }

    @PatchMapping("/sessions/{sessionId}/cancel")
    public SessionResource cancel(@PathVariable Long sessionId,
                                  @RequestParam @NotNull Long patientId) {
        return treatmentService.cancel(sessionId, patientId);
    }

    @PatchMapping("/sessions/{sessionId}/complete")
    public SessionResource complete(@PathVariable Long sessionId,
                                     @Valid @RequestBody CompleteSessionRequest request) {
        return treatmentService.complete(sessionId, request.physiotherapistId(), request.observation());
    }

    @GetMapping("/patients/{patientId}/treatment-plans/{planId}/progress")
    public PlanProgressResource progress(@PathVariable Long patientId, @PathVariable Long planId) {
        return treatmentService.progress(planId, patientId);
    }
}
