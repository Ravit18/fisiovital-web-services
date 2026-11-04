package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.response.AvailabilityResponseDTO;
import com.healthdev.fisiovital.dto.response.PhysiotherapistResponseDTO;
import com.healthdev.fisiovital.dto.response.PhysiotherapistSearchResponseDTO;
import com.healthdev.fisiovital.model.Specialty;
import com.healthdev.fisiovital.service.AvailabilityService;
import com.healthdev.fisiovital.service.PhysiotherapistSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/physiotherapists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Physiotherapists", description = "Busqueda de especialistas y horarios (US04 - US06)")
public class PhysiotherapistController {

    private final PhysiotherapistSearchService physiotherapistSearchService;
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "US04 - Listar fisioterapeutas, opcionalmente filtrados por especialidad")
    public ResponseEntity<PhysiotherapistSearchResponseDTO> search(
            @Parameter(description = "Especialidad a filtrar", example = "DEPORTIVA")
            @RequestParam(required = false) Specialty specialty) {
        return ResponseEntity.ok(physiotherapistSearchService.search(specialty));
    }

    @GetMapping("/{id}")
    @Operation(summary = "US05 - Ver el perfil publico de un fisioterapeuta")
    public ResponseEntity<PhysiotherapistResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(physiotherapistSearchService.profile(id));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "US06 - Consultar horarios libres (por defecto, los proximos 7 dias)")
    public ResponseEntity<AvailabilityResponseDTO> getFreeSlots(
            @PathVariable Long id,
            @Parameter(description = "Inicio del rango (AAAA-MM-DD)", example = "2026-10-19")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fin del rango (AAAA-MM-DD)", example = "2026-10-25")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(availabilityService.findFreeSlotsOfPhysiotherapist(id, from, to));
    }
}
