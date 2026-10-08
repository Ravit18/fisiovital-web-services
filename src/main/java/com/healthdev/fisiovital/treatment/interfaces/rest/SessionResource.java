package com.healthdev.fisiovital.treatment.interfaces.rest;

import java.time.LocalDateTime;

public record SessionResource(Long id, Long planId, Long slotId, int sessionNumber,
                              LocalDateTime startsAt, String status, boolean rescheduledLate) {
}
