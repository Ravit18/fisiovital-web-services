package com.healthdev.fisiovital.agent;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.healthdev.fisiovital.agent.advisor.GuardrailAdvisor;
import com.healthdev.fisiovital.agent.advisor.KnowledgeAdvisor;
import com.healthdev.fisiovital.agent.mcp.FisioVitalMcpTools;
import com.healthdev.fisiovital.agent.memory.DatabaseChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion del agente de IA (US17-US20) con Spring AI y Google Gemini:
 * ChatClient con sus advisors, cliente de Gemini (ChatModel y embeddings), VectorStore del RAG y
 * herramientas publicadas por MCP. Las tools del paciente se pasan por llamada en AgentService.
 */
@Configuration(proxyBeanMethods = false)
public class AgentConfig {

    static final String NOT_CONFIGURED = "not-configured";

    /**
     * ChatClient: memoria (historial por conversacion), guardrail (respuestas fijas de seguridad) y RAG
     * (fragmentos de la base de conocimiento) se aplican como advisors antes de llegar al ChatModel.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, DatabaseChatMemory chatMemory,
                                 GuardrailAdvisor guardrailAdvisor, KnowledgeAdvisor knowledgeAdvisor) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), guardrailAdvisor,
                        knowledgeAdvisor)
                .build();
    }

    /**
     * Cliente de Gemini para el ChatModel. Reemplaza al de la autoconfiguracion, que no deja arrancar la
     * aplicacion sin API key: sin key, la app arranca igual y el asistente responde 503.
     */
    @Bean
    public Client googleGenAiClient(@Value("${app.ai.gemini.api-key:}") String apiKey,
                                    @Value("${app.ai.gemini.timeout-ms:60000}") int timeoutMs) {
        return Client.builder()
                .apiKey(apiKey.isBlank() ? NOT_CONFIGURED : apiKey)
                .httpOptions(HttpOptions.builder().timeout(timeoutMs).build())
                .build();
    }

    /** Igual que el cliente de chat: sin key la app arranca (la autoconfiguracion exigiria un project-id). */
    @Bean
    public GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails(
            @Value("${app.ai.gemini.api-key:}") String apiKey) {
        return GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey.isBlank() ? NOT_CONFIGURED : apiKey)
                .build();
    }

    /**
     * VectorStore en memoria para el RAG. Solo existe cuando los embeddings de Gemini estan activos
     * (spring.ai.model.embedding.text=google-genai); en las pruebas estan apagados.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.embedding.text", havingValue = "google-genai")
    public VectorStore knowledgeVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /** MCP: publica los @Tool de solo lectura de FisioVitalMcpTools en el servidor MCP (/mcp). */
    @Bean
    public ToolCallbackProvider fisioVitalMcpToolCallbacks(FisioVitalMcpTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
