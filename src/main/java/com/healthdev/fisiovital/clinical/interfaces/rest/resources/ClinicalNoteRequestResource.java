package com.healthdev.fisiovital.clinical.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ejemplo: { "observations": "Dolor 4/10, flexion de rodilla 110", "exercises": "Sentadilla isometrica 3x10", "painLevel": 4 } */
public record ClinicalNoteRequestResource(
        @NotBlank(message = "{validation.observations.required}") @Size(max = 4000) String observations,
        @Size(max = 4000) String exercises,
        @Min(0) @Max(10) Integer painLevel) {
}
