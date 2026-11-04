package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.request.CreateTreatmentPlanRequestDTO;
import com.healthdev.fisiovital.dto.response.TreatmentPlanResponseDTO;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.TreatmentPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface TreatmentPlanMapper {

    /** US09 - El paciente sale del token y el fisioterapeuta se valida en el service. */
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "physiotherapist", source = "physiotherapist")
    TreatmentPlan toEntity(CreateTreatmentPlanRequestDTO request, Patient patient, Physiotherapist physiotherapist);

    @Mapping(target = "patientId", source = "plan.patient.id")
    @Mapping(target = "patientName", source = "plan.patient.fullName")
    @Mapping(target = "physiotherapistId", source = "plan.physiotherapist.id")
    @Mapping(target = "physiotherapistName", source = "plan.physiotherapist.fullName")
    @Mapping(target = "bookedSessions", source = "bookedSessions")
    @Mapping(target = "completedSessions", source = "completedSessions")
    TreatmentPlanResponseDTO toResponse(TreatmentPlan plan, long bookedSessions, long completedSessions);
}
