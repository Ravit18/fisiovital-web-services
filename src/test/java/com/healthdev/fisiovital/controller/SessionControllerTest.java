package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US11-US12 (EP04) - Reprogramar y cancelar sesiones.
 *
 * Los escenarios alternativos (&lt;24h) usan bloques que empiezan en 2 horas para ejercitar
 * la politica de cambios tardios sin controlar el reloj.
 */
class SessionControllerTest extends IntegrationTestBase {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Publica 2 bloques consecutivos que empiezan `hoursAhead` horas despues de ahora.
     * Si los bloques cruzarian la medianoche, empiezan a las 00:00 del dia siguiente (sigue siendo &lt;24h).
     */
    private JsonNode publishSoonSlots(String physioToken, int hoursAhead) throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(hoursAhead);
        if (!start.plusHours(2).toLocalDate().equals(start.toLocalDate())) {
            start = start.toLocalDate().plusDays(1).atStartOfDay();
        }
        return publishSlots(physioToken, start.format(DateTimeFormatter.ISO_DATE),
                start.format(TIME), start.plusHours(2).format(TIME), 60);
    }

    // ---------------- US11 - Reprogramar sesion ----------------

    @Test
    void us11_exitoso_reprogramaYLiberaBloqueOriginal() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String physioToken = physio.get("token").asText();
        String date = todayPlusDays(3);
        JsonNode slots = publishSlots(physioToken, date, "09:00", "11:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/reschedule"), patient)
                        .content("{\"newSlotId\":" + slots.get(1).get("id").asLong() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.slotId").value(slots.get(1).get("id").asInt()))
                .andExpect(jsonPath("$.lateChange").value(false));

        // El bloque original queda libre y el nuevo pasa a BOOKED en la agenda.
        mockMvc.perform(authed(get("/api/v1/physiotherapists/me/agenda?date=" + date), physioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("FREE"))
                .andExpect(jsonPath("$.items[1].status").value("BOOKED"));
    }

    @Test
    void us11_error_bloqueOcupado409() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String physioToken = physio.get("token").asText();
        JsonNode slots = publishSlots(physioToken, todayPlusDays(3), "09:00", "12:00", 60); // 3 bloques

        String patientA = newPatientToken();
        JsonNode planA = createPlan(patientA, physio.get("profileId").asLong(), 4, 2);
        JsonNode sessionA = bookSession(patientA, planA.get("id").asLong(), slots.get(0).get("id").asLong());

        // Otro paciente ocupa el bloque objetivo.
        String patientB = newPatientToken();
        JsonNode planB = createPlan(patientB, physio.get("profileId").asLong(), 4, 2);
        bookSession(patientB, planB.get("id").asLong(), slots.get(1).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + sessionA.get("id").asLong() + "/reschedule"), patientA)
                        .content("{\"newSlotId\":" + slots.get(1).get("id").asLong() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este horario no está disponible"));
    }

    @Test
    void us11_alternativo_reprogramacionTardiaMarcaLateChange() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSoonSlots(physio.get("token").asText(), 2);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/reschedule"), patient)
                        .content("{\"newSlotId\":" + slots.get(1).get("id").asLong() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.slotId").value(slots.get(1).get("id").asInt()))
                .andExpect(jsonPath("$.lateChange").value(true));
    }

    // ---------------- US12 - Cancelar sesion ----------------

    @Test
    void us12_exitoso_cancelaSesion() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "10:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/cancel"), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.lateChange").value(false));
    }

    @Test
    void us12_error_yaCancelada422() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "10:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/cancel"), patient))
                .andExpect(status().isOk());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/cancel"), patient))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La sesión no está reservada"));
    }

    @Test
    void us12_alternativo_cancelacionTardiaQuedaLATE_CANCELLED() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSoonSlots(physio.get("token").asText(), 2);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        JsonNode session = bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(patch("/api/v1/sessions/" + session.get("id").asLong() + "/cancel"), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LATE_CANCELLED"))
                .andExpect(jsonPath("$.lateChange").value(true));
    }
}
