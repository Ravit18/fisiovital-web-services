package com.healthdev.fisiovital.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente de Google Gemini para Spring AI (US17-US20). Reemplaza al de la autoconfiguracion, que no deja
 * arrancar la aplicacion sin API key: sin key, la app arranca igual y el asistente responde 503.
 */
@Configuration(proxyBeanMethods = false)
public class GeminiConfig {

    static final String NOT_CONFIGURED = "not-configured";

    @Bean
    public Client googleGenAiClient(@Value("${app.ai.gemini.api-key:}") String apiKey,
                                    @Value("${app.ai.gemini.timeout-ms:60000}") int timeoutMs) {
        return Client.builder()
                .apiKey(apiKey.isBlank() ? NOT_CONFIGURED : apiKey)
                .httpOptions(HttpOptions.builder().timeout(timeoutMs).build())
                .build();
    }
}
