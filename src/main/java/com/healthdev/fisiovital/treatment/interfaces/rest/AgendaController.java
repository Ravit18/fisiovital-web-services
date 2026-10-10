package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.SessionService;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.AgendaResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/physiotherapists/me/agenda")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PHYSIOTHERAPIST')")
@Tag(name = "Availability", description = "Agenda del fisioterapeuta (US08)")
public class AgendaController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "US08 - Ver agenda diaria (por defecto, hoy)")
    public ResponseEntity<AgendaResource> agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.agenda(date));
    }
}
