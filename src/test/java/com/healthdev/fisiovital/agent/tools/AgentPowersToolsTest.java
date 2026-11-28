package com.healthdev.fisiovital.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdev.fisiovital.IntegrationTestBase;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Lo que el asistente ahora puede hacer: el paciente cancela y lee su historia; el fisioterapeuta ve su agenda,
 * publica horarios, lee y escribe notas, y reserva o reprograma por sus pacientes. Las tools se prueban contra la
 * base real con el usuario autenticado en el contexto de seguridad.
 */
class AgentPowersToolsTest extends IntegrationTestBase {

    @Autowired
    private FisioVitalAgentTools patientTools;

    @Autowired
    private PhysiotherapistAgentTools physioTools;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    private record Clinic(JsonNode physio, JsonNode patient, long planId, List<Long> slotIds) {
    }

    /** Fisioterapeuta con 4 bloques libres (09:00-13:00) en today + days, y un paciente con plan activo. */
    private Clinic clinic(int days) throws Exception {
        JsonNode physio = signUpPhysio("Fisio " + uniq(), "fisio." + uniq() + "@correo.com", uniq(), "DEPORTIVA", 7);
        JsonNode slots = publishSlots(physio.get("token").asText(), todayPlusDays(days), "09:00", "13:00", 60);
        JsonNode patient = signUpPatient("Rosa Quispe " + uniq(), "rosa." + uniq() + "@correo.com");
        long planId = createPlan(patient.get("token").asText(), physio.get("profileId").asLong(), 8, 3).get("id").asLong();
        List<Long> slotIds = new java.util.ArrayList<>();
        slots.forEach(s -> slotIds.add(s.get("id").asLong()));
        return new Clinic(physio, patient, planId, slotIds);
    }

    private void actAs(JsonNode signUp, Role role) {
        UserPrincipal principal = new UserPrincipal(signUp.get("userId").asLong(), signUp.get("email").asText(), role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        LocaleContextHolder.setLocale(ES);
    }

    // ---------- Paciente ----------

    @Test
    void patient_cancelsTheirSessionAndTheSlotIsFreeAgain() throws Exception {
        Clinic c = clinic(4);
        actAs(c.patient(), Role.PATIENT);
        FisioVitalAgentTools.ActionResult booked = patientTools.bookSession(c.planId(), c.slotIds().get(0));

        FisioVitalAgentTools.ActionResult cancelled = patientTools.cancelSession(booked.session().sessionId());

        assertThat(cancelled.success()).isTrue();
        assertThat(patientTools.myUpcomingSessions().sessions()).isEmpty();
        assertThat(patientTools.physiotherapistFreeSlots(c.physio().get("profileId").asLong(), todayPlusDays(4))
                .slots()).hasSize(4);
    }

    @Test
    void patient_cannotCancelSomeoneElsesSession() throws Exception {
        Clinic c = clinic(4);
        actAs(c.patient(), Role.PATIENT);
        long sessionId = patientTools.bookSession(c.planId(), c.slotIds().get(0)).session().sessionId();
        actAs(signUpPatient("Otro " + uniq(), "otro." + uniq() + "@correo.com"), Role.PATIENT);

        assertThat(patientTools.cancelSession(sessionId).success()).isFalse();
    }

    @Test
    void patient_readsTheNotesTheirPhysiotherapistWrote() throws Exception {
        Clinic c = clinic(4);
        actAs(c.physio(), Role.PHYSIOTHERAPIST);
        physioTools.writeClinicalNote(c.patient().get("profileId").asLong(), "Buena evolucion de la rodilla.", null);

        actAs(c.patient(), Role.PATIENT);
        List<FisioVitalAgentTools.NoteOption> notes = patientTools.myClinicalHistory();

        assertThat(notes).singleElement().satisfies(n -> {
            assertThat(n.text()).isEqualTo("Buena evolucion de la rodilla.");
            assertThat(n.physiotherapistName()).startsWith("Fisio");
        });
    }

    // ---------- Fisioterapeuta ----------

    @Test
    void physio_seesTheAgendaAndPublishesSlots() throws Exception {
        Clinic c = clinic(5);
        actAs(c.patient(), Role.PATIENT);
        patientTools.bookSession(c.planId(), c.slotIds().get(1));
        actAs(c.physio(), Role.PHYSIOTHERAPIST);

        PhysiotherapistAgentTools.AgendaResult agenda = physioTools.myAgenda(todayPlusDays(5));
        PhysiotherapistAgentTools.ToolResult published = physioTools.publishAvailability(todayPlusDays(6), "15:00", "18:00");
        PhysiotherapistAgentTools.ToolResult overlap = physioTools.publishAvailability(todayPlusDays(6), "16:00", "17:00");

        assertThat(agenda.items()).hasSize(4);
        assertThat(agenda.items()).filteredOn(i -> "BOOKED".equals(i.status()))
                .singleElement().satisfies(i -> assertThat(i.patientName()).startsWith("Rosa Quispe"));
        assertThat(published.success()).isTrue();
        assertThat(((PhysiotherapistAgentTools.PublishResult) published.detail()).published()).hasSize(3);
        assertThat(overlap.success()).isFalse();
        assertThat(overlap.message()).isEqualTo("Esos horarios ya estaban publicados; no se agregó ninguno nuevo");

        // Rango que se cruza a medias: publica solo las horas nuevas y avisa cuales ya estaban.
        PhysiotherapistAgentTools.PublishResult partial = (PhysiotherapistAgentTools.PublishResult)
                physioTools.publishAvailability(todayPlusDays(6), "17:00", "20:00").detail();
        assertThat(partial.published()).containsExactly("18:00", "19:00");
        assertThat(partial.alreadyPublished()).containsExactly("17:00");
    }

    @Test
    void physio_findsAPatientReadsAndWritesTheirHistory() throws Exception {
        Clinic c = clinic(4);
        actAs(c.physio(), Role.PHYSIOTHERAPIST);

        List<PhysiotherapistAgentTools.PatientOption> found = physioTools.findMyPatients("rosa quispe");
        long patientId = found.get(0).patientId();
        PhysiotherapistAgentTools.ToolResult saved = physioTools.writeClinicalNote(patientId,
                "Dolor 3/10, mejora la movilidad.", null);

        assertThat(found).singleElement().satisfies(p -> assertThat(p.plans()).singleElement()
                .satisfies(plan -> assertThat(plan.planId()).isEqualTo(c.planId())));
        assertThat(saved.success()).isTrue();
        assertThat(physioTools.patientClinicalHistory(patientId))
                .anyMatch(n -> n.text().equals("Dolor 3/10, mejora la movilidad."));
    }

    @Test
    void physio_booksAndReschedulesForTheirPatient_andThePatientIsNotified() throws Exception {
        Clinic c = clinic(4);
        actAs(c.physio(), Role.PHYSIOTHERAPIST);
        long patientId = c.patient().get("profileId").asLong();

        PhysiotherapistAgentTools.ToolResult booked = physioTools.bookSessionForPatient(c.planId(), c.slotIds().get(0));
        long sessionId = physioTools.patientUpcomingSessions(patientId).get(0).sessionId();
        PhysiotherapistAgentTools.ToolResult moved = physioTools.reschedulePatientSession(sessionId, c.slotIds().get(2));

        assertThat(booked.success()).isTrue();
        assertThat(moved.success()).isTrue();
        assertThat(physioTools.patientUpcomingSessions(patientId)).singleElement()
                .satisfies(s -> assertThat(s.label()).contains("11:00"));
        mockMvc.perform(authed(get("/api/v1/notifications"), c.patient().get("token").asText()))
                .andExpect(jsonPath("$[0].type").value("SESSION_RESCHEDULED"));
    }

    @Test
    void physio_cannotBookInAnotherPhysiotherapistsPlan() throws Exception {
        Clinic c = clinic(4);
        actAs(newPhysio("DEPORTIVA"), Role.PHYSIOTHERAPIST);

        assertThat(physioTools.bookSessionForPatient(c.planId(), c.slotIds().get(0)).success()).isFalse();
        assertThat(physioTools.findMyPatients("")).isEmpty();
    }

    @Test
    void patientCannotUseThePhysiotherapistTools() throws Exception {
        Clinic c = clinic(4);
        actAs(c.patient(), Role.PATIENT);

        assertThat(physioTools.publishAvailability(todayPlusDays(7), "09:00", "10:00").success()).isFalse();
        assertThat(physioTools.myAgenda(null).items()).isEmpty();
    }
}
