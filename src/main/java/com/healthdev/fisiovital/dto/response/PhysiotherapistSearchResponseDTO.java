package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * US04 - Resultado de la busqueda de fisioterapeutas.
 * Cuando no hay especialistas activos, {@code items} viene vacio y {@code message}
 * lleva el texto i18n explicando por que no hay resultados.
 */
@Schema(example = "{\"items\":[{\"id\":1,\"fullName\":\"Mariana Rojas\",\"specialty\":\"DEPORTIVA\","
        + "\"yearsOfExperience\":8,\"licenseNumber\":\"C-1024\",\"description\":\"...\","
        + "\"hasAvailability\":true,\"availabilityMessage\":null}],\"message\":null}")
public record PhysiotherapistSearchResponseDTO(List<PhysiotherapistResponseDTO> items, String message) {
}
