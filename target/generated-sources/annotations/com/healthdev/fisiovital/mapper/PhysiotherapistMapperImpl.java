package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.PhysiotherapistResponseDTO;
import com.healthdev.fisiovital.model.Physiotherapist;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:07-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class PhysiotherapistMapperImpl implements PhysiotherapistMapper {

    @Override
    public PhysiotherapistResponseDTO toResponse(Physiotherapist physiotherapist, boolean hasAvailability, String availabilityMessage) {
        if ( physiotherapist == null && availabilityMessage == null ) {
            return null;
        }

        Long id = null;
        String fullName = null;
        String specialty = null;
        int yearsOfExperience = 0;
        String licenseNumber = null;
        String description = null;
        if ( physiotherapist != null ) {
            id = physiotherapist.getId();
            fullName = physiotherapist.getFullName();
            if ( physiotherapist.getSpecialty() != null ) {
                specialty = physiotherapist.getSpecialty().name();
            }
            yearsOfExperience = physiotherapist.getYearsOfExperience();
            licenseNumber = physiotherapist.getLicenseNumber();
            description = physiotherapist.getDescription();
        }
        boolean hasAvailability1 = false;
        hasAvailability1 = hasAvailability;
        String availabilityMessage1 = null;
        availabilityMessage1 = availabilityMessage;

        PhysiotherapistResponseDTO physiotherapistResponseDTO = new PhysiotherapistResponseDTO( id, fullName, specialty, yearsOfExperience, licenseNumber, description, hasAvailability1, availabilityMessage1 );

        return physiotherapistResponseDTO;
    }
}
