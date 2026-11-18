package com.healthdev.fisiovital.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** sessionId es opcional: con el id, la nota es la evolucion de esa sesion; sin el, es una nota general. */
public record CreateClinicalNoteRequestDTO(
        Long sessionId,

        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.clinical.note.size}")
        String text
) {}
