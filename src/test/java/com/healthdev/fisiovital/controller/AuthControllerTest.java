package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US01-US03 (EP01 - Cuentas y acceso): 3 escenarios por historia.
 * Todos los mensajes se piden en es-419 y se aserta el texto exacto del cuerpo.
 */
class AuthControllerTest extends IntegrationTestBase {

    // ---------------- US01 - Registrar paciente ----------------

    @Test
    void us01_exitoso_creaCuentaPatient() throws Exception {
        mockMvc.perform(lang(post("/api/v1/auth/sign-up/patient"), "es-419")
                        .content("""
                                {"fullName":"Carlos Mendoza","email":"carlos.%s@correo.com",
                                 "password":"Fisio2026*","phone":"912345678"}
                                """.formatted(uniq())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void us01_error_correoDuplicado409() throws Exception {
        String email = "dup." + uniq() + "@correo.com";
        signUpPatient("Uno", email);

        mockMvc.perform(lang(post("/api/v1/auth/sign-up/patient"), "es-419")
                        .content("""
                                {"fullName":"Otro","email":"%s","password":"Fisio2026*","phone":"912345678"}
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este correo ya está registrado"));
    }

    @Test
    void us01_alternativo_passwordDebil400IndicaCampo() throws Exception {
        mockMvc.perform(lang(post("/api/v1/auth/sign-up/patient"), "es-419")
                        .content("""
                                {"fullName":"Debil","email":"debil.%s@correo.com","password":"1234","phone":"912345678"}
                                """.formatted(uniq())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"))
                .andExpect(jsonPath("$.details[0]").value(
                        org.hamcrest.Matchers.containsString("password")));
    }

    // ---------------- US02 - Registrar fisioterapeuta ----------------

    @Test
    void us02_exitoso_creaFisioterapeuta201() throws Exception {
        mockMvc.perform(lang(post("/api/v1/auth/sign-up/physiotherapist"), "es-419")
                        .content("""
                                {"fullName":"Mariana Rojas","email":"mariana.%s@correo.com",
                                 "password":"Fisio2026*","phone":"987654321","licenseNumber":"%s",
                                 "specialty":"DEPORTIVA","yearsOfExperience":8,
                                 "description":"Fisioterapeuta deportiva"}
                                """.formatted(uniq(), uniq())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PHYSIOTHERAPIST"));
    }

    @Test
    void us02_error_colegiaturaVacia400() throws Exception {
        mockMvc.perform(lang(post("/api/v1/auth/sign-up/physiotherapist"), "es-419")
                        .content("""
                                {"fullName":"Sin Lic","email":"sinlic.%s@correo.com","password":"Fisio2026*",
                                 "phone":"987654321","licenseNumber":"","specialty":"NEUROLOGICA",
                                 "yearsOfExperience":5,"description":"x"}
                                """.formatted(uniq())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El número de colegiatura es obligatorio"));
    }

    @Test
    void us02_alternativo_colegiaturaDuplicada409() throws Exception {
        String license = uniq();
        signUpPhysio("Primero", "primero." + uniq() + "@correo.com", license, "NEUROLOGICA", 5);

        mockMvc.perform(lang(post("/api/v1/auth/sign-up/physiotherapist"), "es-419")
                        .content("""
                                {"fullName":"Segundo","email":"segundo.%s@correo.com","password":"Fisio2026*",
                                 "phone":"987654321","licenseNumber":"%s","specialty":"NEUROLOGICA",
                                 "yearsOfExperience":5,"description":"x"}
                                """.formatted(uniq(), license)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta colegiatura ya está registrada"));
    }

    // ---------------- US03 - Iniciar sesion ----------------

    @Test
    void us03_exitoso_loginDevuelveJwt() throws Exception {
        String email = "login." + uniq() + "@correo.com";
        signUpPatient("Login", email);

        mockMvc.perform(lang(post("/api/v1/auth/sign-in"), "es-419")
                        .content("{\"email\":\"%s\",\"password\":\"Fisio2026*\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void us03_error_passwordIncorrecta401() throws Exception {
        String email = "mala." + uniq() + "@correo.com";
        signUpPatient("Mala", email);

        mockMvc.perform(lang(post("/api/v1/auth/sign-in"), "es-419")
                        .content("{\"email\":\"%s\",\"password\":\"MalaClave123\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void us03_alternativo_bloqueoTras5Intentos423() throws Exception {
        String email = "lock." + uniq() + "@correo.com";
        signUpPatient("Bloqueo", email);

        // 5 intentos fallidos bloquean la cuenta; el siguiente responde 423.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(lang(post("/api/v1/auth/sign-in"), "es-419")
                            .content("{\"email\":\"%s\",\"password\":\"WrongPass123\"}".formatted(email)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(lang(post("/api/v1/auth/sign-in"), "es-419")
                        .content("{\"email\":\"%s\",\"password\":\"WrongPass123\"}".formatted(email)))
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.message")
                        .value("Demasiados intentos. Vuelve a intentarlo en 15 minutos"));
    }

    // ---------------- Seguridad transversal ----------------

    @Test
    void sinToken_responde401ConMensaje() throws Exception {
        mockMvc.perform(lang(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/physiotherapists"), "es-419"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
