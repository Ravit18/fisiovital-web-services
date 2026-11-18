package com.healthdev.fisiovital.dto.request;

import com.healthdev.fisiovital.model.Specialty;

@Schema(example = "{\"fullName\":\"Mariana Rojas\",\"email\":\"mariana.rojas@gmail.com\","
        + "\"password\":\"Fisio2026*\",\"phone\":\"987654321\",\"licenseNumber\":\"C-1024\","
        + "\"specialty\":\"DEPORTIVA\",\"yearsOfExperience\":8,"
        + "\"description\":\"Fisioterapeuta deportiva con enfoque en readaptacion.\"}")
public record SignUpPhysiotherapistRequestDTO(
        @NotBlank(message = "{validation.required}") @Size(max = 100) String fullName,
        @NotBlank(message = "{validation.required}") @Email(message = "{validation.email.invalid}") String email,
        @NotBlank(message = "{validation.required}")
