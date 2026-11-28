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
    void physiotherapistPromptUsesEnglishAndHasItsOwnTools() {
        String prompt = AgentPrompts.system(Role.PHYSIOTHERAPIST, NOW, Locale.US);

        assertThat(prompt)
                .contains("ingles (en-US)")
                .contains("myAgenda", "publishAvailability", "writeClinicalNote", "bookSessionForPatient")
                .doesNotContain("createTreatmentPlan", "cancelSession");
    }

    @Test
    void promptSetsAHumanToneAndUsesTheUsersName() {
        String prompt = AgentPrompts.system(Role.PATIENT, "Carlos Rojas", NOW, Locale.forLanguageTag("es-419"));

        assertThat(prompt)
                .contains("Estas conversando con Carlos Rojas (paciente)", "# TONO", "Tutea", "Aqui tienes algunas opciones");
    }

    @Test
    void eachRoleGetsASpecificCapabilityListInsteadOfAGenericAnswer() {
        String patient = AgentPrompts.system(Role.PATIENT, NOW, Locale.forLanguageTag("es-419"));
        String physio = AgentPrompts.system(Role.PHYSIOTHERAPIST, NOW, Locale.forLanguageTag("es-419"));

        assertThat(patient)
                .contains("# QUE PUEDES HACER", "NO respondas en general", "reservarle la sesion",
                        "los 2 libres mas cercanos", "empieza ese flujo de inmediato", "conversando con un paciente",
                        "createTreatmentPlan", "# FLUJO DE PLAN", "# OPCIONES PARA TOCAR");
        assertThat(physio)
                .contains("# QUE PUEDES HACER", "que especialidad le corresponde", "Publicar horarios")
                .doesNotContain("reservarle la sesion");
    }
}
