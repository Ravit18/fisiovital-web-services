package com.healthdev.fisiovital.treatment.interfaces.rest;

import java.util.List;

public record PlanProgressResource(Long planId, String status, int completedSessions,
                                   int totalSessions, int percentage, String message,
                                   String lastObservation, List<SessionResource> upcomingSessions) {
}
