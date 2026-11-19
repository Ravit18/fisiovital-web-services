package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.model.Role;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Instrucciones de sistema del asistente (US17-US20) construidas con PromptTemplate de Spring AI.
 * La plantilla base vive en resources/prompts/assistant-system.st y recibe las variables
 * {today}, {language} y {roleRules}; las reglas de cada rol estan en rules-*.st.
 */
final class AgentPrompts {

    private static final PromptTemplate SYSTEM = PromptTemplate.builder()
            .resource(new ClassPathResource("prompts/assistant-system.st"))
            .build();
    private static final String PATIENT_RULES = read("prompts/rules-patient.st");
    private static final String PHYSIOTHERAPIST_RULES = read("prompts/rules-physiotherapist.st");
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy 'a las' HH:mm",
            Locale.forLanguageTag("es-419"));

    private AgentPrompts() {
    }

    static String system(Role role, LocalDateTime now, Locale locale) {
        String language = "en".equals(locale.getLanguage()) ? "ingles (en-US)" : "espanol latinoamericano (es-419)";
        return SYSTEM.render(Map.of(
                "today", now.format(TODAY),
                "language", language,
                "roleRules", role == Role.PATIENT ? PATIENT_RULES : PHYSIOTHERAPIST_RULES));
    }

    private static String read(String path) {
        Resource resource = new ClassPathResource(path);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la plantilla " + path, e);
        }
    }
}
