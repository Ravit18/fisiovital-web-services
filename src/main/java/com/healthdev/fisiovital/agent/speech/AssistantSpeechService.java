package com.healthdev.fisiovital.agent.speech;

import com.healthdev.fisiovital.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Lee en voz alta una respuesta del asistente (boton "Escuchar" del chat). Devuelve un WAV listo para el
 * navegador; si Gemini no esta configurado o se agoto la cuota gratuita responde 503 y el front usa la voz
 * del navegador.
 */
@Slf4j
@Service
public class AssistantSpeechService {

    static final int MAX_CHARACTERS = 1000;

    private final SpeechGenerator generator;
    private final boolean configured;

    public AssistantSpeechService(SpeechGenerator generator, @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.generator = generator;
        this.configured = !apiKey.isBlank();
    }

    public byte[] speak(String text) {
        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            throw BusinessException.badRequest("validation.required");
        }
        if (clean.length() > MAX_CHARACTERS) {
            throw BusinessException.badRequest("validation.assistant.message.size");
        }
        if (!configured) {
            throw BusinessException.unavailable("assistant.speech.unavailable");
        }
        try {
            SpeechGenerator.Pcm pcm = generator.generate(clean);
            return wav(SpeechTail.clean(pcm.samples(), pcm.sampleRate()), pcm.sampleRate());
        } catch (Exception e) {
            log.warn("Gemini no pudo generar la voz: {}", e.getMessage());
            throw BusinessException.unavailable("assistant.speech.unavailable");
        }
    }

    /** Cabecera WAV (PCM 16 bits, mono) delante de las muestras para que el navegador pueda reproducirlas. */
    static byte[] wav(byte[] samples, int sampleRate) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes()).putInt(36 + samples.length).put("WAVE".getBytes());
        header.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) channels)
                .putInt(sampleRate).putInt(byteRate).putShort((short) (channels * bitsPerSample / 8))
                .putShort((short) bitsPerSample);
        header.put("data".getBytes()).putInt(samples.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + samples.length);
        out.writeBytes(header.array());
        out.writeBytes(samples);
        return out.toByteArray();
    }
}
