package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    /** Historia clinica del paciente, de la nota mas reciente a la mas antigua. */
    List<ClinicalNote> findByPatientIdOrderByCreatedAtDescIdDesc(Long patientId);
}
