package com.healthdev.fisiovital.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpPatientResource(
        @NotBlank(message = "{validation.required}") @Size(max = 100) String fullName,
        @NotBlank(message = "{validation.required}") @Email(message = "{validation.email.invalid}") String email,
        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$", message = "{validation.password.weak}") String password,
        @Pattern(regexp = "^\\d{9}$", message = "{validation.phone.invalid}") String phone) {
}
