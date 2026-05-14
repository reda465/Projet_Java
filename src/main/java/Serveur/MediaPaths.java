package Serveur;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Répertoire de stockage des pièces jointes côté serveur (sous le répertoire de travail du process).
 */
public final class MediaPaths {

    public static final String DIR_NAME = "server_media";

    private MediaPaths() {}

    public static Path mediaRoot() throws IOException {
        Path p = Path.of(DIR_NAME).toAbsolutePath().normalize();
        Files.createDirectories(p);
        return p;
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "fichier";
        String n = name.replace("\\", "_").replace("/", "_").replace("|", "_").trim();
        if (n.length() > 180) n = n.substring(0, 180);
        return n.isEmpty() ? "fichier" : n;
    }
}
