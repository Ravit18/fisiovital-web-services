package com.healthdev.fisiovital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Nota de la historia clinica: lo que el fisioterapeuta observa del paciente ("Se encontro inflamacion en...",
 * "El paciente cuenta con..."). La comparten los fisioterapeutas que tratan al paciente.
 */
@Entity
@Table(name = "clinical_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalNote {

    public static final int MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "physiotherapist_id", nullable = false)
    private Physiotherapist physiotherapist;

    /** Sesion en la que se hizo la observacion; null si es una nota general del paciente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ClinicalNote(Patient patient, Physiotherapist physiotherapist, Session session, String text) {
        this.patient = patient;
        this.physiotherapist = physiotherapist;
        this.session = session;
        this.text = text.trim();
        this.createdAt = LocalDateTime.now();
    }
}
