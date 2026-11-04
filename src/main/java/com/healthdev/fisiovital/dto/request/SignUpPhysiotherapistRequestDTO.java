package com.healthdev.fisiovital.dto.request;

import com.healthdev.fisiovital.model.Specialty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(example = "{\"fullName\":\"Mariana Rojas\",\"email\":\"mariana.rojas@gmail.com\","
        + "\"password\":\"Fisio2026*\",\"phone\":\"987654321\",\"licenseNumber\":\"C-1024\","
        + "\"specialty\":\"DEPORTIVA\",\"yearsOfExperience\":8,"
        + "\"description\":\"Fisioterapeuta deportiva con enfoque en readaptacion.\"}")
public record SignUpPhysiotherapistRequestDTO(
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
