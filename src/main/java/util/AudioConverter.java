package util;

import java.io.*;
import java.nio.file.*;

public final class AudioConverter {

    private AudioConverter() {}

    /**
     * Convertit un fichier WAV en MP3 via ffmpeg.
     * Retourne les bytes du MP3, ou les bytes originaux si la conversion échoue.
     */
    public static byte[] wavToMp3(byte[] wavBytes, String fileName) {
        File tempWav = null;
        File tempMp3 = null;
        try {
            // 1. Écrire le WAV dans un fichier temporaire
            tempWav = File.createTempFile("audio_in_", ".wav");
            Files.write(tempWav.toPath(), wavBytes);

            // 2. Préparer la destination MP3
            tempMp3 = File.createTempFile("audio_out_", ".mp3");

            // 3. Lancer ffmpeg
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", tempWav.getAbsolutePath(),
                    "-codec:a", "libmp3lame",
                    "-qscale:a", "2",          // qualité VBR haute (~190kbps)
                    tempMp3.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Vider stdout/stderr pour éviter le blocage
            proc.getInputStream().transferTo(OutputStream.nullOutputStream());

            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                System.err.println("[AudioConverter] ffmpeg a échoué (code " + exitCode + ") pour " + fileName);
                return wavBytes; // fallback : renvoyer le WAV original
            }

            return Files.readAllBytes(tempMp3.toPath());

        } catch (Exception e) {
            System.err.println("[AudioConverter] Erreur conversion : " + e.getMessage());
            return wavBytes; // fallback
        } finally {
            if (tempWav != null) tempWav.delete();
            if (tempMp3 != null) tempMp3.delete();
        }
    }

    public static boolean isWav(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".wav");
    }

    /** Remplace l'extension .wav par .mp3 dans le nom de fichier */
    public static String renommerEnMp3(String fileName) {
        if (fileName == null) return "audio.mp3";
        if (fileName.toLowerCase().endsWith(".wav")) {
            return fileName.substring(0, fileName.length() - 4) + ".mp3";
        }
        return fileName;
    }
}