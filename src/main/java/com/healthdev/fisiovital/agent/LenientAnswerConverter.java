package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.agent.dto.AgentAnswer;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;

/**
 * Convierte la salida del modelo en {@link AgentAnswer}. Si el modelo ignora el formato y responde texto
 * plano, se usa ese texto como respuesta en lugar de fallar.
 */
class LenientAnswerConverter implements StructuredOutputConverter<AgentAnswer> {

    private final BeanOutputConverter<AgentAnswer> delegate = new BeanOutputConverter<>(AgentAnswer.class);

    @Override
    public AgentAnswer convert(String source) {
        try {
            AgentAnswer answer = delegate.convert(source);
            if (answer != null && answer.reply() != null) {
                return answer;
            }
        } catch (RuntimeException ignored) {
            // texto plano: se devuelve tal cual
        }
        return new AgentAnswer(source);
    }

    @Override
    public String getFormat() {
        return delegate.getFormat();
    }
}
