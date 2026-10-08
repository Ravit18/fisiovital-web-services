package com.healthdev.fisiovital.scheduling.interfaces.rest;

import java.time.LocalDateTime;

public record SlotResource(Long id, LocalDateTime startsAt, int durationMinutes, String status) {
}
