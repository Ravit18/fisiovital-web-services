package com.healthdev.fisiovital.agent.speech;

import java.util.Arrays;

/**
 * Limpia el final del audio de Gemini TTS: a veces, despues de la ultima palabra y un silencio corto, el modelo
 * agrega una rafaga saturada de unos 200 ms que suena como un chirrido. Si en el ultimo segundo hay un silencio
 * seguido de un tramo corto que llega hasta el final, se corta en ese silencio; siempre se termina con un
 * desvanecimiento breve para que no quede un "clic".
 */
final class SpeechTail {

    /** Amplitud RMS por debajo de la cual una ventana cuenta como silencio (PCM de 16 bits). */
    static final int SILENCE_RMS = 400;
    private static final double WINDOW_SECONDS = 0.02;
    private static final double MIN_SILENCE_SECONDS = 0.08;
    private static final double SEARCH_SECONDS = 1.0;
    private static final double KEEP_SILENCE_SECONDS = 0.12;
    private static final double FADE_SECONDS = 0.03;
    /** La rafaga medida dura unos 0,2 s y llega al tope (32 767); la voz normal no pasa de ~27 000. */
    private static final double MAX_BURST_SECONDS = 0.5;
    static final int BURST_PEAK = 30_000;

    private SpeechTail() {
    }

    /** samples: PCM 16 bits little-endian mono. Devuelve una copia recortada y con desvanecimiento final. */
    static byte[] clean(byte[] samples, int sampleRate) {
        short[] pcm = toShorts(samples);
        int end = cutPoint(pcm, sampleRate);
        short[] out = Arrays.copyOf(pcm, end);
        fadeOut(out, (int) (sampleRate * FADE_SECONDS));
        return toBytes(out);
    }

    /** Indice donde termina el audio util: el inicio del ultimo silencio del final, si tras el hay basura. */
    static int cutPoint(short[] pcm, int sampleRate) {
        int window = Math.max(1, (int) (sampleRate * WINDOW_SECONDS));
        int minSilentWindows = (int) Math.ceil(MIN_SILENCE_SECONDS / WINDOW_SECONDS);
        int searchFrom = Math.max(0, pcm.length - (int) (sampleRate * SEARCH_SECONDS));

        int silentRun = 0;
        int silenceStart = -1;
        int lastSilenceStart = -1;
        int lastSilenceEnd = -1;
        for (int start = searchFrom; start + window <= pcm.length; start += window) {
            if (rms(pcm, start, start + window) < SILENCE_RMS) {
                if (silentRun == 0) {
                    silenceStart = start;
                }
                silentRun++;
                if (silentRun >= minSilentWindows) {
                    lastSilenceStart = silenceStart;
                    lastSilenceEnd = start + window;
                }
            } else {
                silentRun = 0;
            }
        }
        boolean endsInSilence = silentRun >= minSilentWindows;
        if (lastSilenceStart < 0 || endsInSilence) {
            return pcm.length;
        }
        // Solo es basura si es corta y saturada; una pausa normal antes de la ultima palabra no se toca.
        boolean shortTail = pcm.length - lastSilenceEnd <= sampleRate * MAX_BURST_SECONDS;
        if (!shortTail || peak(pcm, lastSilenceEnd, pcm.length) < BURST_PEAK) {
            return pcm.length;
        }
        return Math.min(pcm.length, lastSilenceStart + (int) (sampleRate * KEEP_SILENCE_SECONDS));
    }

    private static int peak(short[] pcm, int from, int to) {
        int max = 0;
        for (int i = from; i < to; i++) {
            max = Math.max(max, Math.abs(pcm[i]));
        }
        return max;
    }

    private static double rms(short[] pcm, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += (double) pcm[i] * pcm[i];
        }
        return Math.sqrt(sum / (to - from));
    }

    private static void fadeOut(short[] pcm, int length) {
        int n = Math.min(length, pcm.length);
        for (int i = 0; i < n; i++) {
            int index = pcm.length - n + i;
            pcm[index] = (short) (pcm[index] * (1.0 - (double) (i + 1) / n));
        }
    }

    private static short[] toShorts(byte[] bytes) {
        short[] out = new short[bytes.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
        }
        return out;
    }

    private static byte[] toBytes(short[] pcm) {
        byte[] out = new byte[pcm.length * 2];
        for (int i = 0; i < pcm.length; i++) {
            out[2 * i] = (byte) pcm[i];
            out[2 * i + 1] = (byte) (pcm[i] >> 8);
        }
        return out;
    }
}
