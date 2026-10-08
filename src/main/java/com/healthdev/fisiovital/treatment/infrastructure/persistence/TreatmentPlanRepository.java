package com.healthdev.fisiovital.treatment.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.PlanStatus;
import com.healthdev.fisiovital.treatment.domain.model.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    Optional<TreatmentPlan> findFirstByPatientIdAndPhysiotherapistIdAndStatus(
            Long patientId, Long physiotherapistId, PlanStatus status);

    List<TreatmentPlan> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<TreatmentPlan> findByPatientIdAndPhysiotherapistIdOrderByCreatedAtDesc(Long patientId, Long physiotherapistId);
}
