package com.healthdev.fisiovital.profiles.interfaces.rest;

import com.healthdev.fisiovital.profiles.application.ProfileLookupService;
import com.healthdev.fisiovital.profiles.domain.model.Physiotherapist;
import com.healthdev.fisiovital.profiles.domain.model.Specialty;
import com.healthdev.fisiovital.profiles.infrastructure.persistence.PhysiotherapistRepository;
import com.healthdev.fisiovital.profiles.interfaces.rest.resources.PhysiotherapistResource;
import com.healthdev.fisiovital.scheduling.application.AvailabilityService;
import com.healthdev.fisiovital.scheduling.interfaces.rest.resources.AvailabilityResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/physiotherapists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Physiotherapists", description = "Busqueda de especialistas y horarios (US04 - US06)")
public class PhysiotherapistController {

    private final PhysiotherapistRepository physiotherapistRepository;
    private final ProfileLookupService profileLookupService;
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "US04 - Listar fisioterapeutas, opcionalmente filtrados por especialidad")
    public ResponseEntity<List<PhysiotherapistResource>> search(@RequestParam(required = false) Specialty specialty) {
        List<Physiotherapist> result = specialty == null
                ? physiotherapistRepository.findByActiveTrueOrderByYearsOfExperienceDesc()
                : physiotherapistRepository.findBySpecialtyAndActiveTrueOrderByYearsOfExperienceDesc(specialty);
        return ResponseEntity.ok(result.stream()
                .map(p -> PhysiotherapistResource.from(p, availabilityService.hasFutureAvailability(p.getId())))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "US05 - Ver el perfil publico de un fisioterapeuta")
    public ResponseEntity<PhysiotherapistResource> getById(@PathVariable Long id) {
        Physiotherapist physio = profileLookupService.activePhysiotherapist(id);
        return ResponseEntity.ok(PhysiotherapistResource.from(physio,
                availabilityService.hasFutureAvailability(physio.getId())));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "US06 - Consultar horarios libres (por defecto, los proximos 7 dias)")
    public ResponseEntity<AvailabilityResource> getFreeSlots(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Physiotherapist physio = profileLookupService.activePhysiotherapist(id);
        return ResponseEntity.ok(availabilityService.findFreeSlots(physio.getId(), from, to));
    }
}
