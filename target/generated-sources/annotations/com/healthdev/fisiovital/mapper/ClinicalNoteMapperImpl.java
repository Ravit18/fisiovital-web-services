package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.ClinicalNoteResponseDTO;
import com.healthdev.fisiovital.model.AvailabilitySlot;
import com.healthdev.fisiovital.model.ClinicalNote;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Session;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:07-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class ClinicalNoteMapperImpl implements ClinicalNoteMapper {

    @Override
    public ClinicalNoteResponseDTO toResponse(ClinicalNote note) {
        if ( note == null ) {
            return null;
        }

        Long patientId = null;
        Long physiotherapistId = null;
        String physiotherapistName = null;
        Long sessionId = null;
        Integer sessionNumber = null;
        LocalDateTime sessionStartTime = null;
        Long id = null;
        String text = null;
        LocalDateTime createdAt = null;

        patientId = notePatientId( note );
        physiotherapistId = notePhysiotherapistId( note );
        physiotherapistName = notePhysiotherapistFullName( note );
        sessionId = noteSessionId( note );
        sessionNumber = noteSessionSessionNumber( note );
        sessionStartTime = noteSessionSlotStartTime( note );
        id = note.getId();
        text = note.getText();
        createdAt = note.getCreatedAt();

        ClinicalNoteResponseDTO clinicalNoteResponseDTO = new ClinicalNoteResponseDTO( id, patientId, physiotherapistId, physiotherapistName, sessionId, sessionNumber, sessionStartTime, text, createdAt );

        return clinicalNoteResponseDTO;
    }

    @Override
    public List<ClinicalNoteResponseDTO> toResponseList(List<ClinicalNote> notes) {
        if ( notes == null ) {
            return null;
        }

        List<ClinicalNoteResponseDTO> list = new ArrayList<ClinicalNoteResponseDTO>( notes.size() );
        for ( ClinicalNote clinicalNote : notes ) {
            list.add( toResponse( clinicalNote ) );
        }

        return list;
    }

    private Long notePatientId(ClinicalNote clinicalNote) {
        Patient patient = clinicalNote.getPatient();
        if ( patient == null ) {
            return null;
        }
        return patient.getId();
    }

    private Long notePhysiotherapistId(ClinicalNote clinicalNote) {
        Physiotherapist physiotherapist = clinicalNote.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getId();
    }

    private String notePhysiotherapistFullName(ClinicalNote clinicalNote) {
        Physiotherapist physiotherapist = clinicalNote.getPhysiotherapist();
        if ( physiotherapist == null ) {
            return null;
        }
        return physiotherapist.getFullName();
    }

    private Long noteSessionId(ClinicalNote clinicalNote) {
        Session session = clinicalNote.getSession();
        if ( session == null ) {
            return null;
        }
        return session.getId();
    }

    private Integer noteSessionSessionNumber(ClinicalNote clinicalNote) {
        Session session = clinicalNote.getSession();
        if ( session == null ) {
            return null;
        }
        return session.getSessionNumber();
    }

    private LocalDateTime noteSessionSlotStartTime(ClinicalNote clinicalNote) {
        Session session = clinicalNote.getSession();
        if ( session == null ) {
            return null;
        }
        AvailabilitySlot slot = session.getSlot();
        if ( slot == null ) {
            return null;
        }
        return slot.getStartTime();
    }
}
