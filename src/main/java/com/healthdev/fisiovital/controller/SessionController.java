package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.request.RescheduleSessionRequestDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.service.SessionService;
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
    public ResponseEntity<SessionResponseDTO> reschedule(@PathVariable Long sessionId,
                                                      @Valid @RequestBody RescheduleSessionRequestDTO request) {
        return ResponseEntity.ok(sessionService.reschedule(sessionId, request));
    }

    @PatchMapping("/{sessionId}/cancel")
    @Operation(summary = "US12 - Cancelar una sesion")
    public ResponseEntity<SessionResponseDTO> cancel(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.cancel(sessionId));
    }
}
