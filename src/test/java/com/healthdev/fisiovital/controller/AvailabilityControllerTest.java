package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US06-US07 (EP03 - Disponibilidad).
 * US06 consulta como paciente autenticado; US07 publica como fisioterapeuta.
 */
class AvailabilityControllerTest extends IntegrationTestBase {

    // ---------------- US06 - Consultar horarios disponibles ----------------

    @Test
    void us06_exitoso_devuelveBloquesLibres() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        publishSlots(physio.get("token").asText(), todayPlusDays(2), "09:00", "11:00", 60);

        String patient = newPatientToken();
        mockMvc.perform(authed(get("/api/v1/physiotherapists/" + physio.get("profileId").asText()
                        + "/slots?from=" + todayPlusDays(0) + "&to=" + todayPlusDays(6)), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void us06_error_sinHorariosPublicadosMuestraMensaje() throws Exception {
        JsonNode physio = newPhysio("NEUROLOGICA"); // sin bloques publicados

        String patient = newPatientToken();
        mockMvc.perform(authed(get("/api/v1/physiotherapists/" + physio.get("profileId").asText() + "/slots"), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Este especialista aún no tiene horarios disponibles"));
    }

    @Test
    void us06_alternativo_semanaLlenaMuestraSiguiente() throws Exception {
        JsonNode physio = newPhysio("GERIATRICA");
        // Bloques fuera de la semana consultada para que esta venga vacia.
        publishSlots(physio.get("token").asText(), todayPlusDays(10), "09:00", "11:00", 60);

        String patient = newPatientToken();
        mockMvc.perform(authed(get("/api/v1/physiotherapists/" + physio.get("profileId").asText()
                        + "/slots?from=" + todayPlusDays(0) + "&to=" + todayPlusDays(6)), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextAvailableWeek").isNotEmpty())
                .andExpect(jsonPath("$.message")
                        .value("No hay horarios esta semana; te mostramos la siguiente"));
    }

    // ---------------- US07 - Publicar bloque de disponibilidad ----------------

    @Test
    void us07_exitoso_publicaCuatroBloques() throws Exception {
        String physio = newPhysioToken();

        mockMvc.perform(authed(post("/api/v1/physiotherapists/me/slots"), physio)
                        .content("""
                                {"date":"%s","startTime":"08:00","endTime":"12:00","durationMinutes":60}
                                """.formatted(todayPlusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void us07_error_bloquesQueNoSonDeUnaHora400() throws Exception {
        String physio = newPhysioToken();

        mockMvc.perform(authed(post("/api/v1/physiotherapists/me/slots"), physio)
                        .content("""
                                {"date":"%s","startTime":"08:00","endTime":"12:00","durationMinutes":45}
                                """.formatted(todayPlusDays(3))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void us07_error_fechaPasada422() throws Exception {
        String physio = newPhysioToken();

        mockMvc.perform(authed(post("/api/v1/physiotherapists/me/slots"), physio)
                        .content("""
                                {"date":"2020-01-01","startTime":"08:00","endTime":"10:00","durationMinutes":60}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("No puedes publicar horarios en fechas pasadas"));
    }

    @Test
    void us07_alternativo_cruceConBloqueExistente409() throws Exception {
        String physio = newPhysioToken();
        String date = todayPlusDays(3);
        publishSlots(physio, date, "08:00", "12:00", 60);

        mockMvc.perform(authed(post("/api/v1/physiotherapists/me/slots"), physio)
                        .content("""
                                {"date":"%s","startTime":"10:00","endTime":"12:00","durationMinutes":60}
                                """.formatted(date)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Este horario se cruza con un bloque ya publicado"));
    }
}
