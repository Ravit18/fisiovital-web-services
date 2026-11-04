package com.healthdev.fisiovital;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void corsPreflightFromAngularIsAllowedWithoutToken() throws Exception {
        // config/CorsConfig: el navegador manda OPTIONS sin token antes de un PUT con JWT
        mockMvc.perform(options("/api/v1/treatment-plans/1")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type,accept-language"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

        mockMvc.perform(options("/api/v1/treatment-plans/1")
                        .header("Origin", "https://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void h2CreatesSchemaFromEntities() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nada@correo.com\",\"password\":\"Fisio2026*\"}"))
                .andExpect(status().isUnauthorized());
    }
}
