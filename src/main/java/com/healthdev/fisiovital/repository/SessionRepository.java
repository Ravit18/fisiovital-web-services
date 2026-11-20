package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    boolean existsByPlanId(Long planId);

    long countByPlanIdAndStatusIn(Long planId, Collection<SessionStatus> statuses);

    long countByPlanIdAndStatusInAndSlotEndTimeBefore(Long planId, Collection<SessionStatus> statuses,
                                                      LocalDateTime before);

    Optional<Session> findFirstBySlotIdAndStatusInOrderByIdDesc(Long slotId, Collection<SessionStatus> statuses);

    @Query("select s from Session s where s.status = com.healthdev.fisiovital.model.SessionStatus.RESERVED " +
            "and s.reminderSent = false and s.slot.startTime between :from and :to")
    List<Session> findPendingReminders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** US10/US11 - ¿El paciente ya tiene otra sesion RESERVED (con cualquier fisio) que se cruza con [start, end)? */
    @Query("select count(s) > 0 from Session s where s.plan.patient.id = :patientId " +
            "and s.status = com.healthdev.fisiovital.model.SessionStatus.RESERVED " +
            "and s.slot.startTime < :end and s.slot.endTime > :start and s.id <> :excludeSessionId")
    boolean existsPatientOverlap(@Param("patientId") Long patientId, @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end, @Param("excludeSessionId") Long excludeSessionId);

    /** US10/US11 - Sesiones vigentes del plan que empiezan en [from, to) (una semana), sin contar una sesion. */
    @Query("select count(s) from Session s where s.plan.id = :planId and s.status in :statuses " +
            "and s.slot.startTime >= :from and s.slot.startTime < :to and s.id <> :excludeSessionId")
    long countPlanSessionsBetween(@Param("planId") Long planId, @Param("statuses") Collection<SessionStatus> statuses,
                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                  @Param("excludeSessionId") Long excludeSessionId);

    /** Sesiones reservadas del paciente que aun no ocurren (asistente, US20). */
    List<Session> findByPlanPatientIdAndStatusAndSlotStartTimeAfterOrderBySlotStartTimeAsc(
            Long patientId, SessionStatus status, LocalDateTime after);
}
