package com.healthdev.fisiovital.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Configuracion comun de los mappers (MapStruct genera la implementacion al compilar).
 * Cada mapper es un bean de Spring y falla la compilacion si un campo del destino queda sin mapear.
 */
@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {
}
