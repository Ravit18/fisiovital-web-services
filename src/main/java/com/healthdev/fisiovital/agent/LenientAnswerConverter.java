package com.healthdev.fisiovital.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdev.fisiovital.agent.dto.AgentAction;
import com.healthdev.fisiovital.agent.dto.AgentAnswer;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte la salida del modelo en {@link AgentAnswer}. Si el JSON viene mal formado (por ejemplo con una clave
 * repetida o entre bloques de codigo), se lee de forma tolerante y se rescatan reply, choices y action; si el
 * modelo respondio texto plano, se usa ese texto como respuesta en lugar de fallar.
 */
class LenientAnswerConverter implements StructuredOutputConverter<AgentAnswer> {

    private static final ObjectMapper TOLERANT = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final BeanOutputConverter<AgentAnswer> delegate = new BeanOutputConverter<>(AgentAnswer.class);

    @Override
    public AgentAnswer convert(String source) {
        try {
            AgentAnswer answer = delegate.convert(source);
            if (answer != null && answer.reply() != null) {
                return answer;
            }
        } catch (RuntimeException ignored) {
            // se intenta la lectura tolerante
        }
        AgentAnswer rescued = tolerant(source);
        return rescued != null ? rescued : new AgentAnswer(source);
    }

    /** Lee el JSON como arbol (una clave repetida no falla: gana la ultima) y arma la respuesta. */
    static AgentAnswer tolerant(String source) {
        if (source == null) {
            return null;
        }
        String text = source.strip();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode root = TOLERANT.readTree(text.substring(start, end + 1));
            JsonNode reply = root.get("reply");
            if (reply == null || !reply.isTextual()) {
                return null;
            }
            List<String> choices = new ArrayList<>();
            JsonNode options = root.get("choices");
            if (options != null && options.isArray()) {
                options.forEach(o -> {
                    if (o.isTextual()) {
                        choices.add(o.asText());
                    }
                });
            }
            AgentAction action = null;
            JsonNode actionNode = root.get("action");
            if (actionNode != null && actionNode.isObject()) {
                action = TOLERANT.treeToValue(actionNode, AgentAction.class);
            }
            return new AgentAnswer(reply.asText(), choices, action);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getFormat() {
        return delegate.getFormat();
    }
}
