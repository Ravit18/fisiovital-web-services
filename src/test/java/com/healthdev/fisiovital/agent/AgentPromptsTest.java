package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.model.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 10, 30);

    @Test
    void patientPromptFillsVariablesAndIncludesBookingRules() {
        String prompt = AgentPrompts.system(Role.PATIENT, NOW, Locale.forLanguageTag("es-419"));

        assertThat(prompt)
                .contains("15/11/2026 a las 10:30")
                .contains("espanol latinoamericano (es-419)")
                .contains("bookSession")
                .doesNotContain("{today}", "{language}", "{roleRules}");
    }

    @Test
    void physiotherapistPromptUsesEnglishAndOmitsBookingRules() {
        String prompt = AgentPrompts.system(Role.PHYSIOTHERAPIST, NOW, Locale.US);

        assertThat(prompt)
                .contains("ingles (en-US)")
                .contains("no reservas ni reprogramas")
                .doesNotContain("bookSession");
    }
}
