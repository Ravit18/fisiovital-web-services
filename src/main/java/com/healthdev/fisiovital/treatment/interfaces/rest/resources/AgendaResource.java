package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

public record AgendaResource(LocalDate date, List<AgendaItemResource> items, String message) {
}
