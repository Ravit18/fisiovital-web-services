package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import com.healthdev.fisiovital.treatment.domain.model.Session;

import java.time.LocalDateTime;

/** lateChange = true cuando el cambio o la cancelacion se hizo con menos de 24 horas (US11, US12). */
public record SessionResource(Long id, Long planId, int sessionNumber, int totalSessions, String status,
                              Long slotId, LocalDateTime startTime, LocalDateTime endTime,
                              String patientName, String physiotherapistName, boolean lateChange) {

    public static SessionResource from(Session s, boolean lateChange) {
        return new SessionResource(s.getId(), s.getPlan().getId(), s.getSessionNumber(),
                s.getPlan().getTotalSessions(), s.getStatus().name(), s.getSlot().getId(),
                s.getSlot().getStartTime(), s.getSlot().getEndTime(), s.getPlan().getPatient().getFullName(),
                s.getPlan().getPhysiotherapist().getFullName(), lateChange);
    }

    public static SessionResource from(Session s) {
        return from(s, false);
    }
}
