package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.SessionService;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.RescheduleSessionResource;
import com.healthdev.fisiovital.treatment.interfaces.rest.resources.SessionResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATIENT')")
@Tag(name = "Sessions", description = "Reprogramar y cancelar sesiones (US11, US12)")
public class SessionController {

    private final SessionService sessionService;

    @PatchMapping("/{sessionId}/reschedule")
    @Operation(summary = "US11 - Reprogramar una sesion")
    public ResponseEntity<SessionResource> reschedule(@PathVariable Long sessionId,
                                                      @Valid @RequestBody RescheduleSessionResource request) {
        return ResponseEntity.ok(sessionService.reschedule(sessionId, request.newSlotId()));
    }

    @PatchMapping("/{sessionId}/cancel")
    @Operation(summary = "US12 - Cancelar una sesion")
    public ResponseEntity<SessionResource> cancel(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.cancel(sessionId));
    }
}
