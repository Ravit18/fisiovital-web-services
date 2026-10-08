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
@Table(name = "treatment_sessions")
@Getter
@Setter
@NoArgsConstructor
public class TreatmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(nullable = false)
    private boolean rescheduledLate;

    @Column(length = 2000)
    private String observation;

    @Version
    private Long version;

    public TreatmentSession(Long planId, Long slotId, int sessionNumber) {
        this.planId = planId;
        this.slotId = slotId;
        this.sessionNumber = sessionNumber;
    }
}
