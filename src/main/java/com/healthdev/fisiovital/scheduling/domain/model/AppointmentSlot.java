package com.healthdev.fisiovital.scheduling.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_slots", uniqueConstraints =
        @UniqueConstraint(name = "uk_slot_physio_start", columnNames = {"physiotherapist_id", "starts_at"}))
@Getter
@Setter
@NoArgsConstructor
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "physiotherapist_id", nullable = false)
    private Long physiotherapistId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private int durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status = SlotStatus.FREE;

    @Column(nullable = false)
    private boolean releasedByCancellation;

    @Version
    private Long version;

    public AppointmentSlot(Long physiotherapistId, LocalDateTime startsAt, int durationMinutes) {
        this.physiotherapistId = physiotherapistId;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
    }
}
