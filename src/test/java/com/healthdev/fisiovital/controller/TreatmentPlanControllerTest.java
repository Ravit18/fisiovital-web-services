package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US09-US10 (EP04) - Plan de tratamiento (CRUD) y reserva de sesiones.
 */
class TreatmentPlanControllerTest extends IntegrationTestBase {

    // ---------------- US09 - Registrar plan de tratamiento ----------------

    @Test
    void us09_exitoso_creaPlanActivo() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();

        mockMvc.perform(authed(post("/api/v1/treatment-plans"), patient)
                        .content("""
                                {"physiotherapistId":%d,"diagnosis":"Lumbalgia cronica",
                                 "totalSessions":12,"frequencyPerWeek":2}
                                """.formatted(physio.get("profileId").asLong())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalSessions").value(12))
                .andExpect(jsonPath("$.bookedSessions").value(0))
                .andExpect(jsonPath("$.completedSessions").value(0));
    }

    @Test
    void us09_error_ceroSesiones400() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();

        mockMvc.perform(authed(post("/api/v1/treatment-plans"), patient)
                        .content("""
                                {"physiotherapistId":%d,"diagnosis":"x","totalSessions":0,"frequencyPerWeek":2}
                                """.formatted(physio.get("profileId").asLong())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Indica un número de sesiones entre 1 y 30"))
                .andExpect(jsonPath("$.details[0]").value(Matchers.containsString("totalSessions")));
    }

    @Test
    void us09_alternativo_planActivoDuplicado409() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 8, 2);

        mockMvc.perform(authed(post("/api/v1/treatment-plans"), patient)
                        .content("""
                                {"physiotherapistId":%d,"diagnosis":"otro diagnostico",
                                 "totalSessions":6,"frequencyPerWeek":1}
                                """.formatted(physio.get("profileId").asLong())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString(
                        "Ya tienes un plan activo con este especialista")))
                .andExpect(jsonPath("$.message").value(Matchers.containsString(
                        String.valueOf(plan.get("id").asLong()))));
    }

    // ---------------- US10 - Reservar sesion del plan ----------------

    @Test
    void us10_exitoso_reservaSesion() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 6, 2);

        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + plan.get("id").asLong() + "/sessions"), patient)
                        .content("{\"slotId\":" + slots.get(0).get("id").asLong() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.sessionNumber").value(1))
                .andExpect(jsonPath("$.totalSessions").value(6))
                .andExpect(jsonPath("$.lateChange").value(false));
    }

    @Test
    void us10_error_bloqueOcupadoPorOtroPaciente409() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);
        long slot0 = slots.get(0).get("id").asLong();

        String patientA = newPatientToken();
        JsonNode planA = createPlan(patientA, physio.get("profileId").asLong(), 6, 2);
        bookSession(patientA, planA.get("id").asLong(), slot0);

        String patientB = newPatientToken();
        JsonNode planB = createPlan(patientB, physio.get("profileId").asLong(), 4, 2);

        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planB.get("id").asLong() + "/sessions"), patientB)
                        .content("{\"slotId\":" + slot0 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este horario acaba de ser reservado, elige otro"));
    }

    @Test
    void us10_alternativo_planCompletado422() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 1, 1);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + plan.get("id").asLong() + "/sessions"), patient)
                        .content("{\"slotId\":" + slots.get(1).get("id").asLong() + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Ya reservaste todas las sesiones de tu plan"));
    }

    // ---------------- US10 - Reglas de agenda del paciente ----------------

    @Test
    void us10_error_pacienteYaTieneSesionALaMismaHoraConOtroFisio409() throws Exception {
        String date = todayPlusDays(3);
        JsonNode physioX = newPhysio("DEPORTIVA");
        JsonNode physioY = newPhysio("DEPORTIVA");
        JsonNode slotsX = publishSlots(physioX.get("token").asText(), date, "09:00", "10:00", 60);
        JsonNode slotsY = publishSlots(physioY.get("token").asText(), date, "09:00", "11:00", 60);

        // Un plan activo con cada fisio esta permitido.
        String patient = newPatientToken();
        long planX = createPlan(patient, physioX.get("profileId").asLong(), 4, 2).get("id").asLong();
        long planY = createPlan(patient, physioY.get("profileId").asLong(), 4, 2).get("id").asLong();
        bookSession(patient, planX, slotsX.get(0).get("id").asLong());

        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planY + "/sessions"), patient)
                        .content("{\"slotId\":" + slotsY.get(0).get("id").asLong() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya tienes otra sesión reservada en ese horario"));

        // 10:00 empieza justo cuando termina la de 09:00: no se cruza.
        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planY + "/sessions"), patient)
                        .content("{\"slotId\":" + slotsY.get(1).get("id").asLong() + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void us10_error_frecuenciaSemanalCompleta422() throws Exception {
        String date = todayPlusDays(3);
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode sameDay = publishSlots(physio.get("token").asText(), date, "09:00", "11:00", 60);

        String patient = newPatientToken();
        long planId = createPlan(patient, physio.get("profileId").asLong(), 4, 1).get("id").asLong();
        bookSession(patient, planId, sameDay.get(0).get("id").asLong());

        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planId + "/sessions"), patient)
                        .content("{\"slotId\":" + sameDay.get(1).get("id").asLong() + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("Tu plan permite 1 sesión(es) por semana y esa semana ya está completa"));
    }

    @Test
    void us10_alternativo_frecuenciaPermiteLaSemanaSiguienteYNoCuentaCanceladas() throws Exception {
        String date = todayPlusDays(3);
        JsonNode physio = newPhysio("DEPORTIVA");
        String physioToken = physio.get("token").asText();
        JsonNode sameDay = publishSlots(physioToken, date, "09:00", "11:00", 60);
        JsonNode nextWeek = publishSlots(physioToken, todayPlusDays(10), "09:00", "10:00", 60);

        String patient = newPatientToken();
        long planId = createPlan(patient, physio.get("profileId").asLong(), 4, 1).get("id").asLong();
        long first = bookSession(patient, planId, sameDay.get(0).get("id").asLong()).get("id").asLong();

        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planId + "/sessions"), patient)
                        .content("{\"slotId\":" + nextWeek.get(0).get("id").asLong() + "}"))
                .andExpect(status().isCreated());

        // Al cancelar, la semana vuelve a tener cupo.
        mockMvc.perform(authed(patch("/api/v1/sessions/" + first + "/cancel"), patient))
                .andExpect(status().isOk());
        mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planId + "/sessions"), patient)
                        .content("{\"slotId\":" + sameDay.get(1).get("id").asLong() + "}"))
                .andExpect(status().isCreated());
    }

    // ---------------- CRUD del plan: ver, editar y eliminar ----------------

    @Test
    void plan_ver_exitoso() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 8, 2);

        mockMvc.perform(authed(get("/api/v1/treatment-plans/" + plan.get("id").asLong()), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plan.get("id").asLong()))
                .andExpect(jsonPath("$.totalSessions").value(8));
    }

    @Test
    void plan_ver_error_inexistente404() throws Exception {
        String patient = newPatientToken();

        mockMvc.perform(authed(get("/api/v1/treatment-plans/999999"), patient))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El plan de tratamiento no existe"));
    }

    @Test
    void plan_ver_alternativo_planDeOtroPaciente403() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode plan = createPlan(newPatientToken(), physio.get("profileId").asLong(), 8, 2);

        mockMvc.perform(authed(get("/api/v1/treatment-plans/" + plan.get("id").asLong()), newPatientToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción"));
    }

    @Test
    void plan_editar_exitoso() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 8, 2);

        mockMvc.perform(authed(put("/api/v1/treatment-plans/" + plan.get("id").asLong()), patient)
                        .content("""
                                {"diagnosis":"Lumbalgia aguda","totalSessions":10,"frequencyPerWeek":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Lumbalgia aguda"))
                .andExpect(jsonPath("$.totalSessions").value(10))
                .andExpect(jsonPath("$.frequencyPerWeek").value(3));
    }

    @Test
    void plan_editar_error_sesionesFueraDeRango400() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 8, 2);

        mockMvc.perform(authed(put("/api/v1/treatment-plans/" + plan.get("id").asLong()), patient)
                        .content("""
                                {"diagnosis":"x","totalSessions":31,"frequencyPerWeek":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Indica un número de sesiones entre 1 y 30"));
    }

    @Test
    void plan_editar_alternativo_totalMenorQueReservadas422() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());
        bookSession(patient, plan.get("id").asLong(), slots.get(1).get("id").asLong());

        mockMvc.perform(authed(put("/api/v1/treatment-plans/" + plan.get("id").asLong()), patient)
                        .content("""
                                {"diagnosis":"Lumbalgia","totalSessions":1,"frequencyPerWeek":2}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "No puedes bajar el total a menos de las 2 sesiones ya reservadas"));
    }

    @Test
    void plan_eliminar_exitoso() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();
        long planId = createPlan(patient, physio.get("profileId").asLong(), 8, 2).get("id").asLong();

        mockMvc.perform(authed(delete("/api/v1/treatment-plans/" + planId), patient))
                .andExpect(status().isNoContent());
        mockMvc.perform(authed(get("/api/v1/treatment-plans/" + planId), patient))
                .andExpect(status().isNotFound());
    }

    @Test
    void plan_eliminar_error_conSesiones409() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(authed(delete("/api/v1/treatment-plans/" + plan.get("id").asLong()), patient))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puedes eliminar un plan que ya tiene sesiones"));
    }

    @Test
    void plan_eliminar_alternativo_enIngles() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "11:00", 60);
        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());

        mockMvc.perform(lang(delete("/api/v1/treatment-plans/" + plan.get("id").asLong()), "en-US")
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot delete a plan that already has sessions"));
    }
}
