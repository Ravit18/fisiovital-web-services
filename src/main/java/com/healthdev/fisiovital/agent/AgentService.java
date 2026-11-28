package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.agent.advisor.AgentGuardrails;
import com.healthdev.fisiovital.agent.advisor.GuardrailAdvisor;
import com.healthdev.fisiovital.agent.dto.AgentAnswer;
import com.healthdev.fisiovital.agent.dto.ChatRequest;
import com.healthdev.fisiovital.agent.dto.ChatResponse;
import com.healthdev.fisiovital.agent.tools.FisioVitalAgentTools;
import com.healthdev.fisiovital.agent.tools.PhysiotherapistAgentTools;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.Conversation;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.repository.ConversationRepository;
import com.healthdev.fisiovital.repository.PatientRepository;
import com.healthdev.fisiovital.repository.PhysiotherapistRepository;
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
    private final PhysiotherapistAgentTools physiotherapistTools;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PhysiotherapistRepository physiotherapistRepository;
    private final CurrentUserProvider currentUser;
    private final AgentMapper agentMapper;
    private final boolean configured;

    /** El ChatClient (con memoria, guardrail y RAG como advisors) se arma en AgentConfig. */
    public AgentService(ChatClient chatClient, AgentGuardrails guardrails, FisioVitalAgentTools tools,
                        PhysiotherapistAgentTools physiotherapistTools,
                        ConversationRepository conversationRepository, UserRepository userRepository,
                        PatientRepository patientRepository, PhysiotherapistRepository physiotherapistRepository,
                        CurrentUserProvider currentUser, AgentMapper agentMapper,
                        @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.chatClient = chatClient;
        this.guardrails = guardrails;
        this.tools = tools;
        this.physiotherapistTools = physiotherapistTools;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.physiotherapistRepository = physiotherapistRepository;
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

        Conversation conversation = resolveConversation(request.conversationId(), principal.userId(),
                request.message());
        ResponseEntity<org.springframework.ai.chat.model.ChatResponse, AgentAnswer> response = callModel(principal.role(),
                fullNameOf(principal), conversation.getId(), request.message(), contextOf(request));
        String reply = replyOf(response.entity());
        String source = GuardrailAdvisor.SAFETY.equals(sourceOf(response.response())) ? "SAFETY" : "AI";
        boolean fixed = "SAFETY".equals(source) || response.entity() == null;
        java.util.List<String> choices = fixed ? java.util.List.of() : response.entity().cleanChoices();
        com.healthdev.fisiovital.agent.dto.AgentAction action = fixed ? null : validAction(response.entity().action());
        return agentMapper.toResponse(conversation, reply, source, choices, action);
    }

    /** Sin conversationId se abre un chat nuevo; con el, se retoma ese chat del historial. */
    private Conversation resolveConversation(Long conversationId, Long userId, String firstMessage) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .filter(c -> c.belongsTo(userId))
                    .orElseThrow(() -> BusinessException.notFound("assistant.conversation.not.found"));
        }
        return conversationRepository.save(
                new Conversation(userRepository.getReferenceById(userId), firstMessage, LocalDateTime.now()));
    }

    /** Memoria, guardrails y RAG son advisors; el modelo responde con salida estructurada (AgentAnswer). */
    /** Pantalla abierta y canal (voz) del mensaje: el modelo los recibe al final de las instrucciones. */
    static String contextOf(ChatRequest request) {
        StringBuilder context = new StringBuilder();
        if (request.screen() != null && !request.screen().isBlank()) {
            context.append("\n# PANTALLA ACTUAL\nEl usuario tiene abierta la ruta ")
                    .append(request.screen().strip().replaceAll("[^A-Za-z0-9/?=&_\\-]", ""))
                    .append(" de la Web App.\n");
        }
        if (Boolean.TRUE.equals(request.voice())) {
            context.append("\n# CANAL DE VOZ\nTu respuesta se lee en voz alta: maximo 2 frases cortas, sin listas ni "
                    + "simbolos. Si abres una pantalla, dilo con naturalidad (\"Te deje en pantalla...\").\n");
        }
        return context.toString();
    }

    /** Solo pantallas conocidas; una pagina inventada por el modelo se descarta. */
    static com.healthdev.fisiovital.agent.dto.AgentAction validAction(com.healthdev.fisiovital.agent.dto.AgentAction a) {
        if (a == null || a.page() == null || !PAGES.contains(a.page().strip().toUpperCase(java.util.Locale.ROOT))) {
            return null;
        }
        String page = a.page().strip().toUpperCase(java.util.Locale.ROOT);
        if ("PHYSIOTHERAPIST_PROFILE".equals(page) && a.physiotherapistId() == null) {
            return null;
        }
        String date = a.date() != null && a.date().matches("\\d{4}-\\d{2}-\\d{2}") ? a.date() : null;
        String specialty = a.specialty() != null && a.specialty().matches("[A-Z_]{3,30}") ? a.specialty() : null;
        return new com.healthdev.fisiovital.agent.dto.AgentAction(page, a.physiotherapistId(), date, specialty);
    }

    static final java.util.Set<String> PAGES = java.util.Set.of("HOME", "PHYSIOTHERAPISTS", "PHYSIOTHERAPIST_PROFILE",
            "APPOINTMENTS", "APPOINTMENT_HISTORY", "TREATMENT_PLANS", "NOTIFICATIONS", "ASSISTANT", "AGENDA",
            "AVAILABILITY", "PATIENTS");

    private String fullNameOf(UserPrincipal principal) {
        return principal.role() == Role.PATIENT
                ? patientRepository.findByUserId(principal.userId()).map(p -> p.getFullName()).orElse(null)
                : physiotherapistRepository.findByUserId(principal.userId()).map(p -> p.getFullName()).orElse(null);
    }

    private ResponseEntity<org.springframework.ai.chat.model.ChatResponse, AgentAnswer> callModel(Role role, String userName,
                                                                                               Long conversationId, String message,
                                                                                               String context) {
        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                    .system(AgentPrompts.system(role, userName, LocalDateTime.now(), LocaleContextHolder.getLocale()) + context)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(conversationId)))
                    .user(message);
            // Cada rol ve solo sus herramientas; ademas, cada herramienta vuelve a verificar el rol del token.
            spec = spec.tools(role == Role.PATIENT ? tools : physiotherapistTools);
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
