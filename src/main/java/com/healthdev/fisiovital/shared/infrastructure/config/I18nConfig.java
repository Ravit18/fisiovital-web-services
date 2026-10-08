package com.healthdev.fisiovital.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Internacionalizacion: el idioma se elige con el header Accept-Language (en-US o es-419).
 * Por defecto se responde en espanol latinoamericano (es-419).
 */
@Configuration
public class I18nConfig {

    public static final Locale ES_419 = Locale.forLanguageTag("es-419");
    public static final Locale EN_US = Locale.forLanguageTag("en-US");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(EN_US, ES_419));
        resolver.setDefaultLocale(ES_419);
        return resolver;
    }
}
