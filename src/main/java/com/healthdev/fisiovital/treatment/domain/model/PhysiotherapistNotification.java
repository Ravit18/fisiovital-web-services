package com.healthdev.fisiovital.treatment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "physiotherapist_notifications")
@Getter
@Setter
@NoArgsConstructor
public class PhysiotherapistNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "physiotherapist_id", nullable = false)
    private Long physiotherapistId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PhysiotherapistNotification(Long physiotherapistId, Long sessionId, String message) {
        this.physiotherapistId = physiotherapistId;
        this.sessionId = sessionId;
        this.message = message;
    }
}
