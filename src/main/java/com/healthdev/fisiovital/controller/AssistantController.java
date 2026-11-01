package com.healthdev.fisiovital.controller;

import com.healthdev.fisiovital.dto.AskAssistantRequestDTO;
import com.healthdev.fisiovital.dto.AssistantReplyResponseDTO;
import com.healthdev.fisiovital.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assistant", description = "Asistente conversacional con IA (US17 - US20)")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/messages")
    @Operation(summary = "US17-US20 - Enviar un mensaje al asistente (dudas, recomendacion de especialista, "
            + "reservar y reprogramar sesiones)")
    public ResponseEntity<AssistantReplyResponseDTO> ask(@Valid @RequestBody AskAssistantRequestDTO request) {
        return ResponseEntity.ok(assistantService.ask(request));
    }
}
