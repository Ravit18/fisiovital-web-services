package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.SessionStatus;
import com.healthdev.fisiovital.model.TreatmentPlan;
import com.healthdev.fisiovital.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Reglas que se revisan al poner una sesion en un bloque: al reservar (US10) y al reprogramar (US11).
 * Las usa tambien el asistente (US19/US20), porque llama a los mismos services.
 */
@Component
@RequiredArgsConstructor
public class BookingRules {

    /** Id que no existe: al reservar no hay sesion propia que excluir. */
    public static final long NO_SESSION = -1L;

    private final SessionRepository sessionRepository;

    /**
     * El paciente no puede estar en dos sesiones a la vez, aunque sean con fisioterapeutas distintos
     * (puede tener un plan activo con cada uno).
     */
    public void checkPatientIsFree(TreatmentPlan plan, AvailabilitySlot slot, long excludeSessionId) {
        if (sessionRepository.existsPatientOverlap(plan.getPatient().getId(), slot.getStartTime(),
                slot.getEndTime(), excludeSessionId)) {
            throw BusinessException.conflict("session.patient.overlap");
        }
    }

    /**
     * La frecuencia del plan es un maximo de sesiones por semana (lunes a domingo). Cuentan las vigentes
     * (reservadas o realizadas); las canceladas no.
     */
    public void checkWeeklyFrequency(TreatmentPlan plan, AvailabilitySlot slot, long excludeSessionId) {
        LocalDateTime weekStart = slot.getStartTime().toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        long inWeek = sessionRepository.countPlanSessionsBetween(plan.getId(), SessionStatus.ACTIVE,
                weekStart, weekStart.plusWeeks(1), excludeSessionId);
        if (inWeek >= plan.getFrequencyPerWeek()) {
            throw BusinessException.unprocessable("plan.frequency.exceeded", plan.getFrequencyPerWeek());
        }
    }
}
