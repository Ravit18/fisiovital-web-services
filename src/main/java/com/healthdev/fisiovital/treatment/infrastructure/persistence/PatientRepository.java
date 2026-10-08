package com.healthdev.fisiovital.treatment.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
