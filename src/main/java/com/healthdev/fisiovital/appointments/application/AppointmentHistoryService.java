package com.healthdev.fisiovital.appointments.application;

import com.healthdev.fisiovital.appointments.infrastructure.persistence.AppointmentRepository;
import com.healthdev.fisiovital.appointments.interfaces.rest.resources.AppointmentHistoryResource;
import com.healthdev.fisiovital.appointments.interfaces.rest.resources.AppointmentResource;
import com.healthdev.fisiovital.profiles.application.ProfileLookupService;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PatientRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import com.healthdev.fisiovital.treatment.domain.model.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentHistoryService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProfileLookupService profileLookupService;
    private final MessageSource messageSource;

    /** US14 - Historial de citas del paciente autenticado. */
    @Transactional(readOnly = true)
    public AppointmentHistoryResource forCurrentPatient() {
        Long patientId = profileLookupService.currentPatient().getId();
        return toHistory(appointmentRepository.findByPatient(patientId), "appointments.empty");
    }

    /** US15 - Historial de citas del fisioterapeuta autenticado con todos sus pacientes. */
    @Transactional(readOnly = true)
    public AppointmentHistoryResource forCurrentPhysiotherapist() {
        Long physioId = profileLookupService.currentPhysiotherapist().getId();
        return toHistory(appointmentRepository.findByPhysiotherapist(physioId), "appointments.empty");
    }

    /** US16 - Historial de citas de un paciente, buscado por su nombre completo. */
    @Transactional(readOnly = true)
    public AppointmentHistoryResource forPatient(String fullName) {
        String name = fullName.trim();
        if (!patientRepository.existsByFullNameIgnoreCase(name)) {
            throw BusinessException.notFound("appointments.patient.not.found");
        }
        return toHistory(appointmentRepository.findByPatientFullName(name), "appointments.patient.empty");
    }

    private AppointmentHistoryResource toHistory(List<Session> sessions, String emptyMessageKey) {
        LocalDateTime now = LocalDateTime.now();
        String message = sessions.isEmpty()
                ? messageSource.getMessage(emptyMessageKey, null, LocaleContextHolder.getLocale())
                : null;
        return new AppointmentHistoryResource(
                sessions.stream().map(s -> AppointmentResource.from(s, now)).toList(), message);
    }
}
