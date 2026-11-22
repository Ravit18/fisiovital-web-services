package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.AppointmentResponseDTO;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.model.Specialty;
import com.healthdev.fisiovital.model.TreatmentPlan;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:08-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class AppointmentMapperImpl implements AppointmentMapper {

    @Override
    public AppointmentResponseDTO toResponse(Session session, LocalDateTime now) {
        if ( session == null ) {
            return null;
        }

        Long sessionId = null;
        int totalSessions = 0;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        Long patientId = null;
        String patientName = null;
        Long physiotherapistId = null;
        String physiotherapistName = null;
        String specialty = null;
        String diagnosis = null;
        int sessionNumber = 0;

        sessionId = session.getId();
        totalSessions = sessionPlanTotalSessions( session );
        startTime = sessionSlotStartTime( session );
        endTime = sessionSlotEndTime( session );
        patientId = sessionPlanPatientId( session );
        patientName = sessionPlanPatientFullName( session );
        physiotherapistId = sessionPlanPhysiotherapistId( session );
        physiotherapistName = sessionPlanPhysiotherapistFullName( session );
        Specialty specialty1 = sessionPlanPhysiotherapistSpecialty( session );
        if ( specialty1 != null ) {
            specialty = specialty1.name();
        }
        diagnosis = sessionPlanDiagnosis( session );
        sessionNumber = session.getSessionNumber();

        String status = session.statusAt(now).name();

        AppointmentResponseDTO appointmentResponseDTO = new AppointmentResponseDTO( sessionId, sessionNumber, totalSessions, status, startTime, endTime, patientId, patientName, physiotherapistId, physiotherapistName, specialty, diagnosis );

        return appointmentResponseDTO;
    }

    @Override
    public List<AppointmentResponseDTO> toResponseList(List<Session> sessions, LocalDateTime now) {
        if ( sessions == null ) {
            return null;
        }

        List<AppointmentResponseDTO> list = new ArrayList<AppointmentResponseDTO>( sessions.size() );
        for ( Session session : sessions ) {
            list.add( toResponse( session, now ) );
        }

        return list;
    }

    private int sessionPlanTotalSessions(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return 0;
        }
        return plan.getTotalSessions();
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

    private Long sessionPlanPatientId(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        Patient patient = plan.getPatient();
        if ( patient == null ) {
            return null;
        }
        return patient.getId();
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

    private Long sessionPlanPhysiotherapistId(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        Physiotherapist physiotherapist = plan.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getId();
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

    private Specialty sessionPlanPhysiotherapistSpecialty(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        Physiotherapist physiotherapist = plan.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getSpecialty();
    }

    private String sessionPlanDiagnosis(Session session) {
        TreatmentPlan plan = session.getPlan();
        if ( plan == null ) {
            return null;
        }
        return plan.getDiagnosis();
    }
}
