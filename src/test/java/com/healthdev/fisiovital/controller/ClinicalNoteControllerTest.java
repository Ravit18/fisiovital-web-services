package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Historia clínica: notas del fisioterapeuta sobre un paciente que trata.
 */
class ClinicalNoteControllerTest extends IntegrationTestBase {

    private static final String NOTE = "Se encontró inflamación leve en el tobillo derecho.";

    @Autowired
    private JdbcTemplate jdbc;

    private String notesUrl(long patientId) {
        return "/api/v1/patients/" + patientId + "/clinical-notes";
    }

    private String noteBody(String text) {
        return objectMapper.createObjectNode().put("text", text).toString();
    }

    /** Paciente con un plan con el fisioterapeuta indicado; devuelve el id de perfil del paciente. */
    private long patientTreatedBy(JsonNode physio) throws Exception {
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        createPlan(patient.get("token").asText(), physio.get("profileId").asLong(), 6, 2);
        return patient.get("profileId").asLong();
    }

    @Test
    void notas_exitoso_elFisioEscribeYLeeLaHistoriaDeSuPaciente() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String token = physio.get("token").asText();
        long patientId = patientTreatedBy(physio);

        mockMvc.perform(authed(post(notesUrl(patientId)), token).content(noteBody("  " + NOTE + "  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientId))
                .andExpect(jsonPath("$.physiotherapistId").value(physio.get("profileId").asLong()))
                .andExpect(jsonPath("$.physiotherapistName").value(physio.get("fullName").asText()))
                .andExpect(jsonPath("$.text").value(NOTE))
                .andExpect(jsonPath("$.createdAt").exists());
        mockMvc.perform(authed(post(notesUrl(patientId)), token).content(noteBody("El paciente cuenta con resonancia.")))
                .andExpect(status().isCreated());

        // La más reciente primero.
        mockMvc.perform(authed(get(notesUrl(patientId)), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("El paciente cuenta con resonancia."))
                .andExpect(jsonPath("$[1].text").value(NOTE));
    }

    @Test
    void notas_alternativo_otroFisioQueTambienLoTrataVeLasNotas() throws Exception {
        JsonNode physioA = newPhysio("DEPORTIVA");
        JsonNode physioB = newPhysio("COLUMNA_POSTURA");
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        String patientToken = patient.get("token").asText();
        createPlan(patientToken, physioA.get("profileId").asLong(), 6, 2);
        createPlan(patientToken, physioB.get("profileId").asLong(), 4, 1);
        long patientId = patient.get("profileId").asLong();

        mockMvc.perform(authed(post(notesUrl(patientId)), physioA.get("token").asText()).content(noteBody(NOTE)))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get(notesUrl(patientId)), physioB.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].physiotherapistName").value(physioA.get("fullName").asText()));
    }

    @Test
    void notas_alternativo_pacienteSinNotasDevuelveListaVacia() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        long patientId = patientTreatedBy(physio);

        mockMvc.perform(authed(get(notesUrl(patientId)), physio.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void notas_error_fisioQueNoTrataAlPaciente403() throws Exception {
        long patientId = patientTreatedBy(newPhysio("DEPORTIVA"));
        String stranger = newPhysioToken();

        mockMvc.perform(authed(get(notesUrl(patientId)), stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Solo puedes ver o escribir la historia clínica de pacientes que tratas"));
        mockMvc.perform(authed(post(notesUrl(patientId)), stranger).content(noteBody(NOTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Solo puedes ver o escribir la historia clínica de pacientes que tratas"));
    }

    @Test
    void notas_error_pacienteInexistente404() throws Exception {
        mockMvc.perform(authed(get(notesUrl(999_999L)), newPhysioToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El paciente no existe"));
    }

    @Test
    void notas_error_textoVacioOLargo400() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String token = physio.get("token").asText();
        long patientId = patientTreatedBy(physio);

        mockMvc.perform(authed(post(notesUrl(patientId)), token).content(noteBody("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este campo es obligatorio"));
        mockMvc.perform(authed(post(notesUrl(patientId)), token).content(noteBody("a".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La nota no puede superar los 1000 caracteres"));
    }

    @Test
    void notas_error_unPacienteNoPuedeLeerHistorias403() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        long patientId = patientTreatedBy(physio);

        mockMvc.perform(authed(get(notesUrl(patientId)), newPatientToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción"));
    }

    // ---------------- Evolución por sesión ----------------

    private String sessionNoteBody(long sessionId, String text) {
        return objectMapper.createObjectNode().put("sessionId", sessionId).put("text", text).toString();
    }

    /** Reserva una sesión del paciente con el fisio y devuelve su id. */
    private long bookedSession(JsonNode physio, String patientToken) throws Exception {
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(3), "09:00", "10:00", 60);
        long planId = createPlan(patientToken, physio.get("profileId").asLong(), 6, 2).get("id").asLong();
        return bookSession(patientToken, planId, slots.get(0).get("id").asLong()).get("id").asLong();
    }

    /**
     * La API no permite reservar en el pasado: para simular una sesión que ya ocurrió, se mueve su bloque
     * directamente en la base de pruebas.
     */
    private LocalDateTime moveToPast(long sessionId, int daysAgo) {
        LocalDateTime start = LocalDateTime.now().minusDays(daysAgo).withHour(9).withMinute(0).withSecond(0).withNano(0);
        jdbc.update("update availability_slots set start_time = ?, end_time = ? "
                + "where id = (select slot_id from sessions where id = ?)", start, start.plusHours(1), sessionId);
        return start;
    }

    @Test
    void evolucion_exitoso_laNotaQuedaLigadaASuSesion() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        String token = physio.get("token").asText();
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        long patientId = patient.get("profileId").asLong();
        long sessionId = bookedSession(physio, patient.get("token").asText());
        LocalDateTime start = moveToPast(sessionId, 2);

        mockMvc.perform(authed(post(notesUrl(patientId)), token).content(sessionNoteBody(sessionId, NOTE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.sessionNumber").value(1))
                .andExpect(jsonPath("$.sessionStartTime").value(start.toString().substring(0, 16) + ":00"));
        mockMvc.perform(authed(post(notesUrl(patientId)), token).content(noteBody("Nota general del paciente.")))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get(notesUrl(patientId)), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].sessionId").isEmpty());
    }

    @Test
    void evolucion_error_sesionDeOtroPaciente400() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode patientA = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        JsonNode patientB = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        long sessionOfA = bookedSession(physio, patientA.get("token").asText());
        createPlan(patientB.get("token").asText(), physio.get("profileId").asLong(), 4, 2);

        mockMvc.perform(authed(post(notesUrl(patientB.get("profileId").asLong())), physio.get("token").asText())
                        .content(sessionNoteBody(sessionOfA, NOTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La sesión no es de este paciente"));
    }

    @Test
    void evolucion_error_sesionQueAtendioOtroFisio403() throws Exception {
        JsonNode physioA = newPhysio("DEPORTIVA");
        JsonNode physioB = newPhysio("COLUMNA_POSTURA");
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        String patientToken = patient.get("token").asText();
        createPlan(patientToken, physioA.get("profileId").asLong(), 6, 2);
        long sessionWithB = bookedSession(physioB, patientToken);

        // A también trata al paciente, pero no puede escribir la evolución de una sesión de B.
        mockMvc.perform(authed(post(notesUrl(patient.get("profileId").asLong())), physioA.get("token").asText())
                        .content(sessionNoteBody(sessionWithB, NOTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Solo puedes registrar la evolución de las sesiones que atendiste"));
    }

    @Test
    void evolucion_error_sesionInexistente404() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        long patientId = patientTreatedBy(physio);

        mockMvc.perform(authed(post(notesUrl(patientId)), physio.get("token").asText())
                        .content(sessionNoteBody(999_999L, NOTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La sesión no existe"));
    }

    @Test
    void evolucion_error_sesionFuturaAunNoSePuedeAnotar422() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        long sessionId = bookedSession(physio, patient.get("token").asText());

        mockMvc.perform(authed(post(notesUrl(patient.get("profileId").asLong())), physio.get("token").asText())
                        .content(sessionNoteBody(sessionId, NOTE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Solo puedes registrar la evolución de una sesión que ya empezó"));
    }

    @Test
    void evolucion_error_sesionCancelada422() throws Exception {
        JsonNode physio = newPhysio("DEPORTIVA");
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        String patientToken = patient.get("token").asText();
        long sessionId = bookedSession(physio, patientToken);
        mockMvc.perform(authed(patch("/api/v1/sessions/" + sessionId + "/cancel"), patientToken))
                .andExpect(status().isOk());

        mockMvc.perform(authed(post(notesUrl(patient.get("profileId").asLong())), physio.get("token").asText())
                        .content(sessionNoteBody(sessionId, NOTE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("No puedes registrar la evolución de una sesión cancelada"));
    }
}
