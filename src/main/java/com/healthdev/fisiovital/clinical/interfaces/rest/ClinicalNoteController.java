package com.healthdev.fisiovital.clinical.interfaces.rest;

import com.healthdev.fisiovital.clinical.application.ClinicalNoteService;
import com.healthdev.fisiovital.clinical.interfaces.rest.resources.ClinicalHistoryResource;
import com.healthdev.fisiovital.clinical.interfaces.rest.resources.ClinicalNoteRequestResource;
import com.healthdev.fisiovital.clinical.interfaces.rest.resources.ClinicalNoteResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PHYSIOTHERAPIST')")
@Tag(name = "Clinical Records", description = "Evolucion e historial clinico (US14, US15)")
public class ClinicalNoteController {

    private final ClinicalNoteService clinicalNoteService;

    @PostMapping("/sessions/{sessionId}/clinical-note")
    @Operation(summary = "US14 - Registrar la evolucion de una sesion")
    public ResponseEntity<ClinicalNoteResource> register(@PathVariable Long sessionId,
                                                         @Valid @RequestBody ClinicalNoteRequestResource request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicalNoteService.register(sessionId, request));
    }

    @PutMapping("/clinical-notes/{noteId}")
    @Operation(summary = "US14 - Editar una nota clinica (dentro de las 24 horas)")
    public ResponseEntity<ClinicalNoteResource> edit(@PathVariable Long noteId,
                                                     @Valid @RequestBody ClinicalNoteRequestResource request) {
        return ResponseEntity.ok(clinicalNoteService.edit(noteId, request));
    }

    @GetMapping("/patients/{patientId}/clinical-history")
    @Operation(summary = "US15 - Ver el historial clinico de un paciente")
    public ResponseEntity<ClinicalHistoryResource> history(@PathVariable Long patientId) {
        return ResponseEntity.ok(clinicalNoteService.history(patientId));
    }
}
