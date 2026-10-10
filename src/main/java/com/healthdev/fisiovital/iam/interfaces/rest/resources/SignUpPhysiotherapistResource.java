package com.healthdev.fisiovital.iam.interfaces.rest.resources;

import com.healthdev.fisiovital.profiles.domain.model.Specialty;
import jakarta.validation.constraints.*;

public record SignUpPhysiotherapistResource(
        @NotBlank(message = "{validation.required}") @Size(max = 100) String fullName,
        @NotBlank(message = "{validation.required}") @Email(message = "{validation.email.invalid}") String email,
        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$", message = "{validation.password.weak}") String password,
        @Pattern(regexp = "^\\d{9}$", message = "{validation.phone.invalid}") String phone,
        @NotBlank(message = "{validation.license.required}") @Size(max = 10) String licenseNumber,
        @NotNull(message = "{validation.required}") Specialty specialty,
        @NotNull(message = "{validation.required}") @Min(0) @Max(60) Integer yearsOfExperience,
        @Size(max = 1000) String description) {
}
