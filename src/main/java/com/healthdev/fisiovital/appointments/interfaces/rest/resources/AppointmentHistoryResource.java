package com.healthdev.fisiovital.appointments.interfaces.rest.resources;

import java.util.List;

/** message solo se envia cuando el historial esta vacio. */
public record AppointmentHistoryResource(List<AppointmentResource> appointments, String message) {
}
