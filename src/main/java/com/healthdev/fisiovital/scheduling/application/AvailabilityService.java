package com.healthdev.fisiovital.scheduling.application;

import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.scheduling.domain.model.AvailabilitySlot;
import com.healthdev.fisiovital.scheduling.domain.model.SlotStatus;
import com.healthdev.fisiovital.scheduling.infrastructure.persistence.AvailabilitySlotRepository;
import com.healthdev.fisiovital.scheduling.interfaces.rest.resources.AvailabilityResource;
import com.healthdev.fisiovital.scheduling.interfaces.rest.resources.PublishSlotsResource;
import com.healthdev.fisiovital.scheduling.interfaces.rest.resources.SlotResource;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository slotRepository;
    private final MessageSource messageSource;

    /** US07 - Publicar bloques de disponibilidad sin fechas pasadas ni cruces. */
    @Transactional
    public List<AvailabilitySlot> publish(Physiotherapist physio, PublishSlotsResource request) {
        LocalDateTime start = request.date().atTime(request.startTime());
        LocalDateTime end = request.date().atTime(request.endTime());

        if (request.date().isBefore(LocalDate.now()) || start.isBefore(LocalDateTime.now())) {
            throw BusinessException.badRequest("slots.past.date");
        }
        if (!end.isAfter(start)) {
            throw BusinessException.badRequest("slots.invalid.range");
        }
        if (slotRepository.existsOverlap(physio.getId(), start, end)) {
            throw BusinessException.conflict("slots.overlap");
        }

        List<AvailabilitySlot> slots = new ArrayList<>();
        LocalDateTime cursor = start;
        while (!cursor.plusMinutes(request.durationMinutes()).isAfter(end)) {
            slots.add(new AvailabilitySlot(physio, cursor, cursor.plusMinutes(request.durationMinutes())));
            cursor = cursor.plusMinutes(request.durationMinutes());
        }
        if (slots.isEmpty()) {
            throw BusinessException.badRequest("slots.invalid.range");
        }
        return slotRepository.saveAll(slots);
    }

    /** US06 - Consultar horarios libres; si la semana esta llena, muestra la siguiente con disponibilidad. */
    @Transactional(readOnly = true)
    public AvailabilityResource findFreeSlots(Long physioId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(6);
        LocalDateTime lowerBound = max(start.atStartOfDay(), LocalDateTime.now());

        List<AvailabilitySlot> slots = slotRepository
                .findByPhysiotherapistIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
                        physioId, SlotStatus.FREE, lowerBound, end.plusDays(1).atStartOfDay());

        if (!slots.isEmpty()) {
            return new AvailabilityResource(physioId, start, end, toResources(slots), null, null);
        }

        Optional<AvailabilitySlot> next = slotRepository
                .findFirstByPhysiotherapistIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                        physioId, SlotStatus.FREE, LocalDateTime.now());

        if (next.isEmpty()) {
            return new AvailabilityResource(physioId, start, end, List.of(), null, msg("slots.none"));
        }

        LocalDate nextStart = next.get().getStartTime().toLocalDate();
        List<AvailabilitySlot> nextWeek = slotRepository
                .findByPhysiotherapistIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
                        physioId, SlotStatus.FREE, next.get().getStartTime(), nextStart.plusDays(7).atStartOfDay());
        return new AvailabilityResource(physioId, nextStart, nextStart.plusDays(6), toResources(nextWeek),
                nextStart, msg("slots.next.week"));
    }

    @Transactional(readOnly = true)
    public boolean hasFutureAvailability(Long physioId) {
        return slotRepository.existsByPhysiotherapistIdAndStatusAndStartTimeAfter(
                physioId, SlotStatus.FREE, LocalDateTime.now());
    }

    public AvailabilitySlot getSlot(Long slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> BusinessException.notFound("slot.not.found"));
    }

    private List<SlotResource> toResources(List<AvailabilitySlot> slots) {
        return slots.stream().map(SlotResource::from).toList();
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private String msg(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
