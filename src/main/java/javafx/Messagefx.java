
package javafx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import util.FileActions;

import java.io.File;
//je dois voir etat de message mn ba3d
public class Messagefx {

    private static MediaPlayer lecteurAudioActif;
    private static Stage stageRef;

    public static void setStage(Stage stage) {
        stageRef = stage;
    }

    public static HBox Messageenvoyer(String text, String time) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setFont(Font.font("Arial", 14));
        msg.setTextFill(Color.BLACK);
        msg.setStyle("-fx-text-fill: black;");
        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(Color.BLACK);
        tm.setStyle("-fx-text-fill: black;");
        VBox bulle = new VBox(4, msg, tm);
        bulle.setPadding(new Insets(8));
        bulle.setMaxWidth(320);
        bulle.setStyle(
                "-fx-background-color: #D9FDD3;" +
                        "-fx-background-radius: 12 2 12 12;"+"-fx-text-fill: black;"
        );

        HBox ligne = new HBox(bulle);
        ligne.setAlignment(Pos.CENTER_RIGHT);
        ligne.setPadding(new Insets(2, 10, 2, 60));
        return ligne;
    }

    public static HBox Messagerecu(String text, String time) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setFont(Font.font("Arial", 14));
        msg.setTextFill(Color.BLACK);
        msg.setStyle("-fx-text-fill: black;");

        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(Color.web("#555555"));
        tm.setStyle("-fx-text-fill: #555555;");

        VBox bulle = new VBox(4, msg, tm);
        bulle.setPadding(new Insets(8));
        bulle.setMaxWidth(320);
        bulle.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 2 12 12 12;"+"-fx-text-fill: black;"
        );
        HBox ligne = new HBox(bulle);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(2, 60, 2, 10));
        return ligne;
    }

    public static HBox MessageenvoyerFichier(String type, String fileName, String time, File localFile) {
        return bulleFichier(true, type, fileName, time, localFile);
    }

    public static HBox MessagerecuFichier(String type, String fileName, String time, File localFile) {
        return bulleFichier(false, type, fileName, time, localFile);
    }

    private static HBox bulleFichier(boolean envoye, String type, String fileName, String time, File localFile) {
        Node contenu = contenuFichier(type, fileName, localFile);
        Label tm = new Label(time);
        tm.setFont(Font.font("Arial", 10));
        tm.setTextFill(envoye ? Color.BLACK : Color.web("#555555"));
        VBox bulle = new VBox(4, contenu, tm);
        bulle.setPadding(new Insets(8));
        bulle.setMaxWidth(320);
        bulle.setStyle(envoye
                ? "-fx-background-color: #D9FDD3; -fx-background-radius: 12 2 12 12;"
                : "-fx-background-color: white; -fx-background-radius: 2 12 12 12;");
        HBox ligne = new HBox(bulle);
        ligne.setAlignment(envoye ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(2, envoye ? 10 : 60, 2, envoye ? 60 : 10));
        return ligne;
    }

    private static Node contenuFichier(String type, String fileName, File localFile) {
        String nom = fileName != null ? fileName : "fichier";

        if (!FileActions.fichierValide(localFile)) {
            Label err = new Label("📎 " + nom + "\n(Fichier non disponible)");
            err.setWrapText(true);
            return err;
        }

        if ("image".equals(type)) {
            ImageView iv = new ImageView(new Image(localFile.toURI().toString(), 200, 200, true, true));
            iv.setPreserveRatio(true);
            iv.setOnMouseClicked(e -> FileActions.ouvrir(localFile));
            iv.setStyle("-fx-cursor: hand;");
            VBox box = new VBox(6, iv, boutonsFichier(localFile, nom));
            return box;
        }

        if ("audio".equals(type)) {
            VBox audio = lecteurAudioInline(nom, localFile);
            audio.getChildren().add(boutonsFichier(localFile, nom));
            return audio;
        }

        String icone = "video".equals(type) ? "🎬 " : "📎 ";
        Label titre = new Label(icone + nom);
        titre.setWrapText(true);
        return new VBox(6, titre, boutonsFichier(localFile, nom));
    }

    private static HBox boutonsFichier(File localFile, String nom) {
        Button btnOuvrir = new Button("Ouvrir");
        btnOuvrir.setStyle(styleBouton("#25D366"));
        btnOuvrir.setOnAction(e -> FileActions.ouvrir(localFile));

        Button btnDl = new Button("Télécharger");
        btnDl.setStyle(styleBouton("#128C7E"));
        btnDl.setOnAction(e -> FileActions.telecharger(localFile, nom, stageRef));

        Button btnDossier = new Button("📁");
        btnDossier.setStyle(styleBouton("#888888"));
        btnDossier.setTooltip(new javafx.scene.control.Tooltip("Ouvrir le dossier"));
        btnDossier.setOnAction(e -> FileActions.ouvrirDossierContenant(localFile));

        return new HBox(6, btnOuvrir, btnDl, btnDossier);
    }

    private static String styleBouton(String couleur) {
        return "-fx-background-color: " + couleur + "; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 11px;";
    }

    private static VBox lecteurAudioInline(String fileName, File localFile) {
        Label titre = new Label("🎤 " + (fileName != null ? fileName : "Message vocal"));
        titre.setWrapText(true);
        Label statut = new Label("Prêt");
        statut.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        ProgressBar progression = new ProgressBar(0);
        progression.setPrefWidth(180);
        progression.setVisible(false);
        progression.setManaged(false);

        Button btnPlay = new Button("▶ Lire");
        btnPlay.setStyle(styleBouton("#25D366"));
        Button btnStop = new Button("⏹");
        btnStop.setStyle(styleBouton("#888888"));

        Media media = new Media(localFile.toURI().toString());
        MediaPlayer player = new MediaPlayer(media);

        player.setOnReady(() -> {
            javafx.util.Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                statut.setText(formatDuree(total));
            }
        });
        player.currentTimeProperty().addListener((obs, o, cur) -> {
            javafx.util.Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                progression.setVisible(true);
                progression.setManaged(true);
                progression.setProgress(cur.toMillis() / total.toMillis());
            }
        });
        player.setOnEndOfMedia(() -> {
            statut.setText("Terminé");
            btnPlay.setText("▶ Rejouer");
            progression.setProgress(0);
        });

        btnPlay.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                statut.setText("Pause");
                btnPlay.setText("▶ Lire");
                return;
            }
            if (player.getStatus() == MediaPlayer.Status.PAUSED) {
                player.play();
                statut.setText("Lecture...");
                btnPlay.setText("⏸ Pause");
                return;
            }
            if (lecteurAudioActif != null && lecteurAudioActif != player) lecteurAudioActif.stop();
            lecteurAudioActif = player;
            player.play();
            statut.setText("Lecture...");
            btnPlay.setText("⏸ Pause");
        });

        btnStop.setOnAction(e -> {
            player.stop();
            statut.setText("Arrêté");
            btnPlay.setText("▶ Lire");
            progression.setProgress(0);
        });

        HBox controles = new HBox(6, btnPlay, btnStop);
        return new VBox(6, titre, controles, progression, statut);
    }

    private static String formatDuree(javafx.util.Duration d) {
        if (d == null || d.isUnknown()) return "Audio";
        int sec = (int) d.toSeconds();
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%d:%02d", m, s);
    }
}
