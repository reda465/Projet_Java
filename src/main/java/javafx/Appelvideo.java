
package javafx;
import client.ClientHandlerAuth;
import client.VideoUDP;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
public class Appelvideo {

    private static VideoUDP videoUDP = null;

    // ── Appel vidéo sortant ───────────────────────────────────────────────────
    public static void demarrer(Stage parent, String contactNom,
                                String numeroContact, int idConversation,
                                String ipDistant) {

        if (!ClientHandlerAuth.getInstance().isConnecteAuServeur()) {
            System.out.println("Impossible d'appeler : client non connecté");
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel vidéo — " + contactNom);

        Label icone = new Label("📹");
        icone.setFont(Font.font("Segoe UI", 48));

        Label nom = new Label(contactNom);
        nom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        nom.setTextFill(Color.web("#111B21"));

        Label statut = new Label("En communication vidéo...");
        statut.setFont(Font.font("Segoe UI", 13));
        statut.setTextFill(Color.web("#667781"));

        ImageView videoView = new ImageView();
        videoView.setFitWidth(420);
        videoView.setFitHeight(300);
        videoView.setPreserveRatio(true);
        videoView.setStyle("-fx-background-color: black; -fx-border-radius: 10px;");

        if (ipDistant != null && !ipDistant.isBlank()) {
            arreterVideo();
            videoUDP = new VideoUDP();
            videoUDP.demarrer(ipDistant, 5003, 5004, videoView);
            System.out.println("[Video] Démarré côté appelant → " + ipDistant);
        } else {
            System.out.println("[Video] IP distante manquante, vidéo non démarrée.");
        }

        Button btnRaccrocher = new Button("📵  Raccrocher");
        btnRaccrocher.setStyle(
                "-fx-background-color: #EA2424;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 28px;" +
                        "-fx-cursor: hand;"
        );
        btnRaccrocher.setOnAction(e -> {
            ClientHandlerAuth.getInstance().raccrocher();
            arreterVideo();
            stage.close();
        });

        stage.setOnCloseRequest(e -> {
            arreterVideo();
        });

        VBox root = new VBox(15, icone, nom, statut, videoView, btnRaccrocher);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F0F2F5;");
        if (ipDistant != null && !ipDistant.isBlank()) {
            videoUDP = new VideoUDP();
            videoUDP.demarrer(ipDistant, 5003, 5004, videoView); // ← ports cohérents avec CallService
        }
        stage.setScene(new Scene(root, 520, 500));
        stage.show();
    }

    // ── Appel vidéo entrant ───────────────────────────────────────────────────
    public static void recevoirAppel(Stage parent, String appelantNom,
                                     String numeroAppelant, String ipAppelant) {

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel vidéo entrant");

        Label icone = new Label("📹");
        icone.setFont(Font.font("Segoe UI", 48));

        Label info = new Label(appelantNom + " vous appelle en vidéo...");
        info.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        info.setTextFill(Color.web("#111B21"));

        Button btnAccepter = new Button("✅  Accepter");
        btnAccepter.setStyle(
                "-fx-background-color: #25D366;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );
        btnAccepter.setOnAction(e -> {
            ClientHandlerAuth.getInstance().accepterAppel();
            stage.close();
            ouvrirFenetreCommunication(parent, appelantNom, numeroAppelant, ipAppelant);
        });

        Button btnRefuser = new Button("❌  Refuser");
        btnRefuser.setStyle(
                "-fx-background-color: #EA2424;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );
        btnRefuser.setOnAction(e -> {
            ClientHandlerAuth.getInstance().refuserAppel();
            stage.close();
        });

        HBox boutons = new HBox(16, btnAccepter, btnRefuser);
        boutons.setAlignment(Pos.CENTER);

        VBox root = new VBox(18, icone, info, boutons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stage.setScene(new Scene(root, 420, 260));
        stage.show();
    }

    // ── Fenêtre communication vidéo ───────────────────────────────────────────
    private static void ouvrirFenetreCommunication(Stage parent,
                                                   String contactNom,
                                                   String numeroContact,
                                                   String ipDistant) {

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel vidéo — " + contactNom);

        Label icone = new Label("📹");
        icone.setFont(Font.font("Segoe UI", 48));

        Label nom = new Label(contactNom);
        nom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        nom.setTextFill(Color.web("#111B21"));

        Label statut = new Label("En communication vidéo...");
        statut.setFont(Font.font("Segoe UI", 13));
        statut.setTextFill(Color.web("#667781"));

        ImageView videoView = new ImageView();
        videoView.setFitWidth(420);
        videoView.setFitHeight(300);
        videoView.setPreserveRatio(true);
        videoView.setStyle("-fx-background-color: black; -fx-border-radius: 10px;");

        if (ipDistant != null && !ipDistant.isBlank()) {
            arreterVideo();
            videoUDP = new VideoUDP();
            videoUDP.demarrer(ipDistant, 5003, 5004, videoView);
            System.out.println("[Video] Démarré côté appelé → " + ipDistant);
        } else {
            System.out.println("[Video] IP distante manquante, vidéo non démarrée.");
        }

        Button btnRaccrocher = new Button("📵  Raccrocher");
        btnRaccrocher.setStyle(
                "-fx-background-color: #EA2424;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 28px;" +
                        "-fx-cursor: hand;"
        );
        btnRaccrocher.setOnAction(e -> {
            ClientHandlerAuth.getInstance().raccrocher();
            arreterVideo();
            stage.close();
        });

        stage.setOnCloseRequest(e -> {
            arreterVideo();
        });

        VBox root = new VBox(15, icone, nom, statut, videoView, btnRaccrocher);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stage.setScene(new Scene(root, 520, 500));
        stage.show();
    }
    // ── Arrêter la vidéo ──────────────────────────────────────────────────────
    private static void arreterVideo() {
        if (videoUDP != null) {
            videoUDP.arreter();
            videoUDP = null;
            System.out.println("[Video] Arrêté");
        }
    }
}