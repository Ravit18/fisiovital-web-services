package com.healthdev.fisiovital.exception;

import com.healthdev.fisiovital.IntegrationTestBase;
import com.healthdev.fisiovital.repository.AppointmentRepository;
import jakarta.persistence.QueryTimeoutException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler - respuestas estandarizadas que antes eran 500 o sin cuerpo.
 *
 * Usa @MockBean sobre AppointmentRepository (contexto propio) para poder simular una
 * consulta lenta sin depender del driver de base de datos: asi se ejercita el camino
 * real HTTP -> servicio -> handler -> 503 de US14 (alternativo, carga &gt; 2 min).
 */
class GlobalExceptionHandlerTest extends IntegrationTestBase {

    @MockBean
    private AppointmentRepository appointmentRepository;

    @Autowired
    private Environment environment;

    @Test
    void rutaInexistenteResponde404ConMensaje() throws Exception {
        String patient = newPatientToken();

        mockMvc.perform(authed(get("/api/v1/ruta-que-no-existe"), patient))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El recurso solicitado no existe"));
    }

    @Test
    void metodoNoSoportadoResponde405ConMensaje() throws Exception {
        mockMvc.perform(lang(get("/api/v1/auth/sign-in"), "es-419"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Método HTTP no soportado para este recurso"))
                .andExpect(jsonPath("$.details[0]").value("GET"));
    }

    /** US14 alternativo - una consulta que supera el timeout responde 503 con mensaje i18n. */
    @Test
    void consultaLentaResponde503ConMensaje() throws Exception {
        String patient = newPatientToken();
        Mockito.when(appointmentRepository.findByPatient(Mockito.anyLong(), Mockito.any()))
                .thenThrow(new QueryTimeoutException("timeout simulado", new SQLException("consulta lenta")));

        mockMvc.perform(authed(get("/api/v1/appointments/me"), patient))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Por favor inténtalo más tarde"));

        // El timeout esta publicado como propiedad configurable (QUERY_TIMEOUT_MS).
        Assertions.assertThat(environment.getProperty(
                "spring.jpa.properties.jakarta.persistence.query.timeout")).isNotBlank();
    }
}
