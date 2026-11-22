package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.AgendaItemResponseDTO;
import com.healthdev.fisiovital.dto.response.SessionResponseDTO;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.model.TreatmentPlan;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:07-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class SessionMapperImpl implements SessionMapper {

    @Override
    public SessionResponseDTO toResponse(Session session, boolean lateChange) {
        if ( session == null ) {
            return null;
        }

        Long planId = null;
        int totalSessions = 0;
        Long slotId = null;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        String patientName = null;
        String physiotherapistName = null;
        Long id = null;
        int sessionNumber = 0;
        String status = null;
        if ( session != null ) {
            planId = sessionPlanId( session );
            totalSessions = sessionPlanTotalSessions( session );
            slotId = sessionSlotId( session );
            startTime = sessionSlotStartTime( session );
            endTime = sessionSlotEndTime( session );
            patientName = sessionPlanPatientFullName( session );
            physiotherapistName = sessionPlanPhysiotherapistFullName( session );
            id = session.getId();
            sessionNumber = session.getSessionNumber();
            if ( session.getStatus() != null ) {
                status = session.getStatus().name();
            }
        }
        boolean lateChange1 = false;
        lateChange1 = lateChange;

        SessionResponseDTO sessionResponseDTO = new SessionResponseDTO( id, planId, sessionNumber, totalSessions, status, slotId, startTime, endTime, patientName, physiotherapistName, lateChange1 );

        return sessionResponseDTO;
    }

    @Override
    public AgendaItemResponseDTO toBookedAgendaItem(AvailabilitySlot slot, Session session) {
        if ( slot == null && session == null ) {
            return null;
        }

        Long slotId = null;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if ( slot != null ) {
            slotId = slot.getId();
            startTime = slot.getStartTime();
            endTime = slot.getEndTime();
        }
        Long sessionId = null;
        String patientName = null;
        Integer sessionNumber = null;
        Integer totalSessions = null;
        String sessionStatus = null;
        if ( session != null ) {
            sessionId = session.getId();
            patientName = sessionPlanPatientFullName( session );
            sessionNumber = session.getSessionNumber();
            totalSessions = sessionPlanTotalSessions1( session );
            if ( session.getStatus() != null ) {
                sessionStatus = session.getStatus().name();
            }
        }

        String status = "BOOKED";

        AgendaItemResponseDTO agendaItemResponseDTO = new AgendaItemResponseDTO( slotId, startTime, endTime, status, sessionId, patientName, sessionNumber, totalSessions, sessionStatus );

        return agendaItemResponseDTO;
    }

    @Override
    public AgendaItemResponseDTO toFreeAgendaItem(AvailabilitySlot slot, String agendaStatus) {
        if ( slot == null && agendaStatus == null ) {
            return null;
        }

        Long slotId = null;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if ( slot != null ) {
            slotId = slot.getId();
            startTime = slot.getStartTime();
            endTime = slot.getEndTime();
        }
        String status = null;
        status = agendaStatus;

        Long sessionId = null;
        String patientName = null;
        Integer sessionNumber = null;
        Integer totalSessions = null;
        String sessionStatus = null;

        AgendaItemResponseDTO agendaItemResponseDTO = new AgendaItemResponseDTO( slotId, startTime, endTime, status, sessionId, patientName, sessionNumber, totalSessions, sessionStatus );

        return agendaItemResponseDTO;
    }

    private Long sessionPlanId(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        return plan.getId();
    }

    private int sessionPlanTotalSessions(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return 0;
        }
        return plan.getTotalSessions();
    }

    private Long sessionSlotId(Session session) {
        AvailabilitySlot slot = session.getSlot();
        if ( slot == null ) {
            return null;
        }
        return slot.getId();
    }

    private LocalDateTime sessionSlotStartTime(Session session) {
        AvailabilitySlot slot = session.getSlot();
        if ( slot == null ) {
            return null;
        }
        return slot.getStartTime();
    }

    private LocalDateTime sessionSlotEndTime(Session session) {
        AvailabilitySlot slot = session.getSlot();
        if ( slot == null ) {
            return null;
        }
        return slot.getEndTime();
    }

    private String sessionPlanPatientFullName(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        Patient patient = plan.getPatient();
        if ( patient == null ) {
            return null;
        }
        return patient.getFullName();
    }

    private String sessionPlanPhysiotherapistFullName(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        Physiotherapist physiotherapist = plan.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getFullName();
    }

    private Integer sessionPlanTotalSessions1(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        return plan.getTotalSessions();
    }
}
