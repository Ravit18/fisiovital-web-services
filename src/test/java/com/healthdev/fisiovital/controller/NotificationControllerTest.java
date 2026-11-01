package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import com.healthdev.fisiovital.service.ReminderScheduler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US13 (EP05) - Notificaciones en la plataforma.
 *
 * El aviso mas determinista es el que recibe el fisioterapeuta cuando el paciente
 * reprograma una sesion; los recordatorios los genera un scheduler y no se usan en tests.
 */
class NotificationControllerTest extends IntegrationTestBase {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ReminderScheduler reminderScheduler;

    /** Fisioterapeuta cuyo paciente acaba de reprogramar una sesion (genera 1 notificacion). */
    private JsonNode fisioConSesionReprogramada() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/reschedule"), patient)
                        .content("{\"newSlotId\":" + slots.get(1).get("id").asLong() + "}"))
                .andExpect(status().isOk());
        return physio;
    }

    @Test
    void us13_exitoso_listaNotificacionesDelFisioterapeuta() throws Exception {
        JsonNode physio = fisioConSesionReprogramada();

        mockMvc.perform(authed(get("/api/v1/notifications"), physio.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("SESSION_RESCHEDULED"))
                .andExpect(jsonPath("$[0].message").value(Matchers.startsWith("El paciente ")))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void us13_alternativo_marcaNotificacionComoLeida() throws Exception {
        JsonNode physio = fisioConSesionReprogramada();
        JsonNode list = json(mockMvc.perform(
                        authed(get("/api/v1/notifications"), physio.get("token").asText())).andReturn());
        long notificationId = list.get(0).get("id").asLong();

        mockMvc.perform(authed(patch("/api/v1/notifications/" + notificationId + "/read"),
                        physio.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void us13_i18n_mismaNotificacionSeLeeEnElIdiomaDeLaPeticion() throws Exception {
        String token = fisioConSesionReprogramada().get("token").asText();

        mockMvc.perform(lang(get("/api/v1/notifications"), "en-US").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value(Matchers.matchesPattern(
                        "Patient .+ rescheduled session 1 to \\d{2}/\\d{2}/\\d{4} 10:00")));

        mockMvc.perform(lang(get("/api/v1/notifications"), "es-419").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value(Matchers.matchesPattern(
                        "El paciente .+ reprogramó la sesión 1 para el \\d{2}/\\d{2}/\\d{4} 10:00")));
    }

    @Test
    void us13_i18n_recordatorioEnIngles() throws Exception {
        // Bloque de 1 hora que empieza en ~2 horas (dentro de la ventana de 24 h del recordatorio).
        LocalDateTime start = LocalDateTime.now().plusHours(2).truncatedTo(ChronoUnit.MINUTES);
        if (!start.plusHours(1).toLocalDate().equals(start.toLocalDate())) {
            start = start.toLocalDate().plusDays(1).atStartOfDay();
        }
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), start.toLocalDate().toString(),
                start.format(TIME), start.plusHours(1).format(TIME), 60);
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        reminderScheduler.sendReminders();

        mockMvc.perform(lang(get("/api/v1/notifications"), "en-US").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SESSION_REMINDER"))
                .andExpect(jsonPath("$[0].message").value("Reminder: you have a session with "
                        + plan.get("physiotherapistName").asText() + " on "
                        + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " at " + start.format(TIME)));
    }

    @Test
    void us13_error_notificacionInexistente404() throws Exception {
        String physio = newPhysioToken();

        mockMvc.perform(authed(patch("/api/v1/notifications/999999/read"), physio))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La notificación no existe"));
    }
}
