package com.healthdev.fisiovital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para la Web App Angular (§4.4). SecurityConfig lo activa con {@code http.cors(...)}, así el preflight
 * OPTIONS se responde antes de pedir el token. Orígenes en {@code app.cors.allowed-origins}
 * (variable CORS_ALLOWED_ORIGINS al desplegar); en local, cualquier puerto de localhost.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
