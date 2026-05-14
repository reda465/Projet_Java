package util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Format du champ {@code contenu} pour un fichier en groupe (sans changement de schéma SQL).
 */
public final class GroupeFichierPayload {

    public static final String PREFIX = "__FILE__;";

    private GroupeFichierPayload() {}

    public static boolean estFichier(String contenu) {
        return contenu != null && contenu.startsWith(PREFIX);
    }

    public static String creer(String typeMessage, String nomAffiche, String storageKey, long taille, String legende) {
        String nom = (nomAffiche == null ? "" : nomAffiche).replace(";", " ").replace("|", "_").trim();
        if (nom.isEmpty()) nom = "fichier";
        String key = (storageKey == null ? "" : storageKey).replace(";", "").trim();
        String capB64 = Base64.getEncoder().encodeToString((legende == null ? "" : legende).getBytes(StandardCharsets.UTF_8));
        return PREFIX + typeMessage + ";" + nom + ";" + key + ";" + taille + ";" + capB64;
    }

    public static Meta parser(String contenu) {
        if (!estFichier(contenu)) return null;
        String body = contenu.substring(PREFIX.length());
        int last = body.lastIndexOf(';');
        if (last < 0) return null;
        String capB64 = body.substring(last + 1);
        String rest = body.substring(0, last);
        String[] a = rest.split(";", 4);
        if (a.length != 4) return null;
        long sz;
        try {
            sz = Long.parseLong(a[3].trim());
        } catch (NumberFormatException e) {
            sz = 0;
        }
        String legende = "";
        try {
            legende = new String(Base64.getDecoder().decode(capB64), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return new Meta(a[0], a[1], a[2], sz, legende);
    }

    public record Meta(String typeMessage, String nomFichier, String storageKey, long taille, String legende) {}
}
