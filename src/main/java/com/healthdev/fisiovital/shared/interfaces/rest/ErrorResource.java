package com.healthdev.fisiovital.shared.interfaces.rest;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResource(int status, String error, String message, List<String> details, LocalDateTime timestamp) {

    public static ErrorResource of(int status, String error, String message, List<String> details) {
        return new ErrorResource(status, error, message, details, LocalDateTime.now());
    }
}
