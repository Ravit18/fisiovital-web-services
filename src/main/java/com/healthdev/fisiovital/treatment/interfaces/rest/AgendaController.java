package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.TreatmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/physiotherapists")
@RequiredArgsConstructor
public class AgendaController {

    private final TreatmentService treatmentService;

    @GetMapping("/{physiotherapistId}/agenda")
    public AgendaResource agenda(@PathVariable Long physiotherapistId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return treatmentService.agenda(physiotherapistId, date);
    }
}
