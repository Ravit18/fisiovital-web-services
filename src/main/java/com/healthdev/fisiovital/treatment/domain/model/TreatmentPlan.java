package com.healthdev.fisiovital.treatment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "treatment_plans")
@Getter
@Setter
@NoArgsConstructor
public class TreatmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "physiotherapist_id", nullable = false)
    private Long physiotherapistId;

    @Column(nullable = false, length = 300)
    private String diagnosis;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(name = "sessions_per_week", nullable = false)
    private int sessionsPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private Long version;

    public TreatmentPlan(Long patientId, Long physiotherapistId, String diagnosis, int totalSessions,
                         int sessionsPerWeek) {
        this.patientId = patientId;
        this.physiotherapistId = physiotherapistId;
        this.diagnosis = diagnosis;
        this.totalSessions = totalSessions;
        this.sessionsPerWeek = sessionsPerWeek;
    }
}
