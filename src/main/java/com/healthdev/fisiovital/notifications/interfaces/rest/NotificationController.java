package com.healthdev.fisiovital.notifications.interfaces.rest;

import com.healthdev.fisiovital.iam.application.CurrentUserProvider;
import com.healthdev.fisiovital.notifications.application.NotificationService;
import com.healthdev.fisiovital.notifications.interfaces.rest.resources.NotificationResource;
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
    private final CurrentUserProvider currentUser;

    @GetMapping
    @Operation(summary = "US13 - Listar mis notificaciones (recordatorios, reprogramaciones, cancelaciones)")
    public ResponseEntity<List<NotificationResource>> findMine() {
        return ResponseEntity.ok(notificationService.findByUser(currentUser.get().userId()).stream()
                .map(NotificationResource::from).toList());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar una notificacion como leida")
    public ResponseEntity<NotificationResource> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(NotificationResource.from(
                notificationService.markAsRead(id, currentUser.get().userId())));
    }
}
