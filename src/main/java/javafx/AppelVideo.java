/*package javafx;
import client.ClientReseau;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class AppelVideo {
    public static void demarrer(Stage parent, String contactNom, ClientReseau client) {
        if (client == null || !client.estConnecte()) {
            System.out.println(" client non connecté");
            return;
        }
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel vidéo — " + contactNom);
        // Zone vidéo distante
        Rectangle videoDistant = new Rectangle(480, 270);
        videoDistant.setFill(Color.web("#1A1A2E"));
        videoDistant.setArcWidth(12);
        videoDistant.setArcHeight(12);

        Label nomDistant = new Label(contactNom);
        nomDistant.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nomDistant.setTextFill(Color.WHITE);

        Label statutVideo = new Label("Vidéo en cours...");
        statutVideo.setFont(Font.font("Arial", 12));
        statutVideo.setTextFill(Color.web("#AAAAAA"));

        VBox infos = new VBox(6, nomDistant, statutVideo);
        infos.setAlignment(Pos.CENTER);

        StackPane zoneDistante = new StackPane(videoDistant, infos);

        // Miniature locale (coin bas droit simulé)
        Rectangle videoLocal = new Rectangle(120, 68);
        videoLocal.setFill(Color.web("#075E54"));
        videoLocal.setArcWidth(8);
        videoLocal.setArcHeight(8);

        Label moiLabel = new Label("Vous");
        moiLabel.setFont(Font.font("Arial", 11));
        moiLabel.setTextFill(Color.WHITE);

        StackPane zoneMoi = new StackPane(videoLocal, moiLabel);
        zoneMoi.setAlignment(Pos.CENTER);

        // Boutons de contrôle
        Button btnMicro = new Button("🎤");
        styleBtn(btnMicro, "#444");
        btnMicro.setOnAction(e -> {
            // Toggle micro
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("VIDEO_MUTE_MIC");
            }
        });

        Button btnCam = new Button("📷");
        styleBtn(btnCam, "#444");
        btnCam.setOnAction(e -> {
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("VIDEO_MUTE_CAM");
            }
        });

        Button btnRaccrocher = new Button("📵");
        styleBtn(btnRaccrocher, "#EA2424");
        btnRaccrocher.setOnAction(e -> {
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("VIDEO_END:" + contactNom);
            }
            stage.close();
        });

        HBox controles = new HBox(16, btnMicro, btnCam, btnRaccrocher);
        controles.setAlignment(Pos.CENTER);
        controles.setPadding(new Insets(12));
        controles.setStyle("-fx-background-color: #111;");

        VBox root = new VBox(0, zoneDistante, zoneMoi, controles);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #111;");

        if (client != null && client.estConnecte()) {
            client.envoyerMessage("VIDEO_CALL:" + contactNom);
        }

        stage.setScene(new Scene(root, 500, 440));
        stage.show();
    }

    public static void recevoirAppel(Stage parent, String appelantNom, ClientReseau client) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel vidéo entrant");

        Label icone = new Label("📹");
        icone.setFont(Font.font("Arial", 48));

        Label info = new Label(appelantNom + " vous appelle en vidéo...");
        info.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        info.setTextFill(Color.web("#111B21"));

        Button btnAccepter = new Button(" Accepter");
        styleBtn(btnAccepter, "#25D366");
        btnAccepter.setOnAction(e -> {
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("VIDEO_ACCEPT:" + appelantNom);
            }
            stage.close();
            demarrer(parent, appelantNom, client);
        });

        Button btnRefuser = new Button("  Refuser");
        styleBtn(btnRefuser, "#EA2424");
        btnRefuser.setOnAction(e -> {
            if (client != null && client.estConnecte()) {
                client.envoyerMessage("VIDEO_REFUSE:" + appelantNom);
            }
            stage.close();
        });

        HBox boutons = new HBox(16, btnAccepter, btnRefuser);
        boutons.setAlignment(Pos.CENTER);

        VBox root = new VBox(18, icone, info, boutons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stage.setScene(new Scene(root, 360, 280));
        stage.show();
    }
    private static void styleBtn(Button b, String color) {
        b.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 48px;" +
                        "-fx-min-height: 48px;" +
                        "-fx-cursor: hand;"
        );
    }
}*/