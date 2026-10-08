package com.healthdev.fisiovital.treatment.interfaces.rest;

import java.time.LocalDateTime;

public record AgendaSlotResource(LocalDateTime startsAt, String status, String patientName,
                                 Integer sessionNumber, Integer totalSessions, String display) {
}
