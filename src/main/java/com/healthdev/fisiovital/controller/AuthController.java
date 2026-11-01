package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.AuthenticatedUserResponseDTO;
import com.healthdev.fisiovital.dto.SignInRequestDTO;
import com.healthdev.fisiovital.dto.SignUpPatientRequestDTO;
import com.healthdev.fisiovital.dto.SignUpPhysiotherapistRequestDTO;
import com.healthdev.fisiovital.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registro e inicio de sesion (US01 - US03)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up/patient")
    @Operation(summary = "US01 - Registrar cuenta de paciente")
    public ResponseEntity<AuthenticatedUserResponseDTO> signUpPatient(@Valid @RequestBody SignUpPatientRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUpPatient(request));
    }

    @PostMapping("/sign-up/physiotherapist")
    @Operation(summary = "US02 - Registrar cuenta de fisioterapeuta")
    public ResponseEntity<AuthenticatedUserResponseDTO> signUpPhysiotherapist(
            @Valid @RequestBody SignUpPhysiotherapistRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUpPhysiotherapist(request));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "US03 - Iniciar sesion y obtener el token JWT")
    public ResponseEntity<AuthenticatedUserResponseDTO> signIn(@Valid @RequestBody SignInRequestDTO request) {
        return ResponseEntity.ok(authService.signIn(request));
    }
}
