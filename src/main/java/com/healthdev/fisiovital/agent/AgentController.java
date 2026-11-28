package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.agent.dto.ChatRequest;
import com.healthdev.fisiovital.agent.dto.ChatResponse;
import com.healthdev.fisiovital.agent.dto.AssistantTranscriptionResponse;
import com.healthdev.fisiovital.agent.dto.ConversationMessage;
import com.healthdev.fisiovital.agent.dto.ConversationSummary;
import com.healthdev.fisiovital.agent.dto.SpeechRequest;
import com.healthdev.fisiovital.agent.speech.AssistantSpeechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assistant", description = "Asistente conversacional con IA (US17 - US20)")
public class AgentController {

    private final AgentService agentService;
    private final AssistantTranscriptionService transcriptionService;
    private final ConversationService conversationService;
    private final AssistantSpeechService speechService;

    @PostMapping("/messages")
    @Operation(summary = "US17-US20 - Enviar un mensaje al asistente (dudas, recomendacion de especialista, "
            + "reservar y reprogramar sesiones)")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(agentService.ask(request));
    }

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Transcribir audio para completar el cuadro de mensaje del asistente")
    public ResponseEntity<AssistantTranscriptionResponse> transcribe(@RequestParam("audio") MultipartFile audio) {
        return ResponseEntity.ok(new AssistantTranscriptionResponse(transcriptionService.transcribe(audio)));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Historial de chats del usuario, del mas reciente al mas antiguo")
    public ResponseEntity<List<ConversationSummary>> conversations() {
        return ResponseEntity.ok(conversationService.history());
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Mensajes de un chat del historial, en orden de lectura")
    public ResponseEntity<List<ConversationMessage>> messages(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.messages(id));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Eliminar un chat del historial con todos sus mensajes")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/speech", produces = "audio/wav")
    @Operation(summary = "Leer en voz alta una respuesta del asistente (Gemini TTS); 503 si no hay cuota")
    public ResponseEntity<byte[]> speech(@Valid @RequestBody SpeechRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(speechService.speak(request.text()));
    }
}
