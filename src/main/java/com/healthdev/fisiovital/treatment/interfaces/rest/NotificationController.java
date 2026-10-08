package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.TreatmentService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final TreatmentService treatmentService;

    @GetMapping
    public List<NotificationResource> notifications(@RequestParam @NotNull Long physiotherapistId) {
        return treatmentService.notifications(physiotherapistId);
    }
}
