package com.healthdev.fisiovital.treatment.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.SessionStatus;
import com.healthdev.fisiovital.treatment.domain.model.TreatmentSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TreatmentSessionRepository extends JpaRepository<TreatmentSession, Long> {

    List<TreatmentSession> findByPlanIdOrderBySessionNumberAsc(Long planId);

    List<TreatmentSession> findBySlotId(Long slotId);

    long countByPlanIdAndStatus(Long planId, SessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TreatmentSession s where s.id = :id")
    Optional<TreatmentSession> findByIdForUpdate(@Param("id") Long id);
}
