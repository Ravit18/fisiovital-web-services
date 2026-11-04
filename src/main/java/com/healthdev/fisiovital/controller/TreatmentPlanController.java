package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.request.*;
import com.healthdev.fisiovital.dto.response.*;
import com.healthdev.fisiovital.service.TreatmentPlanService;
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
@Tag(name = "Treatment Plans", description = "Planes de tratamiento y reservas (US09, US10)")
public class TreatmentPlanController {

    private final TreatmentPlanService treatmentPlanService;

    @PostMapping
    @Operation(summary = "US09 - Registrar plan de tratamiento")
    public ResponseEntity<TreatmentPlanResponseDTO> create(@Valid @RequestBody CreateTreatmentPlanRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treatmentPlanService.create(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Listar mis planes de tratamiento")
    public ResponseEntity<List<TreatmentPlanResponseDTO>> findMine() {
        return ResponseEntity.ok(treatmentPlanService.findMine());
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Ver uno de mis planes de tratamiento")
    public ResponseEntity<TreatmentPlanResponseDTO> findById(@PathVariable Long planId) {
        return ResponseEntity.ok(treatmentPlanService.findById(planId));
    }

    @PutMapping("/{planId}")
    @Operation(summary = "Editar un plan de tratamiento activo")
    public ResponseEntity<TreatmentPlanResponseDTO> update(@PathVariable Long planId,
                                                        @Valid @RequestBody UpdateTreatmentPlanRequestDTO request) {
        return ResponseEntity.ok(treatmentPlanService.update(planId, request));
    }

    @DeleteMapping("/{planId}")
    @Operation(summary = "Eliminar un plan de tratamiento sin sesiones")
    public ResponseEntity<Void> delete(@PathVariable Long planId) {
        treatmentPlanService.delete(planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/sessions")
    @Operation(summary = "US10 - Reservar una sesion del plan")
    public ResponseEntity<SessionResponseDTO> bookSession(@PathVariable Long planId,
                                                       @Valid @RequestBody BookSessionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treatmentPlanService.bookSession(planId, request));
    }
}
