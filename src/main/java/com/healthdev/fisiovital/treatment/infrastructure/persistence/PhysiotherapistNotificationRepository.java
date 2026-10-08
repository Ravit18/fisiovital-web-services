package com.healthdev.fisiovital.treatment.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.PhysiotherapistNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhysiotherapistNotificationRepository
        extends JpaRepository<PhysiotherapistNotification, Long> {

    List<PhysiotherapistNotification> findByPhysiotherapistIdOrderByCreatedAtDesc(Long physiotherapistId);
}
