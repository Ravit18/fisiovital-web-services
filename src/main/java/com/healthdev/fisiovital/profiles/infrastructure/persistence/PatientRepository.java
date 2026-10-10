package com.healthdev.fisiovital.profiles.infrastructure.persistence;

import com.healthdev.fisiovital.profiles.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    boolean existsByFullNameIgnoreCase(String fullName);
}
