package com.healthdev.fisiovital.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Supabase publica el esquema public por su Data API (roles anon y authenticated). La app no usa esa API:
 * entra con el rol postgres, que es dueño de las tablas y por eso no se ve afectado por RLS.
 * Al arrancar se activa RLS en todas las tablas (sin políticas = la Data API no ve nada) y se quitan los permisos
 * de anon/authenticated, también para las tablas que Hibernate cree después. En un PostgreSQL normal
 * (sin el rol anon) no hace nada.
 */
@Component
public class SupabaseSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SupabaseSecurityConfig.class);

    private static final String HARDEN_SQL = """
            DO $$
            DECLARE t record;
            BEGIN
              IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN RETURN; END IF;
              FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
                EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
              END LOOP;
              REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;
              REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;
              ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated;
              ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;
            END $$;
            """;

    private static final String COUNT_UNPROTECTED = """
            SELECT count(*) FROM pg_tables WHERE schemaname = 'public' AND NOT rowsecurity
            """;

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public SupabaseSecurityConfig(JdbcTemplate jdbc,
                                  @Value("${app.db.supabase-hardening:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void harden() {
        if (!enabled) return;
        try {
            jdbc.execute(HARDEN_SQL);
            Integer unprotected = jdbc.queryForObject(COUNT_UNPROTECTED, Integer.class);
            log.info("Supabase hardening aplicado: tablas de public sin RLS = {}", unprotected);
        } catch (Exception e) {
            log.warn("No se pudo aplicar el hardening de Supabase: {}", e.getMessage());
        }
    }
}
