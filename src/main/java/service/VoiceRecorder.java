package service;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Enregistrement court en WAV (PCM 16 bits mono) pour messages vocaux.
 */
public final class VoiceRecorder {

    private VoiceRecorder() {}

    public static File enregistrerVersFichier(File destination, int dureeMaxMs) throws Exception {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Micro non disponible");
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        long fin = System.currentTimeMillis() + dureeMaxMs;
        while (System.currentTimeMillis() < fin) {
            int n = line.read(buf, 0, buf.length);
            if (n > 0) bout.write(buf, 0, n);
        }
        line.stop();
        line.close();
        byte[] audio = bout.toByteArray();
        try (AudioInputStream ais = new AudioInputStream(
                new java.io.ByteArrayInputStream(audio), format, audio.length / format.getFrameSize())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, destination);
        }
        return destination;
    }
}
