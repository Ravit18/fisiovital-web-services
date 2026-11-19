package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.agent.advisor.AgentGuardrails;
import com.healthdev.fisiovital.agent.advisor.GuardrailAdvisor;
import com.healthdev.fisiovital.agent.dto.AgentAnswer;
import com.healthdev.fisiovital.agent.dto.ChatRequest;
import com.healthdev.fisiovital.agent.dto.ChatResponse;
import com.healthdev.fisiovital.agent.tools.FisioVitalAgentTools;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.Conversation;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.repository.ConversationRepository;
import com.healthdev.fisiovital.repository.UserRepository;
import com.healthdev.fisiovital.security.CurrentUserProvider;
import com.healthdev.fisiovital.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * US17-US20 - Asistente conversacional con Spring AI y Google Gemini. Aplica primero las reglas de
 * seguridad fijas y luego consulta al modelo con el historial de la conversacion; los pacientes ademas
 * pueden reservar y reprogramar mediante las herramientas de FisioVitalAgentTools.
 */
@Slf4j
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final AgentGuardrails guardrails;
    private final FisioVitalAgentTools tools;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;
    private final AgentMapper agentMapper;
    private final boolean configured;

    /** El ChatClient (con memoria, guardrail y RAG como advisors) se arma en AgentConfig. */
    public AgentService(ChatClient chatClient, AgentGuardrails guardrails, FisioVitalAgentTools tools,
                        ConversationRepository conversationRepository, UserRepository userRepository,
                        CurrentUserProvider currentUser, AgentMapper agentMapper,
                        @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.chatClient = chatClient;
        this.guardrails = guardrails;
        this.tools = tools;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.agentMapper = agentMapper;
        this.configured = !apiKey.isBlank();
    }

    public ChatResponse ask(ChatRequest request) {
        UserPrincipal principal = currentUser.get();
        boolean safety = guardrails.check(request.message()).isPresent();
        if (!safety && !configured) {
            throw BusinessException.unavailable("assistant.unavailable");
        }

        Conversation conversation = resolveConversation(request.conversationId(), principal.userId());
        ResponseEntity<org.springframework.ai.chat.model.ChatResponse, AgentAnswer> response = callModel(principal.role(), conversation.getId(),
                request.message());
        String reply = replyOf(response.entity());
        String source = GuardrailAdvisor.SAFETY.equals(sourceOf(response.response())) ? "SAFETY" : "AI";
        return agentMapper.toResponse(conversation, reply, source);
    }

    private Conversation resolveConversation(Long conversationId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (conversationId != null) {
            Conversation existing = conversationRepository.findById(conversationId)
                    .filter(c -> c.belongsTo(userId))
                    .orElseThrow(() -> BusinessException.notFound("assistant.conversation.not.found"));
            if (!existing.isExpired(now)) {
                return existing;
            }
        }
        return conversationRepository.save(new Conversation(userRepository.getReferenceById(userId), now));
    }

    /** Memoria, guardrails y RAG son advisors; el modelo responde con salida estructurada (AgentAnswer). */
    private ResponseEntity<org.springframework.ai.chat.model.ChatResponse, AgentAnswer> callModel(Role role, Long conversationId, String message) {
        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                    .system(AgentPrompts.system(role, LocalDateTime.now(), LocaleContextHolder.getLocale()))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(conversationId)))
                    .user(message);
            if (role == Role.PATIENT) {
                spec = spec.tools(tools);
            }
            return spec.call().responseEntity(new LenientAnswerConverter());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("El asistente de IA no respondio: {}", e.getMessage());
            throw BusinessException.unavailable("assistant.unavailable");
        }
    }

    private Object sourceOf(org.springframework.ai.chat.model.ChatResponse response) {
        return response == null ? null : response.getMetadata().get(GuardrailAdvisor.SOURCE_KEY);
    }

    private String replyOf(AgentAnswer answer) {
        String content = answer == null ? null : answer.reply();
        if (content == null || content.isBlank()) {
            throw BusinessException.unavailable("assistant.unavailable");
        }
        return content.trim();
    }
}
