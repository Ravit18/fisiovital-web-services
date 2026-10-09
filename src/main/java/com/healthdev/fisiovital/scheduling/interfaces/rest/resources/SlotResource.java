package com.healthdev.fisiovital.scheduling.interfaces.rest.resources;

import com.healthdev.fisiovital.scheduling.domain.model.AvailabilitySlot;

import java.time.LocalDateTime;

public record SlotResource(Long id, LocalDateTime startTime, LocalDateTime endTime, String status) {

    public static SlotResource from(AvailabilitySlot slot) {
        return new SlotResource(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getStatus().name());
    }
}
