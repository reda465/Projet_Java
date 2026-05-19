package util;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class FileActions {

    private FileActions() {}

    public static boolean fichierValide(File file) {
        return file != null && file.isFile() && file.exists() && file.length() > 0;
    }

    public static void ouvrir(File file) {
        if (!fichierValide(file)) {
            alerte("Fichier introuvable", "Le fichier n'est pas disponible sur cet appareil.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return;
            }
        } catch (Exception e) {
            System.out.println("[FILE] Desktop.open échoué : " + e.getMessage());
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", file.getAbsolutePath()});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", file.getAbsolutePath()});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", file.getAbsolutePath()});
            }
        } catch (Exception e) {
            alerte("Ouverture impossible", "Impossible d'ouvrir le fichier :\n" + file.getAbsolutePath());
        }
    }

    public static void telecharger(File source, String nomSuggere, Stage stage) {
        if (!fichierValide(source)) {
            alerte("Téléchargement impossible", "Le fichier source est introuvable.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le fichier");
        chooser.setInitialFileName(nomSuggere != null ? nomSuggere : source.getName());
        File dest = chooser.showSaveDialog(stage);
        if (dest == null) return;
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            alerte(Alert.AlertType.INFORMATION, "Téléchargement", "Fichier enregistré :\n" + dest.getAbsolutePath());
        } catch (Exception e) {
            alerte("Erreur", "Impossible d'enregistrer : " + e.getMessage());
        }
    }

    public static void ouvrirDossierContenant(File file) {
        if (!fichierValide(file)) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception e) {
            ouvrir(file);
        }
    }

    private static void alerte(String titre, String msg) {
        alerte(Alert.AlertType.ERROR, titre, msg);
    }

    private static void alerte(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
