package javafx;

import client.ClientHandlerAuth;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Groupe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import client.GroupVideoUDP;
import javafx.scene.image.Image;

public class AppelVideoGroupe {

    private final Map<String, StackPane> videoFeeds = new ConcurrentHashMap<>();

    private GridPane gridVideos;
    private Label statutLabel;
    private Stage stageFenetre;
    private boolean cameraActive = true;
    private boolean micActive = true;
    
    private GroupVideoUDP videoUDP;
    private int idGroupe;

    public int getIdGroupe() { return idGroupe; }
    public int getLocalPort() { return videoUDP != null ? videoUDP.getLocalPort() : -1; }

    public static AppelVideoGroupe demarrer(Stage parent, Groupe groupe, int idConversationGroupe) {
        AppelVideoGroupe instance = new AppelVideoGroupe();
        instance.idGroupe = groupe.getIdGroupe();
        instance.afficherFenetre(parent, groupe, idConversationGroupe);
        return instance;
    }

    private void afficherFenetre(Stage parent, Groupe groupe, int idConversationGroupe) {
        this.stageFenetre = new Stage();
        stageFenetre.initModality(Modality.WINDOW_MODAL);
        stageFenetre.initOwner(parent);
        stageFenetre.setTitle("Visioconférence — " + groupe.getNomGroupe());
        stageFenetre.setOnCloseRequest(e -> quitterAppel());

        // Header
        Label titre = new Label("👥 " + groupe.getNomGroupe());
        titre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titre.setTextFill(Color.WHITE);

        statutLabel = new Label("Connexion...");
        statutLabel.setTextFill(Color.WHITE);

        HBox header = new HBox(10, titre, new Region(), statutLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setStyle("-fx-background-color: #25D366;");
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        // Grille vidéos
        gridVideos = new GridPane();
        gridVideos.setHgap(10);
        gridVideos.setVgap(10);
        gridVideos.setPadding(new Insets(15));
        gridVideos.setStyle("-fx-background-color: #1a1a1a;");

        // Ajouter sa propre vidéo locale
        ajouterVideoLocale("Moi (Local)");

        ScrollPane scroll = new ScrollPane(gridVideos);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: transparent;");

        // Footer contrôles
        Button btnMic = new Button("🎤");
        btnMic.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;");
        btnMic.setOnAction(e -> {
            micActive = !micActive;
            btnMic.setStyle(micActive
                    ? "-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;"
                    : "-fx-background-color: #EA2424; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;");
        });

        Button btnCam = new Button("📹");
        btnCam.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;");
        btnCam.setOnAction(e -> {
            cameraActive = !cameraActive;
            btnCam.setStyle(cameraActive
                    ? "-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;"
                    : "-fx-background-color: #EA2424; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;");
        });

        Button btnPartager = new Button("🖥️");
        btnPartager.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;");

        Button btnQuitter = new Button("📵 Quitter");
        btnQuitter.setStyle("-fx-background-color: #EA2424; -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8px 20px;");
        btnQuitter.setOnAction(e -> quitterAppel());

        HBox footer = new HBox(15, btnMic, btnCam, btnPartager, new Region(), btnQuitter);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));
        footer.setStyle("-fx-background-color: #202020;");
        HBox.setHgrow(footer.getChildren().get(3), Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);

        Scene scene = new Scene(root, 900, 600);
        stageFenetre.setScene(scene);
        stageFenetre.show();
        
        // Start UDP
        videoUDP = new GroupVideoUDP(
            (numeroDistant, image) -> surFluxVideoRecu(numeroDistant, image),
            (imageLocale) -> surFluxVideoLocalRecu(imageLocale)
        );
        int localPort = videoUDP.demarrer();
        ClientHandlerAuth.getInstance().demarrerAppelGroupe(groupe.getIdGroupe(), "VIDEO", localPort, false);

        statutLabel.setText("Connecté • " + videoFeeds.size() + " participant(s)");
    }

    private void ajouterVideoLocale(String label) {
        // [MODIF] Stocker le StackPane wrapper, pas juste l'ImageView
        StackPane wrapper = creerVideoWrapper(label);
        videoFeeds.put("local", wrapper);
        repositionnerGrille();
    }

    public void surParticipantRejoint(String numero, String nom, String ip, int port) {
        if (videoUDP != null) videoUDP.addDestination(numero, ip, port);
        Platform.runLater(() -> {
            if (!videoFeeds.containsKey(numero)) {
                StackPane wrapper = creerVideoWrapper(nom);
                videoFeeds.put(numero, wrapper);
                repositionnerGrille();
                majStatut();
            }
        });
    }

    private void surFluxVideoRecu(String numeroDistant, Image image) {
        Platform.runLater(() -> {
            StackPane wrapper = videoFeeds.get(numeroDistant);
            if (wrapper != null) {
                ImageView iv = (ImageView) wrapper.getChildren().get(0);
                iv.setImage(image);
            }
        });
    }
    
    private void surFluxVideoLocalRecu(Image image) {
        Platform.runLater(() -> {
            StackPane wrapper = videoFeeds.get("local");
            if (wrapper != null) {
                ImageView iv = (ImageView) wrapper.getChildren().get(0);
                iv.setImage(image);
            }
        });
    }

    public void surParticipantParti(String numero) {
        if (videoUDP != null) videoUDP.removeDestination(numero);
        Platform.runLater(() -> {
            videoFeeds.remove(numero);
            repositionnerGrille();
            majStatut();
        });
    }

    // [MODIF] repositionnerGrille() utilise maintenant StackPane
    private void repositionnerGrille() {
        gridVideos.getChildren().clear();
        int col = 0, row = 0;
        int maxCols = 3;

        for (StackPane wrapper : videoFeeds.values()) {
            gridVideos.add(wrapper, col, row);
            GridPane.setHgrow(wrapper, Priority.ALWAYS);
            GridPane.setVgrow(wrapper, Priority.ALWAYS);

            col++;
            if (col >= maxCols) { col = 0; row++; }
        }
    }

    // [MODIF] creerVideoView() renommé en creerVideoWrapper() + retourne StackPane
    private StackPane creerVideoWrapper(String name) {
        ImageView iv = new ImageView();
        iv.setFitWidth(280);
        iv.setFitHeight(200);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-color: #333;");

        StackPane wrapper = new StackPane(iv);
        wrapper.setStyle("-fx-background-color: #333; -fx-border-radius: 8px; -fx-border-color: #555;");

        Label lbl = new Label(name);
        lbl.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 4px 8px; -fx-background-radius: 4px;");
        StackPane.setAlignment(lbl, Pos.BOTTOM_LEFT);
        wrapper.getChildren().add(lbl);

        return wrapper;
    }

    private void majStatut() {
        if(statutLabel != null) statutLabel.setText("Connecté • " + videoFeeds.size() + " participant(s)");
    }

    private void quitterAppel() {
        ClientHandlerAuth.getInstance().quitterAppelGroupe(idGroupe);
        if (videoUDP != null) videoUDP.arreter();
        videoFeeds.clear();
        if (stageFenetre != null) stageFenetre.close();
    }

    public void fermerFenetre() {
        if (stageFenetre != null) {
            Platform.runLater(() -> stageFenetre.close());
        }
    }
}