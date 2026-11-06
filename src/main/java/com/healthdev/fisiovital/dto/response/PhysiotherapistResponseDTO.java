package com.healthdev.fisiovital.dto.response;

public record PhysiotherapistResponseDTO(
        Long id,
        String fullName,
        String specialty,
        int yearsOfExperience,
        String licenseNumber,
        String description,
        boolean hasAvailability,
        String availabilityMessage
) {}
