package com.healthdev.fisiovital.agent.tools;

import com.healthdev.fisiovital.dto.request.CreateClinicalNoteRequestDTO;
import com.healthdev.fisiovital.dto.request.PublishSlotsRequestDTO;
import com.healthdev.fisiovital.dto.response.AgendaResponseDTO;
import com.healthdev.fisiovital.dto.response.ClinicalNoteResponseDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.dto.response.SlotResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.security.CurrentUserProvider;
import com.healthdev.fisiovital.service.AvailabilityService;
import com.healthdev.fisiovital.service.ClinicalNoteService;
import com.healthdev.fisiovital.service.SessionService;
import com.healthdev.fisiovital.service.TreatmentPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Herramientas del asistente para el fisioterapeuta (US07, US08, US15, US16 y gestion de citas): ver su agenda,
 * publicar horarios, consultar y escribir la historia clinica de sus pacientes, y reservar o reprogramar por ellos.
 * Delegan en los mismos servicios que la API; cada una vuelve a verificar que el token sea de un fisioterapeuta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhysiotherapistAgentTools {

    static final int SESSION_MINUTES = 60;

    private final SessionService sessionService;
    private final AvailabilityService availabilityService;
    private final ClinicalNoteService clinicalNoteService;
    private final TreatmentPlanService treatmentPlanService;
    private final MessageSource messageSource;
    private final CurrentUserProvider currentUser;

    public record AgendaEntry(Long slotId, String time, String status, Long sessionId, String patientName,
                              Integer sessionNumber, Integer totalSessions) {
    }

    public record AgendaResult(String date, List<AgendaEntry> items, String message) {
    }

    public record PlanSummary(Long planId, String diagnosis, String status, long bookedSessions, int totalSessions,
                              int frequencyPerWeek) {
    }

    public record PatientOption(Long patientId, String patientName, List<PlanSummary> plans) {
    }

    public record NoteOption(Long noteId, Integer sessionNumber, String sessionDate, String text, String createdAt) {
    }

    public record SessionOption(Long sessionId, Long planId, int sessionNumber, String patientName, String start,
                                String label) {
    }

    public record ToolResult(boolean success, String message, Object detail) {
    }

    @Tool(description = "US08. Agenda del fisioterapeuta en un dia: cada bloque con su hora, si esta libre (FREE) o "
            + "reservado (BOOKED) y el paciente. Los bloques FREE traen el slotId para reservar o reprogramar.")
    public AgendaResult myAgenda(@ToolParam(description = "Fecha AAAA-MM-DD; vacia = hoy") String date) {
        if (!isPhysiotherapist()) {
            return new AgendaResult(date, List.of(), msg("assistant.physio.only"));
        }
        try {
            LocalDate day = date == null || date.isBlank() ? null : LocalDate.parse(date.strip());
            AgendaResponseDTO agenda = sessionService.agenda(day);
            List<AgendaEntry> items = agenda.items().stream()
                    .map(i -> new AgendaEntry(i.slotId(), time(i.startTime()), i.status(), i.sessionId(),
                            i.patientName(), i.sessionNumber(), i.totalSessions()))
                    .toList();
            return new AgendaResult(agenda.date().toString(), items, agenda.message());
        } catch (RuntimeException e) {
            return new AgendaResult(date, List.of(), message(e));
        }
    }

    @Tool(description = "US07. Publica bloques libres de 60 minutos en un dia, entre una hora de inicio y una de fin. "
            + "Llamala SOLO despues de resumir dia y horas y de que el fisioterapeuta confirme. No acepta fechas "
            + "pasadas; las horas que ya estaban publicadas se saltan (alreadyPublished). Si published viene vacio, NO "
            + "publicaste nada: dile que esos horarios ya los tenia.")
    public ToolResult publishAvailability(@ToolParam(description = "Fecha AAAA-MM-DD") String date,
                                          @ToolParam(description = "Hora de inicio HH:mm, por ejemplo 09:00") String startTime,
                                          @ToolParam(description = "Hora de fin HH:mm, por ejemplo 13:00") String endTime) {
        if (!isPhysiotherapist()) {
            return new ToolResult(false, msg("assistant.physio.only"), null);
        }
        LocalDate day;
        LocalTime start;
        LocalTime end;
        try {
            day = LocalDate.parse(date.strip());
            start = LocalTime.parse(startTime.strip());
            end = LocalTime.parse(endTime.strip());
        } catch (RuntimeException e) {
            return new ToolResult(false, msg("slots.invalid.range"), null);
        }
        // Hora por hora: las que ya estaban publicadas (o se cruzan) se saltan y se informan, en vez de fallar todo.
        List<String> published = new java.util.ArrayList<>();
        List<String> alreadyPublished = new java.util.ArrayList<>();
        String lastError = null;
        for (LocalTime hour = start; !hour.plusMinutes(SESSION_MINUTES).isAfter(end); hour = hour.plusMinutes(SESSION_MINUTES)) {
            try {
                List<SlotResponseDTO> slots = availabilityService.publish(
                        new PublishSlotsRequestDTO(day, hour, hour.plusMinutes(SESSION_MINUTES), SESSION_MINUTES));
                slots.forEach(s -> published.add(time(s.startTime())));
            } catch (BusinessException e) {
                if ("slots.overlap".equals(e.getMessageKey())) {
                    alreadyPublished.add(hour.toString());
                } else {
                    lastError = message(e);
                }
            }
        }
        PublishResult detail = new PublishResult(day.toString(), published, alreadyPublished);
        log.info("Asistente: publishAvailability {} {}-{} -> nuevas={} ya estaban={}", day, start, end, published,
                alreadyPublished);
        if (published.isEmpty() && alreadyPublished.isEmpty()) {
            return new ToolResult(false, lastError != null ? lastError : msg("slots.invalid.range"), detail);
        }
        // Nada nuevo porque ya existian: no es un error, pero NO se publico nada; el message lo dice tal cual.
        String note = published.isEmpty() ? msg("assistant.slots.already")
                : alreadyPublished.isEmpty() ? null : msg("assistant.slots.partly", String.join(", ", alreadyPublished));
        return new ToolResult(!published.isEmpty(), note, detail);
    }

    /** published: bloques nuevos; alreadyPublished: horas que ya estaban publicadas y se saltaron. */
    public record PublishResult(String date, List<String> published, List<String> alreadyPublished) {
    }

    @Tool(description = "Pacientes del fisioterapeuta con sus planes (planId, diagnostico, sesiones reservadas, "
            + "total y frecuencia). Filtra por nombre si se indica; vacio = todos.")
    public List<PatientOption> findMyPatients(@ToolParam(description = "Nombre o parte del nombre; puede ir vacio")
                                              String name) {
        if (!isPhysiotherapist()) {
            return List.of();
        }
        String wanted = normalize(name);
        Map<Long, PatientOption> byPatient = new LinkedHashMap<>();
        treatmentPlanService.plansOfMyPatients().stream()
                .filter(p -> wanted.isBlank() || normalize(p.patientName()).contains(wanted))
                .forEach(p -> byPatient.computeIfAbsent(p.patientId(),
                                id -> new PatientOption(id, p.patientName(), new java.util.ArrayList<>()))
                        .plans().add(new PlanSummary(p.id(), p.diagnosis(), p.status(), p.bookedSessions(),
                                p.totalSessions(), p.frequencyPerWeek())));
        return byPatient.values().stream().limit(10).toList();
    }

    @Tool(description = "US15. Historia clinica de un paciente del fisioterapeuta: notas de evolucion y generales, "
            + "de la mas reciente a la mas antigua.")
    public List<NoteOption> patientClinicalHistory(@ToolParam(description = "Id del paciente (de findMyPatients)")
                                                   Long patientId) {
        if (!isPhysiotherapist()) {
            return List.of();
        }
        try {
            return clinicalNoteService.findByPatient(patientId).stream().limit(20).map(this::toNote).toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    @Tool(description = "Sesiones reservadas y futuras de un paciente con este fisioterapeuta (sessionId, numero, "
            + "dia y hora). Sirve para reprogramar o para elegir la sesion de una nota.")
    public List<SessionOption> patientUpcomingSessions(@ToolParam(description = "Id del paciente") Long patientId) {
        if (!isPhysiotherapist()) {
            return List.of();
        }
        return sessionService.upcomingOfPatientForCurrentPhysiotherapist(patientId).stream()
                .map(this::toSession)
                .toList();
    }

    @Tool(description = "US16. Escribe una nota en la historia clinica de un paciente. Con sessionId es la evolucion "
            + "de esa sesion (solo si ya empezo y no fue cancelada); sin sessionId es una nota general. Maximo 1000 "
            + "caracteres. Llamala SOLO despues de leerle el texto al fisioterapeuta y de que confirme.")
    public ToolResult writeClinicalNote(@ToolParam(description = "Id del paciente") Long patientId,
                                        @ToolParam(description = "Texto de la nota, con las palabras del fisioterapeuta")
                                        String text,
                                        @ToolParam(description = "Id de la sesion (opcional, null para nota general)",
                                                required = false) Long sessionId) {
        if (!isPhysiotherapist()) {
            return new ToolResult(false, msg("assistant.physio.only"), null);
        }
        if (text == null || text.isBlank() || text.length() > 1000) {
            return new ToolResult(false, msg("validation.clinical.note.size"), null);
        }
        try {
            ClinicalNoteResponseDTO note = clinicalNoteService.create(patientId,
                    new CreateClinicalNoteRequestDTO(sessionId, text.strip()));
            return new ToolResult(true, null, toNote(note));
        } catch (RuntimeException e) {
            return new ToolResult(false, message(e), null);
        }
    }

    @Tool(description = "Reserva una sesion para un paciente del fisioterapeuta, en un bloque FREE de su agenda y "
            + "dentro de un plan ACTIVO de ese paciente. Llamala SOLO despues de confirmar paciente, dia y hora.")
    public ToolResult bookSessionForPatient(@ToolParam(description = "Id del plan activo del paciente") Long planId,
                                            @ToolParam(description = "Id del bloque libre (slotId de myAgenda)") Long slotId) {
        if (!isPhysiotherapist()) {
            return new ToolResult(false, msg("assistant.physio.only"), null);
        }
        try {
            return new ToolResult(true, null, toSession(treatmentPlanService.bookSessionForPatient(planId, slotId)));
        } catch (RuntimeException e) {
            return new ToolResult(false, message(e), null);
        }
    }

    @Tool(description = "Reprograma la sesion de un paciente del fisioterapeuta a otro bloque FREE de su agenda; "
            + "al paciente le llega un aviso. Llamala SOLO despues de confirmar la sesion y el nuevo horario.")
    public ToolResult reschedulePatientSession(@ToolParam(description = "Id de la sesion (de patientUpcomingSessions)")
                                               Long sessionId,
                                               @ToolParam(description = "Id del nuevo bloque libre (slotId de myAgenda)")
                                               Long newSlotId) {
        if (!isPhysiotherapist()) {
            return new ToolResult(false, msg("assistant.physio.only"), null);
        }
        try {
            return new ToolResult(true, null,
                    toSession(sessionService.rescheduleForPhysiotherapist(sessionId, newSlotId)));
        } catch (RuntimeException e) {
            return new ToolResult(false, message(e), null);
        }
    }

    private boolean isPhysiotherapist() {
        return currentUser.get().role() == Role.PHYSIOTHERAPIST;
    }

    private NoteOption toNote(ClinicalNoteResponseDTO n) {
        return new NoteOption(n.id(), n.sessionNumber(), n.sessionStartTime() == null ? null : label(n.sessionStartTime()),
                n.text(), n.createdAt() == null ? null : label(n.createdAt()));
    }

    private SessionOption toSession(SessionResponseDTO s) {
        return new SessionOption(s.id(), s.planId(), s.sessionNumber(), s.patientName(), s.startTime().toString(),
                label(s.startTime()));
    }

    private static String time(LocalDateTime dateTime) {
        return dateTime.toLocalTime().toString();
    }

    private static String label(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy HH:mm", LocaleContextHolder.getLocale()));
    }

    private static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }

    private String message(RuntimeException e) {
        if (e instanceof BusinessException business) {
            return messageSource.getMessage(business.getMessageKey(), business.getArgs(), business.getMessageKey(),
                    LocaleContextHolder.getLocale());
        }
        return msg("assistant.action.failed");
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}
