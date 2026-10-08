package com.healthdev.fisiovital.scheduling.interfaces.rest;

import com.healthdev.fisiovital.treatment.application.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/physiotherapists/{physiotherapistId}/slots")
@RequiredArgsConstructor
public class SlotController {

    private final TreatmentService treatmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResource createSlot(@PathVariable Long physiotherapistId,
                                   @Valid @RequestBody CreateSlotRequest request) {
        return treatmentService.createSlot(physiotherapistId, request.startsAt(), request.durationMinutes());
    }
}
