package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.request.PublishSlotsRequestDTO;
import com.healthdev.fisiovital.dto.response.SlotResponseDTO;
import com.healthdev.fisiovital.service.AvailabilityService;
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

    @PostMapping
    @Operation(summary = "US07 - Publicar bloques de disponibilidad en un dia")
    public ResponseEntity<List<SlotResponseDTO>> publish(@Valid @RequestBody PublishSlotsRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.publish(request));
    }
}
