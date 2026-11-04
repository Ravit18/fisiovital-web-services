package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.AgendaItemResponseDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface SessionMapper {

    /** lateChange lo calcula el service antes de modificar la sesion (US11, US12). */
    @Mapping(target = "planId", source = "session.plan.id")
    @Mapping(target = "totalSessions", source = "session.plan.totalSessions")
    @Mapping(target = "slotId", source = "session.slot.id")
    @Mapping(target = "startTime", source = "session.slot.startTime")
    @Mapping(target = "endTime", source = "session.slot.endTime")
    @Mapping(target = "patientName", source = "session.plan.patient.fullName")
    @Mapping(target = "physiotherapistName", source = "session.plan.physiotherapist.fullName")
    @Mapping(target = "lateChange", source = "lateChange")
    SessionResponseDTO toResponse(Session session, boolean lateChange);

    default SessionResponseDTO toResponse(Session session) {
        return toResponse(session, false);
    }

    /** US08 - Bloque de la agenda con una sesion vigente. */
    @Mapping(target = "slotId", source = "slot.id")
    @Mapping(target = "startTime", source = "slot.startTime")
    @Mapping(target = "endTime", source = "slot.endTime")
    @Mapping(target = "status", constant = "BOOKED")
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "patientName", source = "session.plan.patient.fullName")
    @Mapping(target = "sessionNumber", source = "session.sessionNumber")
    @Mapping(target = "totalSessions", source = "session.plan.totalSessions")
    @Mapping(target = "sessionStatus", source = "session.status")
    AgendaItemResponseDTO toBookedAgendaItem(AvailabilitySlot slot, Session session);

    /** US08 - Bloque libre de la agenda; agendaStatus es FREE o FREE_CANCELLED. */
    @Mapping(target = "slotId", source = "slot.id")
    @Mapping(target = "startTime", source = "slot.startTime")
    @Mapping(target = "endTime", source = "slot.endTime")
    @Mapping(target = "status", source = "agendaStatus")
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "sessionNumber", ignore = true)
    @Mapping(target = "totalSessions", ignore = true)
    @Mapping(target = "sessionStatus", ignore = true)
    AgendaItemResponseDTO toFreeAgendaItem(AvailabilitySlot slot, String agendaStatus);
}
