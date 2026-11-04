package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.response.AuthenticatedUserResponseDTO;
import com.healthdev.fisiovital.dto.request.SignInRequestDTO;
import com.healthdev.fisiovital.dto.request.SignUpPatientRequestDTO;
import com.healthdev.fisiovital.dto.request.SignUpPhysiotherapistRequestDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.AuthMapper;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.model.User;
import com.healthdev.fisiovital.repository.PatientRepository;
import com.healthdev.fisiovital.repository.PhysiotherapistRepository;
import com.healthdev.fisiovital.repository.UserRepository;
import com.healthdev.fisiovital.security.JwtService;
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
    private final AuthMapper authMapper;

    /** US01 - Registrar cuenta de paciente. */
    @Transactional
    public AuthenticatedUserResponseDTO signUpPatient(SignUpPatientRequestDTO request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("auth.email.duplicated");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()), Role.PATIENT));
        Patient patient = patientRepository.save(authMapper.toPatient(request, user));
        return authMapper.toResponse(user, patient.getId(), patient.getFullName(), jwtService.generateToken(user));
    }

    /** US02 - Registrar cuenta de fisioterapeuta. */
    @Transactional
    public AuthenticatedUserResponseDTO signUpPhysiotherapist(SignUpPhysiotherapistRequestDTO request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("auth.email.duplicated");
        }
        if (physiotherapistRepository.existsByLicenseNumber(request.licenseNumber().trim())) {
            throw BusinessException.conflict("auth.license.duplicated");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()), Role.PHYSIOTHERAPIST));
        Physiotherapist physio = physiotherapistRepository.save(authMapper.toPhysiotherapist(request, user));
        return authMapper.toResponse(user, physio.getId(), physio.getFullName(), jwtService.generateToken(user));
    }

    /**
     * US03 - Iniciar sesion. noRollbackFor permite guardar el contador de intentos fallidos
     * aunque se lance la excepcion de credenciales invalidas.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthenticatedUserResponseDTO signIn(SignInRequestDTO request) {
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
        return authMapper.toResponse(user, profileId, fullName, jwtService.generateToken(user));
    }
}
