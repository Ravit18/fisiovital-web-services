package com.healthdev.fisiovital.profiles.application;

import com.healthdev.fisiovital.iam.application.CurrentUserProvider;
import com.healthdev.fisiovital.profiles.domain.model.Patient;
import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PatientRepository;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PhysiotherapistRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Obtiene el perfil (paciente o fisioterapeuta) del usuario autenticado. */
@Service
@RequiredArgsConstructor
public class ProfileLookupService {

    private final CurrentUserProvider currentUser;
    private final PatientRepository patientRepository;
    private final PhysiotherapistRepository physiotherapistRepository;

    public Patient currentPatient() {
        return patientRepository.findByUserId(currentUser.get().userId())
                .orElseThrow(() -> BusinessException.forbidden("auth.forbidden"));
    }

    public Physiotherapist currentPhysiotherapist() {
        return physiotherapistRepository.findByUserId(currentUser.get().userId())
                .orElseThrow(() -> BusinessException.forbidden("auth.forbidden"));
    }

    public Physiotherapist activePhysiotherapist(Long id) {
        return physiotherapistRepository.findById(id)
                .filter(Physiotherapist::isActive)
                .orElseThrow(() -> BusinessException.notFound("physio.not.found"));
    }
}
