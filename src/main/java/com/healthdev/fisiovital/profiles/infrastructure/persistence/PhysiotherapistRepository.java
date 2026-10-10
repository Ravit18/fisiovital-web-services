package com.healthdev.fisiovital.profiles.infrastructure.persistence;

import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.profiles.domain.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysiotherapistRepository extends JpaRepository<Physiotherapist, Long> {

    Optional<Physiotherapist> findByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Physiotherapist> findByActiveTrueOrderByYearsOfExperienceDesc();

    List<Physiotherapist> findBySpecialtyAndActiveTrueOrderByYearsOfExperienceDesc(Specialty specialty);
}
