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
import client.GroupAudioUDP;
import javafx.scene.image.Image;
import model.Utilisateur;

public class AppelVideoGroupe {

    private final Map<String, StackPane> videoFeeds = new ConcurrentHashMap<>();
    private GridPane gridVideos;
    private Label statutLabel;
    private Stage stageFenetre;
    private boolean cameraActive = true;
    private boolean micActive = true;
    private GroupVideoUDP videoUDP;
    private GroupAudioUDP audioUDP; // Audio pour les appels vidéo groupe
    private int idGroupe;
    private int localAudioPort = -1;

    private Runnable onTermine;

    public int getIdGroupe() { return idGroupe; }
    public int getLocalPort() { return videoUDP != null ? videoUDP.getLocalPort() : -1; }
    public int getLocalAudioPort() { return localAudioPort; }

    public static AppelVideoGroupe demarrer(Stage parent, Groupe groupe, int idConversationGroupe, Runnable onTermine) {
        AppelVideoGroupe instance = new AppelVideoGroupe();
        instance.idGroupe  = groupe.getIdGroupe();
        instance.onTermine = onTermine;
        instance.afficherFenetre(parent, groupe, idConversationGroupe);
        return instance;
    }
    private void afficherFenetre(Stage parent, Groupe groupe, int idConversationGroupe) {
        this.stageFenetre = new Stage();
        stageFenetre.initModality(Modality.NONE); // Non-bloquant
        stageFenetre.initOwner(parent);
        stageFenetre.setTitle(" " + groupe.getNomGroupe());
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
        // Vidéo locale
        ajouterVideoLocale("Moi");
        ScrollPane scroll = new ScrollPane(gridVideos);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: transparent;");
        // Contrôles
        Button btnMic = new Button("🎤");
        btnMic.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 50px; -fx-min-height: 50px;");
        btnMic.setOnAction(e -> {
            micActive = !micActive;
            if (audioUDP != null) audioUDP.setMicroActif(micActive);
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

        // Démarrer UDP vidéo
        videoUDP = new GroupVideoUDP(
            (numeroDistant, image) -> surFluxVideoRecu(numeroDistant, image),
            (imageLocale)         -> surFluxVideoLocalRecu(imageLocale)
        );
        Utilisateur moi = client.ClientHandlerAuth.getInstance().getUtilisateurConnecte();
        if (moi != null) videoUDP.setMonNumero(moi.getNumeroTelephone());
        int localVideoPort = videoUDP.demarrer();

        // L'audio écoute sur videoPort+1 (convention fixée pour que les participants sachent où envoyer)
        audioUDP = new GroupAudioUDP();
        if (moi != null) audioUDP.setMonNumero(moi.getNumeroTelephone());
        int audioPort = audioUDP.demarrerSurPort(localVideoPort + 1);
        if (audioPort == -1) {
            System.err.println("[AppelVideoGroupe] Port audio " + (localVideoPort + 1) + " indisponible, port aléatoire...");
            audioPort = audioUDP.demarrer();
        }
        localAudioPort = audioPort;
        if (localAudioPort > 0) {
            System.out.println("[AppelVideoGroupe] Vidéo sur " + localVideoPort + ", Audio sur " + localAudioPort);
        }

        statutLabel.setText("En attente de participants...");
    }

    private void ajouterVideoLocale(String label) {
        StackPane wrapper = creerVideoWrapper(label);
        videoFeeds.put("local", wrapper);
        repositionnerGrille();
    }

    public void surParticipantRejoint(String numero, String nom, String ip, int videoPort, int audioPort) {
        if (videoUDP != null) videoUDP.addDestination(numero, ip, videoPort);
        int portAudioEffectif = audioPort > 0 ? audioPort : videoPort + 1;
        if (audioUDP != null) audioUDP.addDestination(numero, ip, portAudioEffectif);
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
        if (audioUDP != null) audioUDP.removeDestination(numero);
        Platform.runLater(() -> {
            videoFeeds.remove(numero);
            repositionnerGrille();
            majStatut();
        });
    }

    private void repositionnerGrille() {
        gridVideos.getChildren().clear();
        int col = 0, row = 0, maxCols = 3;
        for (StackPane wrapper : videoFeeds.values()) {
            gridVideos.add(wrapper, col, row);
            GridPane.setHgrow(wrapper, Priority.ALWAYS);
            GridPane.setVgrow(wrapper, Priority.ALWAYS);
            if (++col >= maxCols) { col = 0; row++; }
        }
    }

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
        if (statutLabel != null) statutLabel.setText("Connecté • " + videoFeeds.size() + " participant(s)");
    }

    /** Quitter l'appel proprement et notifier Discussion. */
    public void quitterAppel() {
        if (videoUDP != null) {
            ClientHandlerAuth.getInstance().quitterAppelGroupe(idGroupe);
            videoUDP.arreter();
            videoUDP = null;
        }
        if (audioUDP != null) {
            audioUDP.arreter();
            audioUDP = null;
        }
        videoFeeds.clear();
        Platform.runLater(() -> {
            if (stageFenetre != null && stageFenetre.isShowing()) stageFenetre.close();
        });
        // Notifier Discussion → appelVideoGroupeActif = null
        if (onTermine != null) {
            Platform.runLater(onTermine);
        }
    }
    public void fermerFenetre() {
        quitterAppel();
    }
    /** Ramène la fenêtre d'appel au premier plan si elle est ouverte. */
    public void afficherFenetre() {
        Platform.runLater(() -> {
            if (stageFenetre != null) {
                if (!stageFenetre.isShowing()) stageFenetre.show();
                stageFenetre.toFront();
                stageFenetre.requestFocus();
            }
        });
    }
}