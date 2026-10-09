package com.healthdev.fisiovital.scheduling.infrastructure.persistence;

import com.healthdev.fisiovital.scheduling.domain.model.AvailabilitySlot;
import com.healthdev.fisiovital.scheduling.domain.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    @Query("select count(s) > 0 from AvailabilitySlot s where s.physiotherapist.id = :physioId " +
            "and s.startTime < :end and s.endTime > :start")
    boolean existsOverlap(@Param("physioId") Long physioId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    List<AvailabilitySlot> findByPhysiotherapistIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
            Long physiotherapistId, SlotStatus status, LocalDateTime from, LocalDateTime to);

    Optional<AvailabilitySlot> findFirstByPhysiotherapistIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
            Long physiotherapistId, SlotStatus status, LocalDateTime after);

    List<AvailabilitySlot> findByPhysiotherapistIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long physiotherapistId, LocalDateTime from, LocalDateTime to);

    boolean existsByPhysiotherapistIdAndStatusAndStartTimeAfter(
            Long physiotherapistId, SlotStatus status, LocalDateTime after);
}
