package com.healthdev.fisiovital.treatment.interfaces.rest;

import java.time.LocalDateTime;

public record NotificationResource(Long id, Long sessionId, String message, LocalDateTime createdAt) {
}
