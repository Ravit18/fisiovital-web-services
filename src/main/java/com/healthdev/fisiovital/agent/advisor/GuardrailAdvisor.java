package com.healthdev.fisiovital.agent.advisor;

import com.healthdev.fisiovital.agent.dto.AgentAnswer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Advisor de seguridad (US17/US18): si el mensaje cae en una regla fija (emergencia, medicamentos, molestia
 * sin detalle) responde sin llamar al modelo. Corre despues del advisor de memoria, asi la respuesta fija
 * tambien queda en el historial.
 */
@Component
public class GuardrailAdvisor implements CallAdvisor {

    public static final String SOURCE_KEY = "assistant.source";
    public static final String SAFETY = "SAFETY";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentGuardrails guardrails;
    private final MessageSource messageSource;

    public GuardrailAdvisor(AgentGuardrails guardrails, MessageSource messageSource) {
        this.guardrails = guardrails;
        this.messageSource = messageSource;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Optional<String> key = guardrails.check(request.prompt().getUserMessage().getText());
        if (key.isEmpty()) {
            return chain.nextCall(request);
        }
        String text = messageSource.getMessage(key.get(), null, key.get(), LocaleContextHolder.getLocale());
        // Misma forma que la salida estructurada del modelo (AgentAnswer) para que el servicio la convierta igual.
        ChatResponse response = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(toJson(text)))))
                .metadata(SOURCE_KEY, SAFETY)
                .build();
        // El contexto de la peticion (id de conversacion) debe viajar en la respuesta para el advisor de memoria.
        Map<String, Object> context = new HashMap<>(request.context());
        context.put(SOURCE_KEY, SAFETY);
        return new ChatClientResponse(response, context);
    }

    private static String toJson(String text) {
        try {
            return MAPPER.writeValueAsString(new AgentAnswer(text));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return "GuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
