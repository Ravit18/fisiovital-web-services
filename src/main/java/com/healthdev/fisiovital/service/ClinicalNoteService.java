package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.request.CreateClinicalNoteRequestDTO;
import com.healthdev.fisiovital.dto.response.ClinicalNoteResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.ClinicalNoteMapper;
import com.healthdev.fisiovital.model.ClinicalNote;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.model.SessionStatus;
import com.healthdev.fisiovital.repository.ClinicalNoteRepository;
import com.healthdev.fisiovital.repository.PatientRepository;
import com.healthdev.fisiovital.repository.SessionRepository;
import com.healthdev.fisiovital.repository.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Historia clinica del paciente (notas del fisioterapeuta). Solo la leen y escriben los fisioterapeutas que
 * tienen o tuvieron un plan con el paciente; para el resto es 403, aunque el paciente exista.
 */
@Service
@RequiredArgsConstructor
public class ClinicalNoteService {

    private final ClinicalNoteRepository noteRepository;
    private final PatientRepository patientRepository;
    private final TreatmentPlanRepository planRepository;
    private final SessionRepository sessionRepository;
    private final ProfileLookupService profileLookupService;
    private final ClinicalNoteMapper clinicalNoteMapper;

    @Transactional(readOnly = true)
    public List<ClinicalNoteResponseDTO> findByPatient(Long patientId) {
        Patient patient = treatedPatient(patientId, profileLookupService.currentPhysiotherapist());
        return clinicalNoteMapper.toResponseList(noteRepository.findByPatientIdOrderByCreatedAtDescIdDesc(patient.getId()));
    }

    @Transactional
    public ClinicalNoteResponseDTO create(Long patientId, CreateClinicalNoteRequestDTO request) {
        Physiotherapist physio = profileLookupService.currentPhysiotherapist();
        Patient patient = treatedPatient(patientId, physio);
        Session session = request.sessionId() == null ? null : sessionToNote(request.sessionId(), patient, physio);
        return clinicalNoteMapper.toResponse(
                noteRepository.save(new ClinicalNote(patient, physio, session, request.text())));
    }

    /**
     * La evolucion de una sesion la escribe quien la atendio, cuando la sesion ya empezo: debe ser del paciente
     * (400), de un plan del fisioterapeuta autenticado (403), no estar cancelada ni ser futura (422).
     * Una vez guardada, la nota no se edita: queda como registro de esa sesion.
     */
    private Session sessionToNote(Long sessionId, Patient patient, Physiotherapist physio) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound("clinical.note.session.not.found"));
        if (!session.getPlan().belongsToPatient(patient.getId())) {
            throw BusinessException.badRequest("clinical.note.session.other.patient");
        }
        if (!session.getPlan().getPhysiotherapist().getId().equals(physio.getId())) {
            throw BusinessException.forbidden("clinical.note.session.forbidden");
        }
        if (SessionStatus.CANCELLED_STATES.contains(session.getStatus())) {
            throw BusinessException.unprocessable("clinical.note.session.cancelled");
        }
        if (session.getStartTime().isAfter(LocalDateTime.now())) {
            throw BusinessException.unprocessable("clinical.note.session.future");
        }
        return session;
    }

    private Patient treatedPatient(Long patientId, Physiotherapist physio) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> BusinessException.notFound("clinical.note.patient.not.found"));
        if (!planRepository.existsByPatientIdAndPhysiotherapistId(patient.getId(), physio.getId())) {
            throw BusinessException.forbidden("clinical.note.forbidden");
        }
        return patient;
    }
}
