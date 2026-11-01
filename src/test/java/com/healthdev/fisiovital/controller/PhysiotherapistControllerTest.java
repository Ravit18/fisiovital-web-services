package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US04-US05 (EP02 - Busqueda de especialistas).
 *
 * US04 usa la especialidad reservada {@link #EMPTY_SPECIALTY} para el caso "sin resultados"
 * (ningun otro test crea fisioterapeutas con esa especialidad). Como Jackson serializa los
 * null, los campos opcionales vacios se asertan con nullValue() y no con doesNotExist().
 */
class PhysiotherapistControllerTest extends IntegrationTestBase {

    private String patientToken() throws Exception {
        return newPatientToken();
    }

    // ---------------- US04 - Filtrar por especialidad ----------------

    @Test
    void us04_exitoso_filtraPorEspecialidad() throws Exception {
        // Garantiza al menos un fisioterapeuta DEORTIVA en la lista.
        newPhysio("DEPORTIVA");

        mockMvc.perform(authed(get("/api/v1/physiotherapists?specialty=DEPORTIVA"), patientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].specialty").value("DEPORTIVA"))
                .andExpect(jsonPath("$.message", Matchers.nullValue()));
    }

    @Test
    void us04_error_sinEspecialistasRespondeMensaje() throws Exception {
        mockMvc.perform(authed(get("/api/v1/physiotherapists?specialty=" + EMPTY_SPECIALTY), patientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("No hay especialistas disponibles para esta especialidad"));
    }

    @Test
    void us04_alternativo_sinFiltroDevuelveTodos() throws Exception {
        newPhysio("COLUMNA_POSTURA");

        mockMvc.perform(authed(get("/api/v1/physiotherapists"), patientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(Matchers.greaterThan(0)));
    }

    @Test
    void us04_validacion_especialidadInexistente400No500() throws Exception {
        mockMvc.perform(authed(get("/api/v1/physiotherapists?specialty=XYZ"), patientToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---------------- US05 - Ver perfil publico ----------------

    @Test
    void us05_exitoso_muestraPerfilPublico() throws Exception {
        JsonNode physio = newPhysio("GERIATRICA");

        mockMvc.perform(authed(get("/api/v1/physiotherapists/" + physio.get("profileId").asText()), patientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty").value("GERIATRICA"))
                .andExpect(jsonPath("$.licenseNumber").isNotEmpty())
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.hasAvailability").exists());
    }

    @Test
    void us05_error_inexistente404() throws Exception {
        mockMvc.perform(authed(get("/api/v1/physiotherapists/999999"), patientToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Perfil no disponible"));
    }

    @Test
    void us05_alternativo_sinHorariosTieneAvailabilityMessage() throws Exception {
        // Fisioterapeuta recien creado, sin bloques publicados.
        JsonNode physio = newPhysio("PEDIATRICA");

        mockMvc.perform(authed(get("/api/v1/physiotherapists/" + physio.get("profileId").asText()), patientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvailability").value(false))
                .andExpect(jsonPath("$.availabilityMessage").value("Sin disponibilidad por ahora"));
    }

    @Test
    void us05_conDisponibilidad_availabilityMessageNulo() throws Exception {
        JsonNode physio = newPhysio("NEUROLOGICA");
        publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);

        mockMvc.perform(authed(get("/api/v1/physiotherapists/" + physio.get("profileId").asText()), patientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAvailability").value(true))
                .andExpect(jsonPath("$.availabilityMessage", Matchers.nullValue()));
    }
}
