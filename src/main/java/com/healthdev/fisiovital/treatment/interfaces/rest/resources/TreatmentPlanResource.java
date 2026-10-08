package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import com.healthdev.fisiovital.treatment.domain.model.TreatmentPlan;

import java.time.LocalDateTime;

public record TreatmentPlanResource(Long id, Long patientId, String patientName, Long physiotherapistId,
                                    String physiotherapistName, String diagnosis, int totalSessions,
                                    int frequencyPerWeek, String status, LocalDateTime createdAt,
                                    long bookedSessions, long completedSessions) {

    public static TreatmentPlanResource from(TreatmentPlan plan, long booked, long completed) {
        return new TreatmentPlanResource(plan.getId(), plan.getPatient().getId(), plan.getPatient().getFullName(),
                plan.getPhysiotherapist().getId(), plan.getPhysiotherapist().getFullName(), plan.getDiagnosis(),
                plan.getTotalSessions(), plan.getFrequencyPerWeek(), plan.getStatus().name(), plan.getCreatedAt(),
                booked, completed);
    }
}
