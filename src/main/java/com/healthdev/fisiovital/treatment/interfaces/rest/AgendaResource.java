package com.healthdev.fisiovital.treatment.interfaces.rest;

import java.time.LocalDate;
import java.util.List;

public record AgendaResource(LocalDate date, String message, List<AgendaSlotResource> slots) {
}
