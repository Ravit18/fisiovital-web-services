package com.healthdev.fisiovital.agent.speech;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** El chirrido final de Gemini TTS: voz, silencio corto y una rafaga saturada que se debe cortar. */
class SpeechTailTest {

    private static final int RATE = 24_000;

    @Test
    void trailingBurstAfterSilenceIsCut() {
        short[] pcm = concat(tone(RATE * 2, 6000), silence(RATE / 5), tone(RATE / 5, 32_000));

        int cut = SpeechTail.cutPoint(pcm, RATE);

        assertThat(cut).isBetween(RATE * 2, RATE * 2 + RATE / 5);
    }

    @Test
    void aNormalPauseBeforeTheLastWordIsNotCut() {
        short[] pcm = concat(tone(RATE * 2, 6000), silence(RATE / 4), tone(RATE / 3, 9000));

        assertThat(SpeechTail.cutPoint(pcm, RATE)).isEqualTo(pcm.length);
    }

    @Test
    void speechThatEndsCleanlyIsKeptWhole() {
        short[] pcm = concat(tone(RATE * 2, 6000), silence(RATE / 2));

        assertThat(SpeechTail.cutPoint(pcm, RATE)).isEqualTo(pcm.length);
    }

    @Test
    void speechWithoutAnyPauseIsKeptWholeAndFadedOut() {
        short[] pcm = tone(RATE, 8000);

        byte[] cleaned = SpeechTail.clean(bytes(pcm), RATE);

        assertThat(cleaned).hasSize(pcm.length * 2);
        int last = (short) ((cleaned[cleaned.length - 2] & 0xff) | (cleaned[cleaned.length - 1] << 8));
        assertThat(Math.abs(last)).isLessThan(50);
    }

    private static short[] tone(int length, int amplitude) {
        short[] out = new short[length];
        for (int i = 0; i < length; i++) {
            out[i] = (short) (amplitude * Math.sin(2 * Math.PI * 220 * i / RATE));
        }
        return out;
    }

    private static short[] silence(int length) {
        return new short[length];
    }

    private static short[] concat(short[]... parts) {
        int total = 0;
        for (short[] p : parts) {
            total += p.length;
        }
        short[] out = new short[total];
        int at = 0;
        for (short[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    private static byte[] bytes(short[] pcm) {
        byte[] out = new byte[pcm.length * 2];
        for (int i = 0; i < pcm.length; i++) {
            out[2 * i] = (byte) pcm[i];
            out[2 * i + 1] = (byte) (pcm[i] >> 8);
        }
        return out;
    }
}
