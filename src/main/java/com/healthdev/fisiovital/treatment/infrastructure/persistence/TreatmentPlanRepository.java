package com.healthdev.fisiovital.treatment.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.PlanStatus;
import com.healthdev.fisiovital.treatment.domain.model.TreatmentPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    boolean existsByPatientIdAndPhysiotherapistIdAndStatus(Long patientId, Long physiotherapistId, PlanStatus status);

    List<TreatmentPlan> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<TreatmentPlan> findByIdAndPatientId(Long id, Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TreatmentPlan p where p.id = :id")
    Optional<TreatmentPlan> findByIdForUpdate(@Param("id") Long id);
}
