package com.healthdev.fisiovital.agent.advisor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AgentGuardrailsTest {

    private final AgentGuardrails guardrails = new AgentGuardrails();

    @ParameterizedTest
    @ValueSource(strings = {
            "Tengo un dolor fuerte en el pecho y me falta el aire",
            "Tengo dolor en el pecho",
            "Siento un dolor muy intenso del pecho",
            "Me falta el aire desde hace rato",
            "Me ahogo al caminar",
            "I have chest pain"})
    void detectsEmergencies(String message) {
        assertThat(guardrails.check(message)).contains("assistant.emergency");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Me duele la espalda baja desde hace una semana",
            "Tengo dolor de rodilla al correr",
            "Puedo cancelar una sesion que ya paso?"})
    void letsRegularQuestionsReachTheModel(String message) {
        assertThat(guardrails.check(message)).isEmpty();
    }
}
