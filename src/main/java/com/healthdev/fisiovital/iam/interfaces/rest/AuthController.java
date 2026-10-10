package com.healthdev.fisiovital.iam.interfaces.rest;

import com.healthdev.fisiovital.iam.application.AuthService;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.SignInResource;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.SignUpPatientResource;
import com.healthdev.fisiovital.iam.interfaces.rest.resources.SignUpPhysiotherapistResource;
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
    public ResponseEntity<AuthenticatedUserResource> signUpPatient(@Valid @RequestBody SignUpPatientResource request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUpPatient(request));
    }

    @PostMapping("/sign-up/physiotherapist")
    @Operation(summary = "US02 - Registrar cuenta de fisioterapeuta")
    public ResponseEntity<AuthenticatedUserResource> signUpPhysiotherapist(
            @Valid @RequestBody SignUpPhysiotherapistResource request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUpPhysiotherapist(request));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "US03 - Iniciar sesion y obtener el token JWT")
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource request) {
        return ResponseEntity.ok(authService.signIn(request));
    }
}
