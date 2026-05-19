package util;

import java.io.File;
import java.util.Locale;
import java.util.Set;

public final class FileMediaUtil {

    public static final String GROUP_FILE_MARKER = "__FILE__";
    /** Séparateur d'enregistrements pour GROUP_MESSAGES_LIST (évite les conflits avec | dans le contenu). */
    public static final String GROUP_MSG_RECORD_SEP = "\u001e";
    public static final int CHUNK_SIZE = 48 * 1024;

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "avi", "mkv", "webm", "3gp");
    private static final Set<String> AUDIO_EXT = Set.of("wav", "mp3", "ogg", "m4a", "aac", "opus", "webm");

    private FileMediaUtil() {}

    public static String detectType(File file) {
        return detectType(file != null ? file.getName() : "");
    }

    public static String detectType(String fileName) {
        String ext = extension(fileName);
        if (IMAGE_EXT.contains(ext)) return "image";
        if (VIDEO_EXT.contains(ext)) return "video";
        if (AUDIO_EXT.contains(ext)) return "audio";
        return "fichier";
    }

    public static String extension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isGroupFileContent(String contenu) {
        return contenu != null && contenu.startsWith(GROUP_FILE_MARKER + "|");
    }

    /** Parse __FILE__|type|nom|url */
    public static String[] parseGroupFileContent(String contenu) {
        if (!isGroupFileContent(contenu)) return null;
        String[] p = contenu.split("\\|", 4);
        if (p.length < 4) return null;
        return new String[]{p[1], p[2], p[3]};
    }

    public static String buildGroupFileContent(String type, String nom, String url) {
        return GROUP_FILE_MARKER + "|" + type + "|" + nom + "|" + url;
    }

    /** Nettoie un champ du protocole réseau (les | cassent le découpage des paquets). */
    public static String safeProtocolField(String value) {
        if (value == null) return "";
        return value.replace("|", " ").replace('\n', ' ').replace('\r', ' ');
    }

    public static String labelForType(String type) {
        if (type == null) return "📎 Fichier";
        return switch (type) {
            case "image" -> "🖼 Image";
            case "video" -> "🎬 Vidéo";
            case "audio" -> "🎤 Audio";
            default -> "📎 Fichier";
        };
    }
}
