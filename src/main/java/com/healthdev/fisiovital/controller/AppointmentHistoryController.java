package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.AppointmentHistoryResponseDTO;
import com.healthdev.fisiovital.service.AppointmentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Appointment History", description = "Historial de citas (US14 - US16)")
public class AppointmentHistoryController {

    private final AppointmentHistoryService appointmentHistoryService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "US14 - Ver mi historial de citas como paciente (paginado)")
    public ResponseEntity<AppointmentHistoryResponseDTO> forCurrentPatient(
            @Parameter(description = "Pagina (base 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamano de pagina", example = "5") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(appointmentHistoryService.forCurrentPatient(page, size));
    }

    @GetMapping("/physiotherapist/me")
    @PreAuthorize("hasRole('PHYSIOTHERAPIST')")
    @Operation(summary = "US15/US16 - Historial de citas del fisioterapeuta; con patientName busca un paciente puntual")
    public ResponseEntity<AppointmentHistoryResponseDTO> forCurrentPhysiotherapist(
            @Parameter(description = "Nombre completo del paciente (US16); si se omite, devuelve todos (US15)",
                    example = "Carlos Mendoza")
            @RequestParam(required = false) String patientName) {
        return patientName == null || patientName.isBlank()
                ? ResponseEntity.ok(appointmentHistoryService.forCurrentPhysiotherapist())
                : ResponseEntity.ok(appointmentHistoryService.forPatient(patientName));
    }
}
