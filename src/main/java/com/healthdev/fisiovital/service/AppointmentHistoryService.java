package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.AppointmentHistoryResponseDTO;
import com.healthdev.fisiovital.dto.AppointmentResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.AppointmentMapper;
import com.healthdev.fisiovital.model.Session;
import com.healthdev.fisiovital.repository.AppointmentRepository;
import com.healthdev.fisiovital.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final AppointmentMapper appointmentMapper;
    private final MessageSource messageSource;

    /**
     * US14 - Historial de citas del paciente autenticado, paginado para acotar la carga.
     * El timeout de la consulta se configura por propiedad; si se supera, el
     * GlobalExceptionHandler responde 503 con el mensaje appointments.slow.
     */
    @Transactional(readOnly = true)
    public AppointmentHistoryResponseDTO forCurrentPatient(int page, int size) {
        Long patientId = profileLookupService.currentPatient().getId();
        Page<Session> sessions = appointmentRepository.findByPatient(patientId, PageRequest.of(page, size));
        List<AppointmentResponseDTO> items = appointmentMapper.toResponseList(sessions.getContent(),
                LocalDateTime.now());
        String message = sessions.isEmpty() ? msg("appointments.empty") : null;
        return new AppointmentHistoryResponseDTO(items, message, page, size,
                sessions.getTotalElements(), sessions.getTotalPages());
    }

    /** US15 - Historial de citas del fisioterapeuta autenticado con todos sus pacientes. */
    @Transactional(readOnly = true)
    public AppointmentHistoryResponseDTO forCurrentPhysiotherapist() {
        Long physioId = profileLookupService.currentPhysiotherapist().getId();
        return toHistory(appointmentRepository.findByPhysiotherapist(physioId), "appointments.empty");
    }

    /** US16 - Historial de citas de un paciente, buscado por su nombre completo. */
    @Transactional(readOnly = true)
    public AppointmentHistoryResponseDTO forPatient(String fullName) {
        String name = fullName.trim();
        if (!patientRepository.existsByFullNameIgnoreCase(name)) {
            throw BusinessException.notFound("appointments.patient.not.found");
        }
        Long physioId = profileLookupService.currentPhysiotherapist().getId();
        return toHistory(appointmentRepository.findByPhysiotherapistAndPatientFullName(physioId, name),
                "appointments.patient.empty");
    }

    private AppointmentHistoryResponseDTO toHistory(List<Session> sessions, String emptyMessageKey) {
        String message = sessions.isEmpty() ? msg(emptyMessageKey) : null;
        return new AppointmentHistoryResponseDTO(
                appointmentMapper.toResponseList(sessions, LocalDateTime.now()), message);
    }

    private String msg(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
