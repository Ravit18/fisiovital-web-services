package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import java.util.List;

public record ProgressResource(Long planId, String status, int totalSessions, long completedSessions,
                               int progressPercent, List<SessionResource> upcomingSessions,
                               String lastObservation) {
}
