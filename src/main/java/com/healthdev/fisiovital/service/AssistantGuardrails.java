package com.healthdev.fisiovital.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de seguridad que se aplican antes de consultar al modelo de IA, para que las respuestas
 * criticas no dependan del modelo: emergencias (US18 alternativo), indicaciones medicas (US17
 * alternativo) y molestias sin detalle (US18 error). Devuelve la clave i18n de la respuesta fija.
 */
@Component
public class AssistantGuardrails {

    private static final Pattern EMERGENCY = Pattern.compile(
            "dolor (en el|de|del) pecho|dolor toracico|falta de aire|no puedo respirar|dificultad para respirar"
                    + "|me desmaye|desmayo|perdi el conocimiento|perdida de conocimiento|paralisis|no siento (el|la|los|las) "
                    + "|chest pain|shortness of breath|can'?t breathe|cannot breathe|fainted|passed out");

    private static final Pattern MEDICATION = Pattern.compile(
            "medicament|medicina|pastilla|dosis|ibuprofeno|paracetamol|naproxeno|diclofenaco|antiinflamatorio"
                    + "|relajante muscular|que (me )?tomo|medication|medicine|\\bpills?\\b|\\bdose\\b|painkiller");

    private static final Pattern VAGUE_PAIN = Pattern.compile(
            "^(me duele|duele|tengo dolor|siento dolor|me duele algo|it hurts|i have pain|i'?m in pain|pain)[\\s.!?]*$");

    public Optional<String> check(String message) {
        String text = normalize(message);
        if (EMERGENCY.matcher(text).find()) {
            return Optional.of("assistant.emergency");
        }
        if (MEDICATION.matcher(text).find()) {
            return Optional.of("assistant.no.medical.advice");
        }
        if (VAGUE_PAIN.matcher(text).matches()) {
            return Optional.of("assistant.ask.details");
        }
        return Optional.empty();
    }

    private static String normalize(String message) {
        String noAccents = Normalizer.normalize(message, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase().replaceAll("[¿¡]", "").trim().replaceAll("\\s+", " ");
    }
}
