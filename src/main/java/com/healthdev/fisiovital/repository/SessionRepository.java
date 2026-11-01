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

    /** Sesiones reservadas del paciente que aun no ocurren (asistente, US20). */
    List<Session> findByPlanPatientIdAndStatusAndSlotStartTimeAfterOrderBySlotStartTimeAsc(
            Long patientId, SessionStatus status, LocalDateTime after);
}
