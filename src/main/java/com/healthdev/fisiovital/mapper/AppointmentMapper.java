package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.AppointmentResponseDTO;
import com.healthdev.fisiovital.model.Session;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

/** US14-US16 - Una sesion vista como cita del historial. */
@Mapper(config = MapStructConfig.class)
public interface AppointmentMapper {

    /** now decide si una sesion reservada ya figura como realizada (Session.statusAt). */
    @Mapping(target = "sessionId", source = "id")
    @Mapping(target = "totalSessions", source = "plan.totalSessions")
    @Mapping(target = "status", expression = "java(session.statusAt(now).name())")
    @Mapping(target = "startTime", source = "slot.startTime")
    @Mapping(target = "endTime", source = "slot.endTime")
    @Mapping(target = "patientId", source = "plan.patient.id")
    @Mapping(target = "patientName", source = "plan.patient.fullName")
    @Mapping(target = "physiotherapistId", source = "plan.physiotherapist.id")
    @Mapping(target = "physiotherapistName", source = "plan.physiotherapist.fullName")
    @Mapping(target = "specialty", source = "plan.physiotherapist.specialty")
    @Mapping(target = "diagnosis", source = "plan.diagnosis")
    AppointmentResponseDTO toResponse(Session session, @Context LocalDateTime now);

    List<AppointmentResponseDTO> toResponseList(List<Session> sessions, @Context LocalDateTime now);
}
