package com.healthdev.fisiovital.appointments.interfaces.rest;

import com.healthdev.fisiovital.appointments.application.AppointmentHistoryService;
import com.healthdev.fisiovital.appointments.interfaces.rest.resources.AppointmentHistoryResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Appointment History", description = "Historial de citas (US14 - US16)")
public class AppointmentHistoryController {

    private final AppointmentHistoryService appointmentHistoryService;

    @GetMapping("/patients/me/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "US14 - Ver mi historial de citas como paciente")
    public ResponseEntity<AppointmentHistoryResource> forCurrentPatient() {
        return ResponseEntity.ok(appointmentHistoryService.forCurrentPatient());
    }

    @GetMapping("/physiotherapists/me/appointments")
    @PreAuthorize("hasRole('PHYSIOTHERAPIST')")
    @Operation(summary = "US15 - Ver mi historial de citas con todos mis pacientes")
    public ResponseEntity<AppointmentHistoryResource> forCurrentPhysiotherapist() {
        return ResponseEntity.ok(appointmentHistoryService.forCurrentPhysiotherapist());
    }

    @GetMapping("/patients/appointments")
    @PreAuthorize("hasRole('PHYSIOTHERAPIST')")
    @Operation(summary = "US16 - Ver el historial de citas de un paciente por su nombre completo")
    public ResponseEntity<AppointmentHistoryResource> forPatient(@RequestParam String fullName) {
        return ResponseEntity.ok(appointmentHistoryService.forPatient(fullName));
    }
}
