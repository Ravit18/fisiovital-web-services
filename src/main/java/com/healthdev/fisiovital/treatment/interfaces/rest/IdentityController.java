package com.healthdev.fisiovital.treatment.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IdentityController {

    private final TreatmentService treatmentService;

    @PostMapping("/patients")
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityResource createPatient(@Valid @RequestBody IdentityRequest request) {
        return treatmentService.createPatient(request.name());
    }

    @PostMapping("/physiotherapists")
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityResource createPhysiotherapist(@Valid @RequestBody IdentityRequest request) {
        return treatmentService.createPhysiotherapist(request.name());
    }
}
