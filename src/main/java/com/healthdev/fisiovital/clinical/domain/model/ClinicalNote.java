package com.healthdev.fisiovital.clinical.domain.model;

import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinical_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalNote {

    public static final int EDIT_WINDOW_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String observations;

    @Column(columnDefinition = "TEXT")
    private String exercises;

    @Column(name = "pain_level")
    private Integer painLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    public ClinicalNote(Session session, String observations, String exercises, Integer painLevel) {
        this.session = session;
        this.observations = observations.trim();
        this.exercises = exercises;
        this.painLevel = painLevel;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isEditable(LocalDateTime now) {
        return createdAt.plusHours(EDIT_WINDOW_HOURS).isAfter(now);
    }

    /** US14 (alternativo) - Solo se puede editar dentro de las 24 horas; queda registrada la fecha de edicion. */
    public void edit(String observations, String exercises, Integer painLevel, LocalDateTime now) {
        if (!isEditable(now)) {
            throw BusinessException.unprocessable("note.edit.expired");
        }
        this.observations = observations.trim();
        this.exercises = exercises;
        this.painLevel = painLevel;
        this.editedAt = now;
    }
}
