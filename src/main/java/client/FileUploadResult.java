package client;

/** Résultat d'un envoi de fichier (conversation ou groupe). */
public record FileUploadResult(boolean succes, String mode, int idMessage, String erreur) {
    public static FileUploadResult ok(String mode, int idMessage) {
        return new FileUploadResult(true, mode, idMessage, null);
    }
    public static FileUploadResult fail(String erreur) {
        return new FileUploadResult(false, "", -1, erreur);
    }
}
