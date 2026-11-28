package com.healthdev.fisiovital.agent.tools;

import com.healthdev.fisiovital.service.AvailabilityService;
import com.healthdev.fisiovital.service.ClinicalNoteService;
import com.healthdev.fisiovital.service.BookingRules;
import com.healthdev.fisiovital.service.PhysiotherapistSearchService;
import com.healthdev.fisiovital.service.SessionService;
import com.healthdev.fisiovital.service.TreatmentPlanService;

import com.healthdev.fisiovital.dto.request.BookSessionRequestDTO;
import com.healthdev.fisiovital.dto.request.CreateTreatmentPlanRequestDTO;
import com.healthdev.fisiovital.dto.request.RescheduleSessionRequestDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.dto.response.SlotResponseDTO;
import com.healthdev.fisiovital.dto.response.TreatmentPlanResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.Specialty;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Herramientas que Gemini puede invocar (tool calling) para US18-US20. Cada una delega en los servicios
 * existentes, asi el asistente respeta las mismas reglas de negocio que la API: plan activo, bloques del
 * mismo fisioterapeuta, regla de 24 horas y control de concurrencia con @Version.
 */
@Component
@RequiredArgsConstructor
public class FisioVitalAgentTools {

    static final int SUGGESTED_PHYSIOS = 3;
    static final int OPTIONS_WHEN_DAY_IS_FULL = 3;
    static final int NEAREST_OPTIONS = 2;

    private final PhysiotherapistSearchService physiotherapistSearchService;
    private final TreatmentPlanService treatmentPlanService;
    private final AvailabilityService availabilityService;
    private final SessionService sessionService;
    private final ClinicalNoteService clinicalNoteService;
    private final BookingRules bookingRules;
    private final MessageSource messageSource;
    private final CurrentUserProvider currentUser;

    public record PhysioOption(Long id, String fullName, String specialty, int yearsOfExperience,
                               boolean hasAvailability) {
    }

    public record PlanOption(Long planId, String physiotherapistName, String diagnosis, long bookedSessions,
                             int totalSessions) {
    }

    public record SlotOption(int option, Long slotId, String start, String label) {
    }

    public record SlotsResult(String requestedDate, boolean requestedDateHasSlots, List<SlotOption> slots,
                              String message) {
    }

    public record SessionOption(Long sessionId, Long planId, int sessionNumber, String physiotherapistName,
                                String start, String label) {
    }

    public record UpcomingResult(List<SessionOption> sessions, String message) {
    }

    public record ActionResult(boolean success, String message, SessionOption session) {
    }

    public record PlanResult(boolean success, String message, PlanOption plan) {
    }

    public record NoteOption(String physiotherapistName, Integer sessionNumber, String sessionDate, String text) {
    }

    @Tool(description = "US18. Busca hasta 3 fisioterapeutas activos de una especialidad, ordenados por anios "
            + "de experiencia. Especialidades validas: DEPORTIVA (lesiones deportivas), COLUMNA_POSTURA (espalda, "
            + "cuello, lumbalgia, postura), GERIATRICA (adultos mayores), PEDIATRICA (ninos), POSTQUIRURGICA "
            + "(rehabilitacion tras cirugia), NEUROLOGICA (ACV, lesiones neurologicas).")
    public List<PhysioOption> searchPhysiotherapists(
            @ToolParam(description = "Especialidad en mayusculas, por ejemplo COLUMNA_POSTURA") String specialty) {
        Specialty parsed;
        try {
            parsed = Specialty.valueOf(specialty.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        return physiotherapistSearchService.search(parsed).items().stream()
                .limit(SUGGESTED_PHYSIOS)
                .map(p -> new PhysioOption(p.id(), p.fullName(), p.specialty(), p.yearsOfExperience(),
                        p.hasAvailability()))
                .toList();
    }

    @Tool(description = "Busca fisioterapeutas activos por nombre o apellido (por ejemplo \"Jorge\" o \"Paredes\"). "
            + "Usala cuando el usuario nombre a un fisioterapeuta, para obtener su id.")
    public List<PhysioOption> findPhysiotherapistsByName(
            @ToolParam(description = "Nombre, apellido o parte del nombre") String name) {
        String wanted = normalize(name);
        if (wanted.isBlank()) {
            return List.of();
        }
        return physiotherapistSearchService.search(null).items().stream()
                .filter(p -> normalize(p.fullName()).contains(wanted))
                .limit(5)
                .map(p -> new PhysioOption(p.id(), p.fullName(), p.specialty(), p.yearsOfExperience(),
                        p.hasAvailability()))
                .toList();
    }

    @Tool(description = "Horarios libres de un fisioterapeuta en un dia, sin necesidad de plan (solo para mostrarlos "
            + "o decirlos). Para reservar usa freeSlotsOnDate con el plan.")
    public SlotsResult physiotherapistFreeSlots(
            @ToolParam(description = "Id del fisioterapeuta") Long physiotherapistId,
            @ToolParam(description = "Fecha en formato AAAA-MM-DD") String date) {
        try {
            LocalDate day = LocalDate.parse(date);
            List<SlotResponseDTO> sameDay = availabilityService.findFreeSlots(physiotherapistId, day, day).slots()
                    .stream().filter(s -> s.startTime().toLocalDate().equals(day)).toList();
            return new SlotsResult(date, !sameDay.isEmpty(), toOptions(sameDay),
                    sameDay.isEmpty() ? msg("slots.none") : null);
        } catch (BusinessException e) {
            return new SlotsResult(date, false, List.of(), msg(e));
        } catch (RuntimeException e) {
            return new SlotsResult(date, false, List.of(), msg("slots.none"));
        }
    }

    private static String normalize(String text) {
        return text == null ? "" : java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }

    @Tool(description = "US19. Lista los planes de tratamiento ACTIVOS del paciente autenticado. Necesitas el "
            + "planId para consultar horarios y reservar.")
    public List<PlanOption> myActivePlans() {
        return treatmentPlanService.findMine().stream()
                .filter(p -> "ACTIVE".equals(p.status()))
                .map(this::toPlanOption)
                .toList();
    }

    @Tool(description = "US19. Horarios libres para reservar en el plan en una fecha. Si ese dia no hay "
            + "bloques libres o la semana ya completo la frecuencia del plan, devuelve los siguientes horarios "
            + "que si se pueden reservar y un message que explica el motivo.")
    public SlotsResult freeSlotsOnDate(@ToolParam(description = "Id del plan activo") Long planId,
                                       @ToolParam(description = "Fecha en formato AAAA-MM-DD") String date) {
        try {
            TreatmentPlanResponseDTO plan = activePlan(planId);
            Long physioId = plan.physiotherapistId();
            LocalDate day = LocalDate.parse(date);
            Map<LocalDate, Boolean> fullWeeks = new HashMap<>();
            Predicate<SlotResponseDTO> bookable = s -> !fullWeeks.computeIfAbsent(
                    BookingRules.weekStartOf(s.startTime().toLocalDate()),
                    week -> bookingRules.weekIsFull(plan.id(), plan.frequencyPerWeek(), week, BookingRules.NO_SESSION));
            List<SlotResponseDTO> sameDay = availabilityService.findFreeSlots(physioId, day, day).slots().stream()
                    .filter(s -> s.startTime().toLocalDate().equals(day))
                    .filter(bookable)
                    .toList();
            if (!sameDay.isEmpty()) {
                return new SlotsResult(date, true, toOptions(sameDay), null);
            }
            List<SlotResponseDTO> next = availabilityService.findFreeSlots(physioId, day.plusDays(1), null).slots()
                    .stream().filter(bookable).limit(OPTIONS_WHEN_DAY_IS_FULL).toList();
            String message = Boolean.TRUE.equals(fullWeeks.get(BookingRules.weekStartOf(day)))
                    ? msg("plan.frequency.exceeded", plan.frequencyPerWeek())
                    : next.isEmpty() ? msg("slots.none") : null;
            return new SlotsResult(date, false, toOptions(next), message);
        } catch (BusinessException e) {
            return new SlotsResult(date, false, List.of(), msg(e));
        }
    }

    @Tool(description = "US20. Los 2 horarios libres mas cercanos a una fecha y hora pedida, para ofrecerlos "
            + "cuando el horario pedido esta ocupado.")
    public SlotsResult nearestFreeSlots(@ToolParam(description = "Id del plan activo") Long planId,
                                        @ToolParam(description = "Fecha y hora en formato AAAA-MM-DDTHH:mm")
                                        String dateTime) {
        try {
            Long physioId = activePlan(planId).physiotherapistId();
            LocalDateTime target = LocalDateTime.parse(dateTime);
            List<SlotResponseDTO> nearest = availabilityService.findFreeSlots(physioId,
                            target.toLocalDate().minusDays(7), target.toLocalDate().plusDays(14)).slots().stream()
                    .sorted(Comparator.comparing(s -> Duration.between(target, s.startTime()).abs()))
                    .limit(NEAREST_OPTIONS)
                    .sorted(Comparator.comparing(SlotResponseDTO::startTime))
                    .toList();
            boolean exactIsFree = nearest.stream().anyMatch(s -> s.startTime().equals(target));
            String message = nearest.isEmpty() ? msg("slots.none") : null;
            return new SlotsResult(dateTime, exactIsFree, toOptions(nearest), message);
        } catch (BusinessException e) {
            return new SlotsResult(dateTime, false, List.of(), msg(e));
        }
    }

    @Tool(description = "US19. Crea un plan de tratamiento del paciente con un fisioterapeuta (el mismo paso "
            + "que hace en el perfil del especialista antes de reservar). Llamala SOLO despues de mostrar un resumen "
            + "del plan y de que el paciente responda que confirma; elegir un dato no es confirmar. Solo puede haber un plan "
            + "activo por fisioterapeuta; si ya existe, devuelve un message.")
    public PlanResult createTreatmentPlan(
            @ToolParam(description = "Id del fisioterapeuta devuelto por searchPhysiotherapists") Long physiotherapistId,
            @ToolParam(description = "Motivo o molestia con las palabras del paciente, maximo 255 caracteres") String diagnosis,
            @ToolParam(description = "Numero total de sesiones, de 1 a 30") Integer totalSessions,
            @ToolParam(description = "Sesiones por semana, de 1 a 7") Integer frequencyPerWeek) {
        try {
            if (!isPatient()) {
                return new PlanResult(false, msg("assistant.patient.only"), null);
            }
            if (diagnosis == null || diagnosis.isBlank() || totalSessions == null || totalSessions < 1
                    || totalSessions > 30 || frequencyPerWeek == null || frequencyPerWeek < 1 || frequencyPerWeek > 7) {
                return new PlanResult(false, msg("assistant.plan.invalid"), null);
            }
            String reason = diagnosis.strip();
            TreatmentPlanResponseDTO plan = treatmentPlanService.create(new CreateTreatmentPlanRequestDTO(
                    physiotherapistId, reason.length() > 255 ? reason.substring(0, 255) : reason,
                    totalSessions, frequencyPerWeek));
            return new PlanResult(true, null, toPlanOption(plan));
        } catch (BusinessException e) {
            return new PlanResult(false, msg(e), null);
        }
    }

    @Tool(description = "US19. Reserva una sesion del plan en un bloque libre. Llamala SOLO despues de que el "
            + "paciente confirme explicitamente el horario.")
    public ActionResult bookSession(@ToolParam(description = "Id del plan activo") Long planId,
                                    @ToolParam(description = "Id del bloque libre elegido") Long slotId) {
        try {
            if (!isPatient()) {
                return new ActionResult(false, msg("assistant.patient.only"), null);
            }
            return new ActionResult(true, null, toSessionOption(treatmentPlanService.bookSession(planId, new BookSessionRequestDTO(slotId))));
        } catch (BusinessException e) {
            return new ActionResult(false, msg(e), null);
        }
    }

    @Tool(description = "US20. Sesiones reservadas y futuras del paciente autenticado. Si no tiene, devuelve "
            + "el mensaje que debes repetir al paciente.")
    public UpcomingResult myUpcomingSessions() {
        List<SessionOption> sessions = sessionService.upcomingForCurrentPatient().stream()
                .map(this::toSessionOption)
                .toList();
        return new UpcomingResult(sessions, sessions.isEmpty() ? msg("assistant.no.upcoming.sessions") : null);
    }

    @Tool(description = "US20. Mueve una sesion reservada a otro bloque libre del mismo fisioterapeuta. Llamala "
            + "SOLO despues de que el paciente confirme explicitamente el nuevo horario.")
    public ActionResult rescheduleSession(@ToolParam(description = "Id de la sesion a mover") Long sessionId,
                                          @ToolParam(description = "Id del nuevo bloque libre") Long newSlotId) {
        try {
            if (!isPatient()) {
                return new ActionResult(false, msg("assistant.patient.only"), null);
            }
            return new ActionResult(true, null, toSessionOption(sessionService.reschedule(sessionId, new RescheduleSessionRequestDTO(newSlotId))));
        } catch (BusinessException e) {
            return new ActionResult(false, msg(e), null);
        }
    }

    @Tool(description = "US12. Cancela una sesion reservada del paciente. Llamala SOLO despues de mostrarle la "
            + "sesion (dia y hora) y de que confirme. Con menos de 24 horas queda como cancelacion tardia.")
    public ActionResult cancelSession(@ToolParam(description = "Id de la sesion a cancelar (de myUpcomingSessions)")
                                      Long sessionId) {
        try {
            if (!isPatient()) {
                return new ActionResult(false, msg("assistant.patient.only"), null);
            }
            SessionResponseDTO cancelled = sessionService.cancel(sessionId);
            return new ActionResult(true, cancelled.lateChange() ? msg("assistant.cancel.late") : null,
                    toSessionOption(cancelled));
        } catch (BusinessException e) {
            return new ActionResult(false, msg(e), null);
        }
    }

    @Tool(description = "Historia clinica del paciente autenticado: notas de evolucion de sus sesiones y notas "
            + "generales que escribieron sus fisioterapeutas, de la mas reciente a la mas antigua.")
    public List<NoteOption> myClinicalHistory() {
        if (!isPatient()) {
            return List.of();
        }
        return clinicalNoteService.findMine().stream()
                .limit(20)
                .map(n -> new NoteOption(n.physiotherapistName(), n.sessionNumber(),
                        n.sessionStartTime() == null ? null : label(n.sessionStartTime()), n.text()))
                .toList();
    }

    /**
     * Control dentro de las tools que escriben: aunque el asistente solo entrega las tools a pacientes,
     * cada tool vuelve a verificar el rol del usuario del token antes de reservar o mover una sesion.
     */
    private boolean isPatient() {
        return currentUser.get().role() == Role.PATIENT;
    }

    private TreatmentPlanResponseDTO activePlan(Long planId) {
        return treatmentPlanService.findMine().stream()
                .filter(p -> p.id().equals(planId) && "ACTIVE".equals(p.status()))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("plan.not.found"));
    }

    private PlanOption toPlanOption(TreatmentPlanResponseDTO p) {
        return new PlanOption(p.id(), p.physiotherapistName(), p.diagnosis(), p.bookedSessions(), p.totalSessions());
    }

    private List<SlotOption> toOptions(List<SlotResponseDTO> slots) {
        return IntStream.range(0, slots.size())
                .mapToObj(i -> new SlotOption(i + 1, slots.get(i).id(), slots.get(i).startTime().toString(),
                        label(slots.get(i).startTime())))
                .toList();
    }

    private SessionOption toSessionOption(SessionResponseDTO s) {
        return new SessionOption(s.id(), s.planId(), s.sessionNumber(), s.physiotherapistName(),
                s.startTime().toString(), label(s.startTime()));
    }

    private static String label(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy HH:mm", LocaleContextHolder.getLocale()));
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    private String msg(BusinessException e) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(), e.getMessageKey(),
                LocaleContextHolder.getLocale());
    }
}
