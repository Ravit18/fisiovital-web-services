package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.PhysiotherapistResponseDTO;
import com.healthdev.fisiovital.model.Physiotherapist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface PhysiotherapistMapper {

    /** US04/US05 - Perfil publico; la disponibilidad la calcula el service. */
    @Mapping(target = "hasAvailability", source = "hasAvailability")
    @Mapping(target = "availabilityMessage", source = "availabilityMessage")
    PhysiotherapistResponseDTO toResponse(Physiotherapist physiotherapist, boolean hasAvailability,
                                          String availabilityMessage);
}
