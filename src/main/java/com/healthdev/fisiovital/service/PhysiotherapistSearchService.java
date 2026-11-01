package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.PhysiotherapistResponseDTO;
import com.healthdev.fisiovital.dto.PhysiotherapistSearchResponseDTO;
import com.healthdev.fisiovital.mapper.PhysiotherapistMapper;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Specialty;
import com.healthdev.fisiovital.repository.PhysiotherapistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Busqueda de especialistas (US04) y perfil publico con su disponibilidad (US05). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhysiotherapistSearchService {

    private final PhysiotherapistRepository physiotherapistRepository;
    private final ProfileLookupService profileLookupService;
    private final AvailabilityService availabilityService;
    private final PhysiotherapistMapper physiotherapistMapper;
    private final MessageSource messageSource;

    /** US04 - Activos ordenados por anios de experiencia; si no hay, devuelve el mensaje i18n. */
    public PhysiotherapistSearchResponseDTO search(Specialty specialty) {
        List<Physiotherapist> result = specialty == null
                ? physiotherapistRepository.findByActiveTrueOrderByYearsOfExperienceDesc()
                : physiotherapistRepository.findBySpecialtyAndActiveTrueOrderByYearsOfExperienceDesc(specialty);
        List<PhysiotherapistResponseDTO> items = result.stream().map(this::toResponse).toList();
        String message = items.isEmpty() ? msg("physio.search.empty") : null;
        return new PhysiotherapistSearchResponseDTO(items, message);
    }

    /** US05 - Perfil publico de un fisioterapeuta activo. */
    public PhysiotherapistResponseDTO profile(Long id) {
        return toResponse(profileLookupService.activePhysiotherapist(id));
    }

    /** Construye el DTO publico con su disponibilidad; si no hay, anexa el mensaje i18n (US05). */
    private PhysiotherapistResponseDTO toResponse(Physiotherapist p) {
        boolean hasAvailability = availabilityService.hasFutureAvailability(p.getId());
        String availabilityMessage = hasAvailability ? null : msg("physio.availability.none");
        return physiotherapistMapper.toResponse(p, hasAvailability, availabilityMessage);
    }

    private String msg(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
