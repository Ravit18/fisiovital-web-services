package com.healthdev.fisiovital.treatment.interfaces.rest;

public record PlanResource(Long id, Long patientId, Long physiotherapistId, String diagnosis,
                           int totalSessions, int sessionsPerWeek, String status) {
}
