package com.healthdev.fisiovital.iam.application;

import com.healthdev.fisiovital.iam.domain.model.Role;
import com.healthdev.fisiovital.iam.domain.model.User;
import com.healthdev.fisiovital.iam.infrastructure.persistence.UserRepository;
import com.healthdev.fisiovital.iam.infrastructure.security.JwtService;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.SignInResource;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.SignUpPatientResource;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.SignUpPhysiotherapistResource;
import com.healthdev.fisiovital.profiles.domain.model.Patient;
import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PatientRepository;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PhysiotherapistRepository;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PhysiotherapistRepository physiotherapistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** US01 - Registrar cuenta de paciente. */
    @Transactional
    public AuthenticatedUserResource signUpPatient(SignUpPatientResource request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("auth.email.duplicated");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()), Role.PATIENT));
        Patient patient = patientRepository.save(new Patient(user, request.fullName(), request.phone()));
        return new AuthenticatedUserResource(user.getId(), patient.getId(), user.getEmail(), user.getRole().name(),
                patient.getFullName(), jwtService.generateToken(user));
    }

    /** US02 - Registrar cuenta de fisioterapeuta. */
    @Transactional
    public AuthenticatedUserResource signUpPhysiotherapist(SignUpPhysiotherapistResource request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("auth.email.duplicated");
        }
        if (physiotherapistRepository.existsByLicenseNumber(request.licenseNumber().trim())) {
            throw BusinessException.conflict("auth.license.duplicated");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()), Role.PHYSIOTHERAPIST));
        Physiotherapist physio = physiotherapistRepository.save(new Physiotherapist(user, request.fullName(),
                request.phone(), request.licenseNumber(), request.specialty(), request.yearsOfExperience(),
                request.description()));
        return new AuthenticatedUserResource(user.getId(), physio.getId(), user.getEmail(), user.getRole().name(),
                physio.getFullName(), jwtService.generateToken(user));
    }

    /**
     * US03 - Iniciar sesion. noRollbackFor permite guardar el contador de intentos fallidos
     * aunque se lance la excepcion de credenciales invalidas.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthenticatedUserResource signIn(SignInResource request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> BusinessException.unauthorized("auth.invalid.credentials"));

        if (user.isLocked()) {
            throw BusinessException.locked("auth.account.locked");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedAttempt();
            userRepository.save(user);
            throw BusinessException.unauthorized("auth.invalid.credentials");
        }
        user.resetFailedAttempts();
        userRepository.save(user);

        Long profileId;
        String fullName;
        if (user.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByUserId(user.getId()).orElseThrow();
            profileId = patient.getId();
            fullName = patient.getFullName();
        } else {
            Physiotherapist physio = physiotherapistRepository.findByUserId(user.getId()).orElseThrow();
            profileId = physio.getId();
            fullName = physio.getFullName();
        }
        return new AuthenticatedUserResource(user.getId(), profileId, user.getEmail(), user.getRole().name(),
                fullName, jwtService.generateToken(user));
    }
}
