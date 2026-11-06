package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.request.CreateClinicalNoteRequestDTO;
import com.healthdev.fisiovital.dto.response.ClinicalNoteResponseDTO;
import com.healthdev.fisiovital.service.ClinicalNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/clinical-notes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PHYSIOTHERAPIST')")
@Tag(name = "Clinical Notes", description = "Historia clínica: notas del fisioterapeuta sobre el paciente")
public class ClinicalNoteController {

    private final ClinicalNoteService clinicalNoteService;

    @GetMapping
    @Operation(summary = "Ver la historia clínica (notas) de un paciente que trato")
    public ResponseEntity<List<ClinicalNoteResponseDTO>> findByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(clinicalNoteService.findByPatient(patientId));
    }

    @PostMapping
    @Operation(summary = "Agregar una nota a la historia clínica de un paciente que trato")
    public ResponseEntity<ClinicalNoteResponseDTO> create(@PathVariable Long patientId,
                                                          @Valid @RequestBody CreateClinicalNoteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicalNoteService.create(patientId, request));
    }
}
