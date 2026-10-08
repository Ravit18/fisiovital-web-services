package com.healthdev.fisiovital.scheduling.interfaces.rest;

import com.healthdev.fisiovital.profiles.application.ProfileLookupService;
import com.healthdev.fisiovital.scheduling.application.AvailabilityService;
import com.healthdev.fisiovital.scheduling.interfaces.rest.resources.PublishSlotsResource;
import com.healthdev.fisiovital.scheduling.interfaces.rest.resources.SlotResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/physiotherapists/me/slots")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PHYSIOTHERAPIST')")
@Tag(name = "Availability", description = "Disponibilidad del fisioterapeuta (US07)")
public class MySlotsController {

    private final AvailabilityService availabilityService;
    private final ProfileLookupService profileLookupService;

    @PostMapping
    @Operation(summary = "US07 - Publicar bloques de disponibilidad en un dia")
    public ResponseEntity<List<SlotResource>> publish(@Valid @RequestBody PublishSlotsResource request) {
        var physio = profileLookupService.currentPhysiotherapist();
        var slots = availabilityService.publish(physio, request).stream().map(SlotResource::from).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(slots);
    }
}
