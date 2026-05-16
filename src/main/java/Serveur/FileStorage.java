package Serveur;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public final class FileStorage {

    private static final Path UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"), "uploads");

    private FileStorage() {}

    public static String saveForConversation(int idConversation, String fileName, byte[] data) throws IOException {
        Path dir = UPLOAD_ROOT.resolve("conv").resolve(String.valueOf(idConversation));
        Files.createDirectories(dir);
        Path path = dir.resolve(uniqueName(fileName));
        Files.write(path, data);
        return path.toString().replace('\\', '/');
    }

    public static String saveForGroup(int idGroupe, String fileName, byte[] data) throws IOException {
        Path dir = UPLOAD_ROOT.resolve("groups").resolve(String.valueOf(idGroupe));
        Files.createDirectories(dir);
        Path path = dir.resolve(uniqueName(fileName));
        Files.write(path, data);
        return path.toString().replace('\\', '/');
    }

    public static byte[] read(String urlFichier) throws IOException {
        if (urlFichier == null || urlFichier.isBlank()) return new byte[0];
        Path p = Paths.get(urlFichier);
        if (!p.isAbsolute()) {
            p = UPLOAD_ROOT.getParent().resolve(urlFichier).normalize();
        }
        return Files.readAllBytes(p);
    }

    public static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static String uniqueName(String fileName) {
        String safe = sanitize(fileName);
        return UUID.randomUUID() + "_" + safe;
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) return "fichier";
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
