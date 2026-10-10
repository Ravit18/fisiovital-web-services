package com.healthdev.fisiovital.appointments.interfaces.rest.resources;

import com.healthdev.fisiovital.treatment.domain.model.Session;

import java.time.LocalDateTime;

public record AppointmentResource(Long sessionId, int sessionNumber, int totalSessions, String status,
                                  LocalDateTime startTime, LocalDateTime endTime, Long patientId,
                                  String patientName, Long physiotherapistId, String physiotherapistName,
                                  String specialty, String diagnosis) {

    public static AppointmentResource from(Session s, LocalDateTime now) {
        var plan = s.getPlan();
        return new AppointmentResource(s.getId(), s.getSessionNumber(), plan.getTotalSessions(),
                s.statusAt(now).name(), s.getSlot().getStartTime(), s.getSlot().getEndTime(),
                plan.getPatient().getId(), plan.getPatient().getFullName(), plan.getPhysiotherapist().getId(),
                plan.getPhysiotherapist().getFullName(), plan.getPhysiotherapist().getSpecialty().name(),
                plan.getDiagnosis());
    }
}
