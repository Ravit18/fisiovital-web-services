package com.healthdev.fisiovital.scheduling.domain.model;

import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "availability_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "physiotherapist_id", nullable = false)
    private Physiotherapist physiotherapist;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SlotStatus status;

    /** Control de concurrencia optimista: evita que dos pacientes reserven el mismo bloque (US10). */
    @Version
    private Long version;

    public AvailabilitySlot(Physiotherapist physiotherapist, LocalDateTime startTime, LocalDateTime endTime) {
        this.physiotherapist = physiotherapist;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = SlotStatus.FREE;
    }

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startTime.isBefore(end) && endTime.isAfter(start);
    }

    public boolean isFree() {
        return status == SlotStatus.FREE;
    }

    public boolean belongsTo(Long physiotherapistId) {
        return physiotherapist.getId().equals(physiotherapistId);
    }

    public void book() {
        if (!isFree()) {
            throw BusinessException.conflict("slot.not.available");
        }
        this.status = SlotStatus.BOOKED;
    }

    public void release() {
        this.status = SlotStatus.FREE;
    }
}
