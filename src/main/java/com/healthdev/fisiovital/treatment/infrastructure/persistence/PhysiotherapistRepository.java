package com.healthdev.fisiovital.treatment.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.Physiotherapist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PhysiotherapistRepository extends JpaRepository<Physiotherapist, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Physiotherapist p where p.id = :id")
    Optional<Physiotherapist> findByIdForUpdate(@Param("id") Long id);
}
