package com.healthdev.fisiovital.notifications.interfaces.rest.resources;

import com.healthdev.fisiovital.notifications.domain.model.Notification;

import java.time.LocalDateTime;

public record NotificationResource(Long id, String type, String message, boolean read, LocalDateTime createdAt) {

    public static NotificationResource from(Notification n) {
        return new NotificationResource(n.getId(), n.getType().name(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
