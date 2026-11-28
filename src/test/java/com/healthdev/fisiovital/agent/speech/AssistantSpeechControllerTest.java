package com.healthdev.fisiovital.agent.speech;

import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Voz del asistente: Gemini TTS se simula; se prueba el WAV, la validacion y el 503 cuando falla la cuota. */
class AssistantSpeechControllerTest extends IntegrationTestBase {

    @MockitoBean
    private SpeechGenerator generator;

    @Test
    void speech_returnsAPlayableWav() throws Exception {
        when(generator.generate(anyString())).thenReturn(new SpeechGenerator.Pcm(new byte[]{1, 2, 3, 4}, 24_000));

        byte[] wav = mockMvc.perform(authed(post("/api/v1/assistant/speech"), newPatientToken())
                        .content("{\"text\": \"Tu sesión quedó reservada.\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(wav, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(wav, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WAVE");
        assertThat(wav).hasSize(44 + 4);
        verify(generator).generate("Tu sesión quedó reservada.");
    }

    @Test
    void speech_whenGeminiFails_returns503SoTheBrowserVoiceTakesOver() throws Exception {
        when(generator.generate(anyString())).thenThrow(new RuntimeException("429 quota exceeded"));

        mockMvc.perform(authed(post("/api/v1/assistant/speech"), newPatientToken())
                        .content("{\"text\": \"Hola\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("La voz no está disponible en este momento"));
    }

    @Test
    void speech_blankText_returns400WithoutCallingGemini() throws Exception {
        mockMvc.perform(authed(post("/api/v1/assistant/speech"), newPatientToken()).content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(generator);
    }

    @Test
    void speech_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/speech").contentType("application/json").content("{\"text\": \"Hola\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void geminiRateIsReadFromTheMimeType() {
        assertThat(GeminiSpeechGenerator.rateOf("audio/L16;codec=pcm;rate=24000")).isEqualTo(24_000);
        assertThat(GeminiSpeechGenerator.rateOf("audio/L16")).isEqualTo(24_000);
    }
}
