package com.healthdev.fisiovital.scheduling.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

/** Horarios libres de un fisioterapeuta. nextAvailableWeek se llena cuando la semana pedida esta llena (US06). */
public record AvailabilityResource(Long physiotherapistId, LocalDate from, LocalDate to,
                                   List<SlotResource> slots, LocalDate nextAvailableWeek, String message) {
}
