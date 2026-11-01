package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US17 (error) - Sin GEMINI_API_KEY la aplicacion arranca igual y el asistente responde 503. */
@TestPropertySource(properties = "app.ai.gemini.api-key=")
class AssistantControllerWithoutKeyTest extends IntegrationTestBase {

    @Test
    void withoutApiKey_appStartsAndAssistantReturns503() throws Exception {
        mockMvc.perform(authed(post("/api/v1/assistant/messages"), newPatientToken())
                        .content("{\"message\":\"¿Cuál es la política de cancelación?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("El asistente no está disponible"));
    }

    @Test
    void withoutApiKey_safetyRulesStillAnswer() throws Exception {
        mockMvc.perform(authed(post("/api/v1/assistant/messages"), newPatientToken())
                        .content("{\"message\":\"Tengo dolor en el pecho y falta de aire\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SAFETY"));
    }
}
