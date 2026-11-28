package com.healthdev.fisiovital.agent.speech;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voz del asistente con el modelo TTS de Gemini (plan gratuito, con limites diarios). Usa el mismo cliente y la
 * misma API key que el chat. El modelo y la voz se cambian con GEMINI_TTS_MODEL y GEMINI_TTS_VOICE.
 */
@Component
public class GeminiSpeechGenerator implements SpeechGenerator {

    /*
     * Al modelo TTS se le envia SOLO el texto de la respuesta: una indicacion de estilo en el mismo texto a veces
     * se lee en voz alta. El tono calido sale de la voz elegida (GEMINI_TTS_VOICE).
     */
    private static final Pattern RATE = Pattern.compile("rate=(\\d+)");
    private static final int DEFAULT_RATE = 24_000;
    /** Voz de respaldo (calida, de la lista oficial) si Gemini rechaza la configurada. */
    static final String FALLBACK_VOICE = "Sulafat";

    private final Client client;
    private final String model;
    private final String voice;

    public GeminiSpeechGenerator(Client client,
                                 @Value("${app.ai.gemini.tts.model:gemini-3.8-flash-tts}") String model,
                                 @Value("${app.ai.gemini.tts.voice:Orbit}") String voice) {
        this.client = client;
        this.model = model;
        this.voice = voice;
    }

    @Override
    public Pcm generate(String text) {
        try {
            return generate(text, voice);
        } catch (RuntimeException e) {
            if (FALLBACK_VOICE.equals(voice) || isQuotaError(e)) {
                throw e;
            }
            // La voz configurada no existe para este modelo (por ejemplo, una voz de la app de Gemini).
            return generate(text, FALLBACK_VOICE);
        }
    }

    static boolean isQuotaError(RuntimeException e) {
        String message = String.valueOf(e.getMessage());
        return message.contains("429") || message.toLowerCase(java.util.Locale.ROOT).contains("quota");
    }

    private Pcm generate(String text, String voiceName) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities("AUDIO")
                .speechConfig(SpeechConfig.builder()
                        .voiceConfig(VoiceConfig.builder()
                                .prebuiltVoiceConfig(PrebuiltVoiceConfig.builder().voiceName(voiceName))))
                .build();
        GenerateContentResponse response = client.models.generateContent(model, text, config);
        Blob audio = response.parts().stream()
                .map(Part::inlineData)
                .flatMap(java.util.Optional::stream)
                .filter(blob -> blob.data().isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Gemini no devolvio audio"));
        return new Pcm(audio.data().get(), rateOf(audio.mimeType().orElse("")));
    }

    /** El mimeType llega como "audio/L16;codec=pcm;rate=24000". */
    static int rateOf(String mimeType) {
        Matcher m = RATE.matcher(mimeType);
        return m.find() ? Integer.parseInt(m.group(1)) : DEFAULT_RATE;
    }
}
