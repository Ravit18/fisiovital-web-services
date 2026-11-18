package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.ClinicalNoteResponseDTO;
import com.healthdev.fisiovital.model.ClinicalNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ClinicalNoteMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "physiotherapistId", source = "physiotherapist.id")
    @Mapping(target = "physiotherapistName", source = "physiotherapist.fullName")
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "sessionNumber", source = "session.sessionNumber")
    @Mapping(target = "sessionStartTime", source = "session.slot.startTime")
    ClinicalNoteResponseDTO toResponse(ClinicalNote note);

    List<ClinicalNoteResponseDTO> toResponseList(List<ClinicalNote> notes);
}
