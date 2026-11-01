package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"fullName\":\"Carlos Mendoza\",\"email\":\"carlos.mendoza@gmail.com\","
        + "\"password\":\"Fisio2026*\",\"phone\":\"987654321\"}")
public record SignUpPatientRequestDTO(
        @NotBlank(message = "{validation.required}") @Size(max = 100) String fullName,
        @NotBlank(message = "{validation.required}") @Email(message = "{validation.email.invalid}") String email,
        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$", message = "{validation.password.weak}") String password,
        @Pattern(regexp = "^\\d{9}$", message = "{validation.phone.invalid}") String phone) {
}
