package com.healthdev.fisiovital.agent.speech;

/**
 * Convierte texto en voz. La implementacion real usa el modelo TTS de Gemini; en las pruebas se simula para no
 * depender de la red ni gastar cuota.
 */
public interface SpeechGenerator {

    /** Audio PCM de 16 bits mono y su frecuencia de muestreo (Gemini TTS entrega 24 000 Hz). */
    record Pcm(byte[] samples, int sampleRate) {
    }

    Pcm generate(String text);
}
