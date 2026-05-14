package util;

import java.io.File;

/**
 * Normalise le type de message pour l'insertion en base : beaucoup de schémas MySQL utilisent
 * un ENUM ou VARCHAR court (ex. uniquement {@code texte}, {@code image}, {@code video}, {@code fichier}).
 * Les valeurs comme {@code document} ou {@code audio} provoquent alors "Data truncated for column 'type_message'".
 */
public final class SqlMessageTypeUtil {

    private SqlMessageTypeUtil() {}

    /** Valeur à persister dans {@code messages.type_message}. */
    public static String pourStockageIndividuel(String typeAppli) {
        if (typeAppli == null || typeAppli.isBlank()) return "fichier";
        String t = typeAppli.trim().toLowerCase();
        if ("texte".equals(t)) return "texte";
        if ("image".equals(t)) return "image";
        if ("video".equals(t)) return "video";
        return "fichier";
    }

    /** Pour l'affichage : retrouver image / audio / document à partir du type stocké et du nom de fichier. */
    public static String pourAffichage(String typeStocke, String nomFichier) {
        if (typeStocke != null) {
            String s = typeStocke.trim().toLowerCase();
            if ("image".equals(s) || "video".equals(s)) return s;
            if ("texte".equals(s)) return "texte";
        }
        if (nomFichier == null || nomFichier.isBlank()) return "document";
        return detecterDepuisNom(nomFichier);
    }

    public static String detecterDepuisNom(String nom) {
        String n = nom.toLowerCase();
        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".webp"))
            return "image";
        if (n.endsWith(".mp4") || n.endsWith(".webm") || n.endsWith(".mov")) return "video";
        if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg") || n.endsWith(".m4a")) return "audio";
        return "document";
    }

    public static String detecterDepuisFichier(File f) {
        return f != null ? detecterDepuisNom(f.getName()) : "document";
    }
}
