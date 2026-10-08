package com.healthdev.fisiovital.scheduling.infrastructure.persistence;

import com.healthdev.fisiovital.scheduling.domain.model.AppointmentSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppointmentSlot s where s.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") Long id);

    List<AppointmentSlot> findByPhysiotherapistIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            Long physiotherapistId, LocalDateTime from, LocalDateTime to);

    List<AppointmentSlot> findByPhysiotherapistIdAndStartsAtBetweenOrderByStartsAtAsc(
            Long physiotherapistId, LocalDateTime from, LocalDateTime to);
}
