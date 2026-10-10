package com.healthdev.fisiovital.clinical.infrastructure.persistence;

import com.healthdev.fisiovital.clinical.domain.model.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    boolean existsBySessionId(Long sessionId);

    Optional<ClinicalNote> findFirstBySessionPlanIdOrderByCreatedAtDesc(Long planId);

    @Query("select n from ClinicalNote n where n.session.plan.patient.id = :patientId " +
            "and n.session.plan.physiotherapist.id = :physioId order by n.createdAt desc")
    List<ClinicalNote> findHistory(@Param("patientId") Long patientId, @Param("physioId") Long physioId);
}
