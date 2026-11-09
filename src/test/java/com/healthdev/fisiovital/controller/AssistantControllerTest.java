package com.healthdev.fisiovital.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.security.UserPrincipal;
import com.healthdev.fisiovital.service.AssistantTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US17-US20 - Asistente con IA. Gemini se simula con un ChatModel de Mockito para que los escenarios sean
 * deterministas; las herramientas (tool calling) se prueban contra la base de datos real con un paciente
 * autenticado.
 */
class AssistantControllerTest extends IntegrationTestBase {

    private static final String UNAVAILABLE = "El asistente no está disponible";

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private AssistantTools tools;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    // ---------- US17 - Consultar dudas al asistente ----------

    @Test
    void us17_success_answersCancellationPolicyUsingFisioVitalRules() throws Exception {
        modelReplies("Puedes cancelar sin penalidad hasta 24 horas antes de tu sesión.");

        ask(newPatientToken(), null, "¿Cuál es la política de cancelación?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.reply").value("Puedes cancelar sin penalidad hasta 24 horas antes de tu sesión."))
                .andExpect(jsonPath("$.conversationId").isNumber());

        String system = lastPrompt().getInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText).findFirst().orElseThrow();
        assertThat(system).contains("sin penalidad hasta 24 horas antes");
    }

    @Test
    void us17_error_aiServiceDown_returns503() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Gemini caido"));

        ask(newPatientToken(), null, "¿Cuál es la política de cancelación?")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(UNAVAILABLE));
    }

    @Test
    void us17_alternative_medicationQuestion_noMedicalAdvice() throws Exception {
        ask(newPatientToken(), null, "¿Qué medicamento tomo?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SAFETY"))
                .andExpect(jsonPath("$.reply").value(
                        "No doy indicaciones médicas ni recomiendo medicamentos. Consulta con tu médico o con tu "
                                + "fisioterapeuta en tu próxima sesión."));
        verify(chatModel, never()).call(any(Prompt.class));
    }

    // ---------- US18 - Recibir recomendacion de especialista ----------

    @Test
    void us18_success_suggestsThreeSpinePhysiosByExperience() throws Exception {
        for (int years = 1; years <= 4; years++) {
            signUpPhysio("Columna " + uniq(), "col." + uniq() + "@correo.com", uniq(), "COLUMNA_POSTURA", years);
        }
        actAsPatient();

        List<AssistantTools.PhysioOption> suggested = tools.searchPhysiotherapists("COLUMNA_POSTURA");

        assertThat(suggested).hasSize(3);
        assertThat(suggested).allMatch(p -> p.specialty().equals("COLUMNA_POSTURA"));
        assertThat(suggested).extracting(AssistantTools.PhysioOption::yearsOfExperience)
                .isSortedAccordingTo((a, b) -> Integer.compare(b, a));
    }

    @Test
    void us18_error_vagueComplaint_asksWhereAndSinceWhen() throws Exception {
        ask(newPatientToken(), null, "Me duele")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SAFETY"))
                .andExpect(jsonPath("$.reply").value("¿En qué parte del cuerpo te duele y desde cuándo?"));
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void us18_alternative_chestPain_recommendsEmergency() throws Exception {
        ask(newPatientToken(), null, "Tengo dolor en el pecho y falta de aire")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SAFETY"))
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString("emergencia")));
        verify(chatModel, never()).call(any(Prompt.class));
    }

    // ---------- US19 - Reservar sesion mediante el asistente ----------

    @Test
    void us19_success_booksConfirmedSlotOfActivePlan() throws Exception {
        Fixture f = patientWithPlanAndSlots(4);
        actAs(f.patient());

        AssistantTools.PlanOption plan = tools.myActivePlans().get(0);
        AssistantTools.SlotsResult slots = tools.freeSlotsOnDate(plan.planId(), todayPlusDays(4));
        AssistantTools.ActionResult booked = tools.bookSession(plan.planId(), slots.slots().get(0).slotId());

        assertThat(slots.requestedDateHasSlots()).isTrue();
        assertThat(booked.success()).isTrue();
        assertThat(booked.session().start()).isEqualTo(slots.slots().get(0).start());
    }

    @Test
    void us19_error_dayWithoutSlots_proposesNextAvailable() throws Exception {
        Fixture f = patientWithPlanAndSlots(5);
        actAs(f.patient());

        AssistantTools.SlotsResult result = tools.freeSlotsOnDate(f.planId(), todayPlusDays(4));

        assertThat(result.requestedDateHasSlots()).isFalse();
        assertThat(result.slots()).isNotEmpty();
        assertThat(result.slots().get(0).start()).startsWith(todayPlusDays(5));
    }

    @Test
    void us19_alternative_secondMessageKeepsOfferedOptionsInContext() throws Exception {
        String token = newPatientToken();
        modelReplies("Tengo estos horarios: 1. jueves 16:00 2. jueves 17:00 3. jueves 18:00");
        JsonNode first = json(ask(token, null, "Resérvame el jueves en la tarde").andReturn());

        modelReplies("¿Confirmas el jueves a las 17:00?");
        ask(token, first.get("conversationId").asLong(), "el segundo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(first.get("conversationId").asLong()));

        List<String> sent = lastPrompt().getInstructions().stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .map(Message::getText).toList();
        assertThat(sent).containsExactly("Resérvame el jueves en la tarde",
                "Tengo estos horarios: 1. jueves 16:00 2. jueves 17:00 3. jueves 18:00", "el segundo");
    }

    // ---------- US20 - Reprogramar sesion mediante el asistente ----------

    @Test
    void us20_success_reschedulesToConfirmedSlot() throws Exception {
        Fixture f = patientWithPlanAndSlots(4);
        bookSession(f.patientToken(), f.planId(), f.slotIds().get(0));
        actAs(f.patient());

        AssistantTools.SessionOption session = tools.myUpcomingSessions().sessions().get(0);
        AssistantTools.ActionResult moved = tools.rescheduleSession(session.sessionId(), f.slotIds().get(2));

        assertThat(moved.success()).isTrue();
        assertThat(moved.session().start()).isNotEqualTo(session.start());
    }

    @Test
    void us20_error_requestedTimeTaken_offersTwoNearest() throws Exception {
        Fixture f = patientWithPlanAndSlots(4);
        bookSession(f.patientToken(), f.planId(), f.slotIds().get(1));
        actAs(f.patient());

        AssistantTools.SlotsResult result = tools.nearestFreeSlots(f.planId(), todayPlusDays(4) + "T09:00");

        assertThat(result.requestedDateHasSlots()).isFalse();
        assertThat(result.slots()).hasSize(2);
        assertThat(result.slots()).extracting(AssistantTools.SlotOption::start)
                .containsExactly(todayPlusDays(4) + "T08:00", todayPlusDays(4) + "T10:00");
    }

    @Test
    void us20_alternative_noUpcomingSessions_saysThereIsNothingToReschedule() throws Exception {
        JsonNode patient = signUpPatient("Sin Sesiones " + uniq(), "sin." + uniq() + "@correo.com");
        actAs(patient);

        AssistantTools.UpcomingResult result = tools.myUpcomingSessions();

        assertThat(result.sessions()).isEmpty();
        assertThat(result.message()).isEqualTo("No tienes sesiones reservadas para reprogramar");
    }

    // ---------- Validaciones generales ----------

    @Test
    void emptyMessage_returns400() throws Exception {
        ask(newPatientToken(), null, " ")
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersConversation_returns404() throws Exception {
        modelReplies("Hola");
        long conversationId = json(ask(newPatientToken(), null, "Hola").andReturn()).get("conversationId").asLong();

        ask(newPatientToken(), conversationId, "Hola")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La conversación no existe"));
    }

    @Test
    void englishRequest_safetyReplyInEnglish() throws Exception {
        mockMvc.perform(lang(post("/api/v1/assistant/messages"), "en-US")
                        .header("Authorization", "Bearer " + newPatientToken())
                        .content(body(null, "What medication should I take?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.startsWith("I do not give medical advice")));
    }

    // ---------- Control: las tools que escriben verifican el rol del token ----------

    @Test
    void control_physiotherapistCannotBookOrRescheduleThroughTools() throws Exception {
        Fixture f = patientWithPlanAndSlots(4);
        JsonNode physio = newPhysio("DEPORTIVA");
        UserPrincipal principal = new UserPrincipal(physio.get("userId").asLong(), physio.get("email").asText(),
                Role.PHYSIOTHERAPIST);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_PHYSIOTHERAPIST"))));
        LocaleContextHolder.setLocale(ES);

        AssistantTools.ActionResult booked = tools.bookSession(f.planId(), f.slotIds().get(0));
        AssistantTools.ActionResult moved = tools.rescheduleSession(1L, f.slotIds().get(1));

        assertThat(booked.success()).isFalse();
        assertThat(booked.message()).isEqualTo("Solo un paciente puede reservar o reprogramar sesiones");
        assertThat(moved.success()).isFalse();
    }

    // ---------- Helpers ----------

    private record Fixture(JsonNode patient, String patientToken, long planId, List<Long> slotIds) {
    }

    /** Paciente con plan activo y 4 bloques libres (08:00-12:00) en today + days. */
    private Fixture patientWithPlanAndSlots(int days) throws Exception {
        JsonNode physio = newPhysio("COLUMNA_POSTURA");
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(days), "08:00", "12:00", 60);
        JsonNode patient = signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com");
        String token = patient.get("token").asText();
        long planId = createPlan(token, physio.get("profileId").asLong(), 10, 2).get("id").asLong();
        List<Long> slotIds = new java.util.ArrayList<>();
        slots.forEach(s -> slotIds.add(s.get("id").asLong()));
        return new Fixture(patient, token, planId, slotIds);
    }

    private void actAsPatient() throws Exception {
        actAs(signUpPatient("Paciente " + uniq(), "paciente." + uniq() + "@correo.com"));
    }

    private void actAs(JsonNode signUp) {
        UserPrincipal principal = new UserPrincipal(signUp.get("userId").asLong(), signUp.get("email").asText(),
                Role.PATIENT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))));
        LocaleContextHolder.setLocale(ES);
    }

    private void modelReplies(String text) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }

    private Prompt lastPrompt() {
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, atLeastOnce()).call(captor.capture());
        return captor.getValue();
    }

    private org.springframework.test.web.servlet.ResultActions ask(String token, Long conversationId,
                                                                   String message) throws Exception {
        return mockMvc.perform(authed(post("/api/v1/assistant/messages"), token).content(body(conversationId, message)));
    }

    private String body(Long conversationId, String message) {
        var node = objectMapper.createObjectNode().put("message", message);
        if (conversationId != null) {
            node.put("conversationId", conversationId);
        }
        return node.toString();
    }
}
