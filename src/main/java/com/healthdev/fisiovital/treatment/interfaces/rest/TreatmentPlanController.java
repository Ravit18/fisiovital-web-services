package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.TreatmentPlanService;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/treatment-plans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATIENT')")
@Tag(name = "Treatment Plans", description = "Planes de tratamiento, reservas y progreso (US09, US10, US16)")
public class TreatmentPlanController {

    private final TreatmentPlanService treatmentPlanService;

    @PostMapping
    @Operation(summary = "US09 - Registrar plan de tratamiento")
    public ResponseEntity<TreatmentPlanResource> create(@Valid @RequestBody CreateTreatmentPlanResource request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treatmentPlanService.create(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Listar mis planes de tratamiento")
    public ResponseEntity<List<TreatmentPlanResource>> findMine() {
        return ResponseEntity.ok(treatmentPlanService.findMine());
    }

    @PostMapping("/{planId}/sessions")
    @Operation(summary = "US10 - Reservar una sesion del plan")
    public ResponseEntity<SessionResource> bookSession(@PathVariable Long planId,
                                                       @Valid @RequestBody BookSessionResource request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treatmentPlanService.bookSession(planId, request.slotId()));
    }

    @GetMapping("/{planId}/progress")
    @Operation(summary = "US16 - Ver el progreso de mi plan")
    public ResponseEntity<ProgressResource> progress(@PathVariable Long planId) {
        return ResponseEntity.ok(treatmentPlanService.progress(planId));
    }
}
