package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US14-US16 (EP05) - Historial de citas.
 *
 * Rutas nuevas del plan: /appointments/me (paciente, paginado) y
 * /appointments/physiotherapist/me (fisioterapeuta, ?patientName= para US16).
 * El escenario alternativo de US14 (timeout 503) vive en GlobalExceptionHandlerTest.
 */
class AppointmentHistoryControllerTest extends IntegrationTestBase {

    private void reservarSesion(JsonNode physio, String patient) throws Exception {
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "10:00", 60);
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());
    }

    // ---------------- US14 - Historial del paciente (paginado) ----------------

    @Test
    void us14_exitoso_historialPaginado() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "12:00", 60);

        String patient = newPatientToken();
        JsonNode plan = createPlan(patient, physio.get("profileId").asLong(), 4, 2);
        bookSession(patient, plan.get("id").asLong(), slots.get(0).get("id").asLong());
        bookSession(patient, plan.get("id").asLong(), slots.get(1).get("id").asLong());

        mockMvc.perform(authed(get("/api/v1/appointments/me?page=0&size=1"), patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments.length()").value(1))
                .andExpect(jsonPath("$.appointments[0].status").value("RESERVED"))
                .andExpect(jsonPath("$.appointments[0].patientName").isNotEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.message").value(Matchers.nullValue()));
    }

    @Test
    void us14_error_fisioterapeutaNoPuedeVerlo403() throws Exception {
        String physio = newPhysioToken();

        mockMvc.perform(authed(get("/api/v1/appointments/me"), physio))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción"));
    }

    // ---------------- US15 - Historial del fisioterapeuta ----------------

    @Test
    void us15_exitoso_historialConSusPacientes() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String patient = newPatientToken();
        reservarSesion(physio, patient);

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), physio.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isNotEmpty())
                .andExpect(jsonPath("$.appointments[0].patientName").isNotEmpty())
                .andExpect(jsonPath("$.message").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.page").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.totalElements").value(Matchers.nullValue()));
    }

    @Test
    void us15_error_pacienteNoPuedeVerlo403() throws Exception {
        String patient = newPatientToken();

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), patient))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción"));
    }

    @Test
    void us15_alternativo_sinCitasMuestraMensaje() throws Exception {
        String physio = newPhysioToken();

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), physio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isEmpty())
                .andExpect(jsonPath("$.message").value("Haz tu primera cita para tener historial"));
    }

    // ---------------- US16 - Historial de un paciente por nombre ----------------

    @Test
    void us16_exitoso_buscaPacientePorNombreCompleto() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String fullName = "Paciente Historial " + uniq();
        JsonNode patientNode = signUpPatient(fullName, "historial." + uniq() + "@correo.com");
        reservarSesion(physio, patientNode.get("token").asText());

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), physio.get("token").asText())
                        .param("patientName", fullName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isNotEmpty())
                .andExpect(jsonPath("$.appointments[0].patientName").value(fullName))
                .andExpect(jsonPath("$.page").value(Matchers.nullValue()));
    }

    @Test
    void us16_noMuestraSesionesDelPacienteConOtroFisioterapeuta() throws Exception {
        JsonNode otherPhysio = newPhysio("DEPORTIVA");
        String fullName = "Paciente Privado " + uniq();
        JsonNode patientNode = signUpPatient(fullName, "privado." + uniq() + "@correo.com");
        reservarSesion(otherPhysio, patientNode.get("token").asText());

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), newPhysioToken())
                        .param("patientName", fullName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isEmpty())
                .andExpect(jsonPath("$.message").value("El paciente elegido aún no tiene citas en su historial"));
    }

    @Test
    void us16_error_pacienteInexistente404() throws Exception {
        String physioToken = newPhysioToken();

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), physioToken)
                        .param("patientName", "Nombre Que No Existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El paciente que ha digitado no existe"));
    }

    @Test
    void us16_alternativo_pacienteSinCitas200ConMensaje() throws Exception {
        String physioToken = newPhysioToken();
        String fullName = "Paciente Sin Citas " + uniq();
        signUpPatient(fullName, "sincitas." + uniq() + "@correo.com");

        mockMvc.perform(authed(get("/api/v1/appointments/physiotherapist/me"), physioToken)
                        .param("patientName", fullName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments").isEmpty())
                .andExpect(jsonPath("$.message").value("El paciente elegido aún no tiene citas en su historial"));
    }
}
