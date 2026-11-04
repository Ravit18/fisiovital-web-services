package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.response.NotificationResponseDTO;
import com.healthdev.fisiovital.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Recordatorios y avisos (US13)")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "US13 - Listar mis notificaciones (recordatorios, reprogramaciones, cancelaciones)")
    public ResponseEntity<List<NotificationResponseDTO>> findMine() {
        return ResponseEntity.ok(notificationService.findMine());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar una notificacion como leida")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
