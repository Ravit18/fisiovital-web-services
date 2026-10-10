package com.healthdev.fisiovital.profiles.interfaces.rest.resources;

import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;

public record PhysiotherapistResource(Long id, String fullName, String specialty, int yearsOfExperience,
                                      String licenseNumber, String description, boolean hasAvailability) {

    public static PhysiotherapistResource from(Physiotherapist p, boolean hasAvailability) {
        return new PhysiotherapistResource(p.getId(), p.getFullName(), p.getSpecialty().name(),
                p.getYearsOfExperience(), p.getLicenseNumber(), p.getDescription(), hasAvailability);
    }
}
