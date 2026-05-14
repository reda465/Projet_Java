package Serveur;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Réception des morceaux d'upload (un client = une session à la fois côté handler).
 */
public final class ChunkedUploadBuffer {

    public static final long MAX_BYTES = 80L * 1024 * 1024;

    public enum Mode {
        IND, GRP
    }

    public static final class Session {
        public final String sessionId;
        public final Mode mode;
        public final String destTel;
        public final int idGroupe;
        public final long expectedSize;
        public final String originalName;
        public final String typeMessage;
        public final String captionPlain;
        public long received;
        public Path tempPath;
        public OutputStream out;

        Session(String sessionId, Mode mode, String destTel, int idGroupe, long expectedSize,
                String originalName, String typeMessage, String captionPlain) {
            this.sessionId = sessionId;
            this.mode = mode;
            this.destTel = destTel;
            this.idGroupe = idGroupe;
            this.expectedSize = expectedSize;
            this.originalName = originalName;
            this.typeMessage = typeMessage;
            this.captionPlain = captionPlain;
        }
    }

    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private ChunkedUploadBuffer() {}

    public static Session begin(String sessionId, Mode mode, String destTel, int idGroupe,
                                long expectedSize, String originalName, String typeMessage, String captionPlain)
            throws IOException {
        if (expectedSize < 0 || expectedSize > MAX_BYTES) {
            throw new IOException("SIZE");
        }
        Path root = MediaPaths.mediaRoot();
        Path temp = root.resolve("tmp_" + sessionId + ".part");
        Session s = new Session(sessionId, mode, destTel, idGroupe, expectedSize,
                MediaPaths.sanitizeFileName(originalName), typeMessage, captionPlain == null ? "" : captionPlain);
        s.tempPath = temp;
        s.out = new BufferedOutputStream(Files.newOutputStream(temp));
        s.received = 0;
        SESSIONS.put(sessionId, s);
        return s;
    }

    public static void appendChunk(String sessionId, String base64Chunk) throws IOException {
        Session s = SESSIONS.get(sessionId);
        if (s == null || s.out == null) throw new IOException("NO_SESSION");
        byte[] data = Base64.getDecoder().decode(base64Chunk);
        if (s.received + data.length > s.expectedSize + 1024) {
            throw new IOException("OVERFLOW");
        }
        s.out.write(data);
        s.received += data.length;
    }

    /**
     * Finalise l'upload : renvoie le nom de fichier stocké sous {@link MediaPaths#DIR_NAME} (pas un chemin absolu).
     */
    public record ResolvedUpload(Mode mode, String destTel, int idGroupe, String originalName,
                                   String typeMessage, String captionPlain, String storedFilename) {}

    public static Session peek(String sessionId) {
        return SESSIONS.get(sessionId);
    }

    public static ResolvedUpload endSession(String sessionId) throws IOException {
        Session s = SESSIONS.remove(sessionId);
        if (s == null || s.tempPath == null) throw new IOException("NO_SESSION");
        try {
            s.out.flush();
            s.out.close();
        } catch (IOException ignored) {
        }
        s.out = null;
        if (s.received != s.expectedSize) {
            Files.deleteIfExists(s.tempPath);
            throw new IOException("SIZE_MISMATCH");
        }
        String storageKey = UUID.randomUUID().toString().replace("-", "");
        String safe = MediaPaths.sanitizeFileName(s.originalName);
        String storedName = storageKey + "_" + safe;
        Path root = MediaPaths.mediaRoot();
        Path dest = root.resolve(storedName);
        Files.move(s.tempPath, dest, StandardCopyOption.REPLACE_EXISTING);
        return new ResolvedUpload(s.mode, s.destTel, s.idGroupe, s.originalName, s.typeMessage, s.captionPlain, storedName);
    }

    public static void cancel(String sessionId) {
        Session s = SESSIONS.remove(sessionId);
        if (s == null) return;
        try {
            if (s.out != null) s.out.close();
        } catch (IOException ignored) {
        }
        if (s.tempPath != null) {
            try {
                Files.deleteIfExists(s.tempPath);
            } catch (IOException ignored) {
            }
        }
    }
}
