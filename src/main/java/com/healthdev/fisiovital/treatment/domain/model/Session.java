package com.healthdev.fisiovital.treatment.domain.model;

import com.healthdev.fisiovital.scheduling.domain.model.AvailabilitySlot;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session {

    public static final int LATE_CHANGE_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private AvailabilitySlot slot;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    public Session(TreatmentPlan plan, AvailabilitySlot slot, int sessionNumber) {
        slot.book();
        this.plan = plan;
        this.slot = slot;
        this.sessionNumber = sessionNumber;
        this.status = SessionStatus.RESERVED;
        this.reminderSent = false;
    }

    public LocalDateTime getStartTime() {
        return slot.getStartTime();
    }

    public boolean isReserved() {
        return status == SessionStatus.RESERVED;
    }

    /** true si faltan menos de 24 horas: aplica la politica de cambios tardios. */
    public boolean isLateChange(LocalDateTime now) {
        return getStartTime().isBefore(now.plusHours(LATE_CHANGE_HOURS));
    }

    /** US11 - Mueve la sesion a otro bloque libre y libera el anterior. */
    public void reschedule(AvailabilitySlot newSlot, LocalDateTime now) {
        if (!isReserved()) {
            throw BusinessException.unprocessable("session.not.reserved");
        }
        if (!getStartTime().isAfter(now)) {
            throw BusinessException.unprocessable("session.reschedule.past");
        }
        newSlot.book();
        this.slot.release();
        this.slot = newSlot;
        this.reminderSent = false;
    }

    /** US12 - Cancela la sesion; con menos de 24 horas queda como cancelacion tardia. */
    public void cancel(LocalDateTime now) {
        if (!isReserved()) {
            throw BusinessException.unprocessable("session.not.reserved");
        }
        if (!getStartTime().isAfter(now)) {
            throw BusinessException.unprocessable("session.past");
        }
        this.status = isLateChange(now) ? SessionStatus.LATE_CANCELLED : SessionStatus.CANCELLED;
        this.slot.release();
    }

    /** US14 - Al registrar la nota clinica la sesion queda como realizada. */
    public void complete() {
        if (!isReserved()) {
            throw BusinessException.unprocessable("session.not.reserved");
        }
        this.status = SessionStatus.COMPLETED;
    }

    public void markReminderSent() {
        this.reminderSent = true;
    }
}
