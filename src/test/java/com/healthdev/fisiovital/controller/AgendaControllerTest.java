package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US08 (EP03) - Agenda diaria del fisioterapeuta autenticado.
 */
class AgendaControllerTest extends IntegrationTestBase {

    @Test
    void us08_exitoso_muestraSesionesDelDia() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String physioToken = physio.get("token").asText();
        String date = todayPlusDays(3);
        JsonNode slots = publishSlots(physioToken, date, "09:00", "11:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 3, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(get("/api/v1/physiotherapists/me/agenda?date=" + date), physioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(date))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].status").value("BOOKED"))
                .andExpect(jsonPath("$.items[0].patientName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].sessionNumber").value(1))
                .andExpect(jsonPath("$.items[0].totalSessions").value(3))
                .andExpect(jsonPath("$.items[0].sessionStatus").value("RESERVED"))
                .andExpect(jsonPath("$.items[1].status").value("FREE"))
                .andExpect(jsonPath("$.message").value(Matchers.nullValue()));
    }

    @Test
    void us08_error_sinSesionesReservadasMuestraMensaje() throws Exception {
        JsonNode physio = newPhysio("NEUROLOGICA");
        String date = todayPlusDays(3);
        String physioToken = physio.get("token").asText();
        publishSlots(physioToken, date, "09:00", "11:00", 60); // bloques publicados pero sin reservar

        mockMvc.perform(authed(get("/api/v1/physiotherapists/me/agenda?date=" + date), physioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].status").value("FREE"))
                .andExpect(jsonPath("$.message").value("No tienes sesiones programadas para hoy"));
    }

    @Test
    void us08_alternativo_sesionCanceladaMuestraBloqueLiberado() throws Exception {
        JsonNode physio = newPhysio("GERIATRICA");
        String physioToken = physio.get("token").asText();
        String date = todayPlusDays(3);
        JsonNode slots = publishSlots(physioToken, date, "09:00", "10:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 2, 1);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/cancel"), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(authed(get("/api/v1/physiotherapists/me/agenda?date=" + date), physioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("FREE_CANCELLED"));
    }
}
