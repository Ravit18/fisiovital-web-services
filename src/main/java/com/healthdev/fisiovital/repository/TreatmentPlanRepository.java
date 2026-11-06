package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.PlanStatus;
import com.healthdev.fisiovital.model.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    Optional<TreatmentPlan> findFirstByPatientIdAndPhysiotherapistIdAndStatus(
            Long patientId, Long physiotherapistId, PlanStatus status);

    List<TreatmentPlan> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    /** ¿El fisioterapeuta trata o trato al paciente? Da acceso a su historia clinica. */
    boolean existsByPatientIdAndPhysiotherapistId(Long patientId, Long physiotherapistId);
}
