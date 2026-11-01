package com.healthdev.fisiovital;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base de las pruebas de integracion (US01-US16).
 *
 * Arranca el contexto completo con H2 (profile "test") y usa los endpoints reales de
 * registro/login para obtener un JWT valido, de modo que se ejercita el stack completo
 * (filtro JWT -> SecurityContext -> perfil -> servicio). Todos los mensajes se piden en
 * es-419 para que las aserciones de texto sean deterministas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    /** Especialidad reservada para el caso "sin resultados" de US04: ninguna prueba la crea. */
    protected static final String EMPTY_SPECIALTY = "POSTQUIRURGICA";
    protected static final String PASSWORD = "Fisio2026*";
    protected static final Locale ES = Locale.forLanguageTag("es-419");

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    /** Sufijo unico por ejecucion para no chocar con restricciones unicas (correo, colegiatura). */
    protected String uniq() {
        return Long.toString(SEQ.incrementAndGet());
    }

    protected String todayPlusDays(int days) {
        return LocalDate.now().plusDays(days).format(DateTimeFormatter.ISO_DATE);
    }

    // ---------- JSON ----------

    protected JsonNode json(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    // ---------- Requests autenticadas (con Accept-Language es-419) ----------

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder
                .header("Authorization", "Bearer " + token)
                .header("Accept-Language", "es-419")
                .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder lang(MockHttpServletRequestBuilder builder, String tag) {
        return builder.header("Accept-Language", tag).contentType(MediaType.APPLICATION_JSON);
    }

    // ---------- Auth / registro ----------

    protected JsonNode signUpPatient(String fullName, String email) throws Exception {
        return json(mockMvc.perform(lang(post("/api/v1/auth/sign-up/patient"), "es-419")
                .content(objectMapper.createObjectNode()
                        .put("fullName", fullName)
                        .put("email", email)
                        .put("password", PASSWORD)
                        .put("phone", "912345678").toString()))
                .andReturn());
    }

    protected JsonNode signUpPhysio(String fullName, String email, String license, String specialty,
                                    int years) throws Exception {
        return json(mockMvc.perform(lang(post("/api/v1/auth/sign-up/physiotherapist"), "es-419")
                .content(objectMapper.createObjectNode()
                        .put("fullName", fullName)
                        .put("email", email)
                        .put("password", PASSWORD)
                        .put("phone", "987654321")
                        .put("licenseNumber", license)
                        .put("specialty", specialty)
                        .put("yearsOfExperience", years)
                        .put("description", "Fisioterapeuta de prueba").toString()))
                .andReturn());
    }

    /** Crea un paciente y devuelve su token JWT. */
    protected String newPatientToken() throws Exception {
        return signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com").get("token").asText();
    }

    /** Crea un fisioterapeuta y devuelve su token JWT. */
    protected String newPhysioToken() throws Exception {
        return signUpPhysio("Fisio " + uniq(), "fisio." + uniq() + "@correo.com",
                uniq(), "DEPORTIVA", 5).get("token").asText();
    }

    /** Fisioterapeuta con un id de perfil conocido (para endpoints que toman /{id}). */
    protected JsonNode newPhysio(String specialty) throws Exception {
        return signUpPhysio("Fisio " + uniq(), "fisio." + uniq() + "@correo.com",
                uniq(), specialty, 5);
    }

    // ---------- Fixtures de dominio (reutilizadas por varias US) ----------

    /** US07 - Publica un bloque de slots para el fisio autenticado. Devuelve el arreglo de slots. */
    protected JsonNode publishSlots(String physioToken, String date, String start, String end,
                                    int durationMinutes) throws Exception {
        return json(mockMvc.perform(authed(post("/api/v1/physiotherapists/me/slots"), physioToken)
                .content(objectMapper.createObjectNode()
                        .put("date", date)
                        .put("startTime", start)
                        .put("endTime", end)
                        .put("durationMinutes", durationMinutes).toString()))
                .andReturn());
    }

    /** US09 - Registra un plan para el paciente autenticado. */
    protected JsonNode createPlan(String patientToken, Long physiotherapistId, int totalSessions,
                                  int frequencyPerWeek) throws Exception {
        return json(mockMvc.perform(authed(post("/api/v1/treatment-plans"), patientToken)
                .content(objectMapper.createObjectNode()
                        .put("physiotherapistId", physiotherapistId)
                        .put("diagnosis", "Diagnostico de prueba")
                        .put("totalSessions", totalSessions)
                        .put("frequencyPerWeek", frequencyPerWeek).toString()))
                .andReturn());
    }

    /** US10 - Reserva una sesion del plan en el slot indicado. */
    protected JsonNode bookSession(String patientToken, Long planId, Long slotId) throws Exception {
        return json(mockMvc.perform(authed(post("/api/v1/treatment-plans/" + planId + "/sessions"), patientToken)
                .content(objectMapper.createObjectNode().put("slotId", slotId).toString()))
                .andReturn());
    }
}
