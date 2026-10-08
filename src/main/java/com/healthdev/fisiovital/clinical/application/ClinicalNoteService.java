package com.healthdev.fisiovital.clinical.application;

import com.healthdev.fisiovital.clinical.domain.model.ClinicalNote;
import com.healthdev.fisiovital.clinical.infrastructure.persistence.ClinicalNoteRepository;
import com.healthdev.fisiovital.clinical.interfaces.rest.resources.ClinicalHistoryResource;
import com.healthdev.fisiovital.clinical.interfaces.rest.resources.ClinicalNoteRequestResource;
import com.healthdev.fisiovital.clinical.interfaces.rest.resources.ClinicalNoteResource;
import com.healthdev.fisiovital.profiles.application.ProfileLookupService;
import com.healthdev.fisiovital.profiles.domain.model.Patient;
import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PatientRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import com.healthdev.fisiovital.treatment.domain.model.TreatmentPlan;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.SessionRepository;
import com.healthdev.fisiovital.treatment.infrastructure.persistence.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalNoteService {

    private final ClinicalNoteRepository noteRepository;
    private final SessionRepository sessionRepository;
    private final TreatmentPlanRepository planRepository;
    private final PatientRepository patientRepository;
    private final ProfileLookupService profileLookupService;
    private final MessageSource messageSource;

    /** US14 - Registrar la evolucion de una sesion; la sesion pasa a COMPLETED. */
    @Transactional
    public ClinicalNoteResource register(Long sessionId, ClinicalNoteRequestResource request) {
        Physiotherapist physio = profileLookupService.currentPhysiotherapist();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound("session.not.found"));
        if (!session.getPlan().belongsToPhysiotherapist(physio.getId())) {
            throw BusinessException.forbidden("auth.forbidden");
        }
        if (noteRepository.existsBySessionId(sessionId)) {
            throw BusinessException.conflict("note.exists");
        }
        session.complete();
        ClinicalNote note = noteRepository.save(new ClinicalNote(session, request.observations(),
                request.exercises(), request.painLevel()));
        return ClinicalNoteResource.from(note);
    }

    /** US14 (alternativo) - Editar una nota dentro de las 24 horas. */
    @Transactional
    public ClinicalNoteResource edit(Long noteId, ClinicalNoteRequestResource request) {
        Physiotherapist physio = profileLookupService.currentPhysiotherapist();
        ClinicalNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> BusinessException.notFound("note.not.found"));
        if (!note.getSession().getPlan().belongsToPhysiotherapist(physio.getId())) {
            throw BusinessException.forbidden("auth.forbidden");
        }
        note.edit(request.observations(), request.exercises(), request.painLevel(), LocalDateTime.now());
        return ClinicalNoteResource.from(note);
    }

    /** US15 - Historial clinico, solo para el fisioterapeuta que tiene un plan con el paciente. */
    @Transactional(readOnly = true)
    public ClinicalHistoryResource history(Long patientId) {
        Physiotherapist physio = profileLookupService.currentPhysiotherapist();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> BusinessException.notFound("patient.not.found"));
        List<TreatmentPlan> plans = planRepository
                .findByPatientIdAndPhysiotherapistIdOrderByCreatedAtDesc(patientId, physio.getId());
        if (plans.isEmpty()) {
            throw BusinessException.forbidden("history.forbidden");
        }
        List<ClinicalNoteResource> notes = noteRepository.findHistory(patientId, physio.getId()).stream()
                .map(ClinicalNoteResource::from).toList();
        String message = notes.isEmpty()
                ? messageSource.getMessage("history.empty", null, LocaleContextHolder.getLocale())
                : null;
        return new ClinicalHistoryResource(patient.getId(), patient.getFullName(),
                plans.stream().map(TreatmentPlan::getDiagnosis).toList(), notes, message);
    }
}
