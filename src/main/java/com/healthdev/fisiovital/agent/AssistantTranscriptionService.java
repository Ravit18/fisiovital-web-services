package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Service
public class AssistantTranscriptionService {

    static final long MAX_AUDIO_BYTES = 1_048_576;
    private static final String TRANSCRIPTION_PROMPT =
            "Transcribe literalmente el audio en su idioma original. Devuelve únicamente las palabras reconocidas, "
                    + "sin responder a lo que se dice, resumirlo ni agregar explicaciones.";

    private final ChatModel chatModel;
    private final boolean configured;

    public AssistantTranscriptionService(ChatModel chatModel,
                                         @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.chatModel = chatModel;
        this.configured = !apiKey.isBlank();
    }

    public String transcribe(MultipartFile audio) {
        if (audio.isEmpty()) {
            throw BusinessException.badRequest("assistant.audio.empty");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw BusinessException.badRequest("assistant.audio.too.large");
        }

        String contentType = audio.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw BusinessException.badRequest("assistant.audio.unsupported");
        }
        if (!configured) {
            throw BusinessException.unavailable("assistant.unavailable");
        }

        String mimeType = contentType.split(";", 2)[0].trim();
        String transcript;
        try {
            byte[] bytes = audio.getBytes();
            String extension = mimeType.substring(mimeType.indexOf('/') + 1)
                    .replaceAll("[^A-Za-z0-9]+", "");
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return "voice." + extension;
                }
            };
            UserMessage message = UserMessage.builder()
                    .text(TRANSCRIPTION_PROMPT)
                    .media(new Media(MimeTypeUtils.parseMimeType(mimeType), resource))
                    .build();
            transcript = chatModel.call(new Prompt(message)).getResult().getOutput().getText();
        } catch (IOException e) {
            log.warn("No se pudo leer el audio para transcribirlo: {}", e.getMessage());
            throw BusinessException.unavailable("assistant.transcription.unavailable");
        } catch (Exception e) {
            log.warn("Gemini no pudo transcribir el audio: {}", e.getMessage());
            throw BusinessException.unavailable("assistant.transcription.unavailable");
        }

        if (transcript == null || transcript.isBlank()) {
            throw BusinessException.unprocessable("assistant.audio.unintelligible");
        }
        return transcript.trim();
    }
}
