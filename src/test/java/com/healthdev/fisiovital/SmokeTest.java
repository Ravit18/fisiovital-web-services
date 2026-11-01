package com.healthdev.fisiovital;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica que el contexto de pruebas (H2) arranca y el flujo basico de auth funciona. */
class SmokeTest extends IntegrationTestBase {

    @Test
    void contextLoadsAndSignUpPatientWorks() throws Exception {
        JsonNode node = signUpPatient("Smoke User", "smoke." + uniq() + "@correo.com");
        org.junit.jupiter.api.Assertions.assertNotNull(node.get("token"));
        org.junit.jupiter.api.Assertions.assertEquals("PATIENT", node.get("role").asText());
    }

    @Test
    void publicEndpointAndSecurityWork() throws Exception {
        // sin token -> 401 con mensaje i18n
        mockMvc.perform(lang(get("/api/v1/physiotherapists"), "es-419"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void h2CreatesSchemaFromEntities() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nada@correo.com\",\"password\":\"Fisio2026*\"}"))
                .andExpect(status().isUnauthorized());
    }
}
