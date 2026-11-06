package com.healthdev.fisiovital.dto.response;

import java.util.List;

/**
 * US04 - Resultado de la busqueda de fisioterapeutas.
 * Cuando no hay especialistas activos, {@code items} viene vacio y {@code message}
 * lleva el texto i18n explicando por que no hay resultados.
 */
public record PhysiotherapistSearchResponseDTO(
        List<PhysiotherapistResponseDTO> items,
        String message
) {}
