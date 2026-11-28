package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.agent.dto.AgentAnswer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Gemini a veces devuelve el JSON con una clave repetida o entre bloques de codigo: igual se rescata la respuesta. */
class LenientAnswerConverterTest {

    private final LenientAnswerConverter converter = new LenientAnswerConverter();

    @Test
    void duplicatedKeysStillGiveTheReplyInsteadOfRawJson() {
        AgentAnswer answer = converter.convert("""
                {
                  "action": {"page": null},
                  "choices": [],
                  "reply": "Listo, Carlos. Quedó cancelada tu sesión.",
                  "action": null
                }""");

        assertThat(answer.reply()).isEqualTo("Listo, Carlos. Quedó cancelada tu sesión.");
        assertThat(answer.action()).isNull();
    }

    @Test
    void jsonInsideCodeFencesKeepsChoicesAndAction() {
        AgentAnswer answer = converter.convert("""
                ```json
                {"reply": "Te dejé su perfil.", "choices": ["jue 03/12 · 10:00"],
                 "action": {"page": "PHYSIOTHERAPIST_PROFILE", "physiotherapistId": 2}}
                ```""");

        assertThat(answer.reply()).isEqualTo("Te dejé su perfil.");
        assertThat(answer.choices()).containsExactly("jue 03/12 · 10:00");
        assertThat(answer.action().physiotherapistId()).isEqualTo(2L);
    }

    @Test
    void plainTextIsKeptAsTheReply() {
        assertThat(converter.convert("Hola, ¿en qué te ayudo?").reply()).isEqualTo("Hola, ¿en qué te ayudo?");
    }
}
