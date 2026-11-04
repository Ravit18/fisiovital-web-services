package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(int status, String error, String message, List<String> details, LocalDateTime timestamp) {

    public static ErrorResponseDTO of(int status, String error, String message, List<String> details) {
        return new ErrorResponseDTO(status, error, message, details, LocalDateTime.now());
    }
}
