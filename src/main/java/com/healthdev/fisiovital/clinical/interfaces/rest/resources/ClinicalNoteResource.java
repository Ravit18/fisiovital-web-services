package com.healthdev.fisiovital.clinical.interfaces.rest.resources;

import com.healthdev.fisiovital.clinical.domain.model.ClinicalNote;

import java.time.LocalDateTime;

public record ClinicalNoteResource(Long id, Long sessionId, int sessionNumber, LocalDateTime sessionDate,
                                   String observations, String exercises, Integer painLevel,
                                   LocalDateTime createdAt, LocalDateTime editedAt) {

    public static ClinicalNoteResource from(ClinicalNote n) {
        return new ClinicalNoteResource(n.getId(), n.getSession().getId(), n.getSession().getSessionNumber(),
                n.getSession().getStartTime(), n.getObservations(), n.getExercises(), n.getPainLevel(),
                n.getCreatedAt(), n.getEditedAt());
    }
}
