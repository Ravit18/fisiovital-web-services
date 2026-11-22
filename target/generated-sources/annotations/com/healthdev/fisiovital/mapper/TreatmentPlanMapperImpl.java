package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.request.CreateTreatmentPlanRequestDTO;
import com.healthdev.fisiovital.dto.response.TreatmentPlanResponseDTO;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.TreatmentPlan;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:06-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class TreatmentPlanMapperImpl implements TreatmentPlanMapper {

    @Override
    public TreatmentPlan toEntity(CreateTreatmentPlanRequestDTO request, Patient patient, Physiotherapist physiotherapist) {
        if ( request == null && patient == null && physiotherapist == null ) {
            return null;
        }

        String diagnosis = null;
        int totalSessions = 0;
        int frequencyPerWeek = 0;
        if ( request != null ) {
            diagnosis = request.diagnosis();
            if ( request.totalSessions() != null ) {
                totalSessions = request.totalSessions();
            }
            if ( request.frequencyPerWeek() != null ) {
                frequencyPerWeek = request.frequencyPerWeek();
            }
        }
        Patient patient1 = null;
        patient1 = patient;
        Physiotherapist physiotherapist1 = null;
        physiotherapist1 = physiotherapist;

        TreatmentPlan treatmentPlan = new TreatmentPlan( patient1, physiotherapist1, diagnosis, totalSessions, frequencyPerWeek );

        return treatmentPlan;
    }

    @Override
    public TreatmentPlanResponseDTO toResponse(TreatmentPlan plan, long bookedSessions, long completedSessions) {
        if ( plan == null ) {
            return null;
        }

        Long patientId = null;
        String patientName = null;
        Long physiotherapistId = null;
        String physiotherapistName = null;
        Long id = null;
        String diagnosis = null;
        int totalSessions = 0;
        int frequencyPerWeek = 0;
        String status = null;
        LocalDateTime createdAt = null;
        if ( plan != null ) {
            patientId = planPatientId( plan );
            patientName = planPatientFullName( plan );
            physiotherapistId = planPhysiotherapistId( plan );
            physiotherapistName = planPhysiotherapistFullName( plan );
            id = plan.getId();
            diagnosis = plan.getDiagnosis();
            totalSessions = plan.getTotalSessions();
            frequencyPerWeek = plan.getFrequencyPerWeek();
            if ( plan.getStatus() != null ) {
                status = plan.getStatus().name();
            }
            createdAt = plan.getCreatedAt();
        }
        long bookedSessions1 = 0L;
        bookedSessions1 = bookedSessions;
        long completedSessions1 = 0L;
        completedSessions1 = completedSessions;

        TreatmentPlanResponseDTO treatmentPlanResponseDTO = new TreatmentPlanResponseDTO( id, patientId, patientName, physiotherapistId, physiotherapistName, diagnosis, totalSessions, frequencyPerWeek, status, createdAt, bookedSessions1, completedSessions1 );

        return treatmentPlanResponseDTO;
    }

    private Long planPatientId(TreatmentPlan treatmentPlan) {
        Patient patient = treatmentPlan.getPatient();
        if ( patient == null ) {
            return null;
        }
        return patient.getId();
    }

    private String planPatientFullName(TreatmentPlan treatmentPlan) {
        Patient patient = treatmentPlan.getPatient();
        if ( patient == null ) {
            return null;
        }
        return patient.getFullName();
    }

    private Long planPhysiotherapistId(TreatmentPlan treatmentPlan) {
        Physiotherapist physiotherapist = treatmentPlan.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getId();
    }

    private String planPhysiotherapistFullName(TreatmentPlan treatmentPlan) {
        Physiotherapist physiotherapist = treatmentPlan.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getFullName();
    }
}
