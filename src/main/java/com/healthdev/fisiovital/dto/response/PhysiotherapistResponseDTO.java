package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = "{\"id\":1,\"fullName\":\"Mariana Rojas\",\"specialty\":\"DEPORTIVA\",\"yearsOfExperience\":8,"
        + "\"licenseNumber\":\"C-1024\",\"description\":\"Fisioterapeuta deportiva con enfoque en readaptacion.\","
        + "\"hasAvailability\":true,\"availabilityMessage\":null}")
public record PhysiotherapistResponseDTO(Long id, String fullName, String specialty, int yearsOfExperience,
                                         String licenseNumber, String description, boolean hasAvailability,
                                         String availabilityMessage) {
}
