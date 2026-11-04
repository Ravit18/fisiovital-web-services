package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.request.BookSessionRequestDTO;
import com.healthdev.fisiovital.dto.request.RescheduleSessionRequestDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.dto.response.SlotResponseDTO;
import com.healthdev.fisiovital.dto.response.TreatmentPlanResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.Specialty;
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
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Herramientas que Gemini puede invocar (tool calling) para US18-US20. Cada una delega en los servicios
 * existentes, asi el asistente respeta las mismas reglas de negocio que la API: plan activo, bloques del
 * mismo fisioterapeuta, regla de 24 horas y control de concurrencia con @Version.
 */
@Component
@RequiredArgsConstructor
public class AssistantTools {

    static final int SUGGESTED_PHYSIOS = 3;
    static final int OPTIONS_WHEN_DAY_IS_FULL = 3;
    static final int NEAREST_OPTIONS = 2;

    private final PhysiotherapistSearchService physiotherapistSearchService;
    private final TreatmentPlanService treatmentPlanService;
    private final AvailabilityService availabilityService;
    private final SessionService sessionService;
    private final MessageSource messageSource;

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

    @Tool(description = "US19. Lista los planes de tratamiento ACTIVOS del paciente autenticado. Necesitas el "
            + "planId para consultar horarios y reservar.")
    public List<PlanOption> myActivePlans() {
        return treatmentPlanService.findMine().stream()
                .filter(p -> "ACTIVE".equals(p.status()))
                .map(this::toPlanOption)
                .toList();
    }

    @Tool(description = "US19. Horarios libres del fisioterapeuta del plan en una fecha. Si ese dia no hay "
            + "bloques libres, devuelve los siguientes horarios disponibles para proponerlos.")
    public SlotsResult freeSlotsOnDate(@ToolParam(description = "Id del plan activo") Long planId,
                                       @ToolParam(description = "Fecha en formato AAAA-MM-DD") String date) {
        try {
            Long physioId = activePlan(planId).physiotherapistId();
            LocalDate day = LocalDate.parse(date);
            List<SlotResponseDTO> sameDay = availabilityService.findFreeSlots(physioId, day, day).slots().stream()
                    .filter(s -> s.startTime().toLocalDate().equals(day))
                    .toList();
            if (!sameDay.isEmpty()) {
                return new SlotsResult(date, true, toOptions(sameDay), null);
            }
            List<SlotResponseDTO> next = availabilityService.findFreeSlots(physioId, day.plusDays(1), null).slots()
                    .stream().limit(OPTIONS_WHEN_DAY_IS_FULL).toList();
            String message = next.isEmpty() ? msg("slots.none") : null;
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

    @Tool(description = "US19. Reserva una sesion del plan en un bloque libre. Llamala SOLO despues de que el "
            + "paciente confirme explicitamente el horario.")
    public ActionResult bookSession(@ToolParam(description = "Id del plan activo") Long planId,
                                    @ToolParam(description = "Id del bloque libre elegido") Long slotId) {
        try {
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
            return new ActionResult(true, null, toSessionOption(sessionService.reschedule(sessionId, new RescheduleSessionRequestDTO(newSlotId))));
        } catch (BusinessException e) {
            return new ActionResult(false, msg(e), null);
        }
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

    private String msg(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }

    private String msg(BusinessException e) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(), e.getMessageKey(),
                LocaleContextHolder.getLocale());
    }
}
