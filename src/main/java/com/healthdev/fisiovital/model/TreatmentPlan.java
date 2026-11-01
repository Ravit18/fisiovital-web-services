package com.healthdev.fisiovital.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "treatment_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreatmentPlan {

    public static final int MIN_SESSIONS = 1;
    public static final int MAX_SESSIONS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "physiotherapist_id", nullable = false)
    private Physiotherapist physiotherapist;

    @Column(nullable = false)
    private String diagnosis;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(name = "frequency_per_week", nullable = false)
    private int frequencyPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlanStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TreatmentPlan(Patient patient, Physiotherapist physiotherapist, String diagnosis,
                         int totalSessions, int frequencyPerWeek) {
        this.patient = patient;
        this.physiotherapist = physiotherapist;
        this.diagnosis = diagnosis.trim();
        this.totalSessions = totalSessions;
        this.frequencyPerWeek = frequencyPerWeek;
        this.status = PlanStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == PlanStatus.ACTIVE;
    }

    public boolean belongsToPatient(Long patientId) {
        return patient.getId().equals(patientId);
    }

    /** Hay cupo si las sesiones vigentes (reservadas o realizadas) no alcanzan el total del plan. */
    public boolean canBookMore(long activeSessions) {
        return activeSessions < totalSessions;
    }

    /** Solo cambian los datos clinicos; el paciente y el fisioterapeuta son fijos. */
    public void update(String diagnosis, int totalSessions, int frequencyPerWeek) {
        this.diagnosis = diagnosis.trim();
        this.totalSessions = totalSessions;
        this.frequencyPerWeek = frequencyPerWeek;
    }

    public void finish() {
        this.status = PlanStatus.FINISHED;
    }
}
