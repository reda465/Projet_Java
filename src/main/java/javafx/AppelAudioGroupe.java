package javafx;

import client.ClientHandlerAuth;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Groupe;
import model.Utilisateur;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import client.GroupAudioUDP;

public class AppelAudioGroupe {

    private final Set<String> participantsConnectes = ConcurrentHashMap.newKeySet();
    private Label statutLabel;
    private ListView<String> participantsList;
    private Stage stage;
    private GroupAudioUDP audioUDP;
    private int idGroupe;

    /** Callback appelé quand l'utilisateur quitte l'appel (bouton ou fermeture fenêtre). */
    private Runnable onTermine;

    public int getIdGroupe() { return idGroupe; }
    public int getLocalPort() { return audioUDP != null ? audioUDP.getLocalPort() : -1; }

    public static AppelAudioGroupe demarrer(Stage parent, Groupe groupe, int idConversationGroupe, Runnable onTermine) {
        AppelAudioGroupe instance = new AppelAudioGroupe();
        instance.idGroupe  = groupe.getIdGroupe();
        instance.onTermine = onTermine;
        instance.afficherFenetre(parent, groupe, idConversationGroupe);
        return instance;
    }

    private void afficherFenetre(Stage parent, Groupe groupe, int idConversationGroupe) {
        this.stage = new Stage();
        stage.initModality(Modality.NONE); // Non-bloquant pour permettre de continuer à utiliser l'app
        stage.initOwner(parent);
        stage.setTitle("Appel audio groupe — " + groupe.getNomGroupe());
        stage.setOnCloseRequest(e -> raccrocherTout());

        Label icone = new Label("👥📞");
        icone.setFont(Font.font("Segoe UI", 48));

        Label nomGroupe = new Label(groupe.getNomGroupe());
        nomGroupe.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        nomGroupe.setTextFill(Color.web("#111B21"));

        statutLabel = new Label("Connexion au serveur vocal...");
        statutLabel.setFont(Font.font("Segoe UI", 14));
        statutLabel.setTextFill(Color.web("#667781"));

        participantsList = new ListView<>();
        participantsList.setPrefHeight(150);
        participantsList.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        majListeParticipants(groupe);

        Button btnRaccrocher = new Button("📵 Quitter l'appel");
        btnRaccrocher.setStyle(
                "-fx-background-color: #EA2424; -fx-text-fill: white; -fx-font-size: 14px;" +
                        "-fx-background-radius: 30px; -fx-padding: 10px 28px; -fx-cursor: hand;"
        );
        btnRaccrocher.setOnAction(e -> raccrocherTout());

        VBox root = new VBox(15, icone, nomGroupe, statutLabel, new Label("Participants:"), participantsList, btnRaccrocher);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stage.setScene(new Scene(root, 350, 500));
        stage.show();

        // Démarrer UDP audio
        audioUDP = new GroupAudioUDP();
        Utilisateur moi = ClientHandlerAuth.getInstance().getUtilisateurConnecte();
        if (moi != null) {
            audioUDP.setMonNumero(moi.getNumeroTelephone());
            participantsConnectes.add(moi.getNumeroTelephone());
        }
        int localPort = audioUDP.demarrer();

        // NE PAS appeler demarrerAppelGroupe ici.
        // C'est Discussion qui l'appelle APRES l'assignation de appelAudioGroupeActif.

        statutLabel.setText("Appel en cours • " + participantsConnectes.size() + " participants");

    }

    public void notifierMembreRejoint(String numero, String nom, String ip, int port) {
        if (audioUDP != null) audioUDP.addDestination(numero, ip, port);
        participantsConnectes.add(numero);
        Platform.runLater(() -> {
            majListeParticipants(null);
            if (statutLabel != null) statutLabel.setText("Appel en cours • " + participantsConnectes.size() + " participants");
        });
    }

    public void notifierMembreParti(String numero) {
        if (audioUDP != null) audioUDP.removeDestination(numero);
        participantsConnectes.remove(numero);
        Platform.runLater(() -> {
            majListeParticipants(null);
            if (statutLabel != null) statutLabel.setText("Appel en cours • " + participantsConnectes.size() + " participants");
        });
    }

    private void majListeParticipants(Groupe groupe) {
        if (participantsList == null) return;
        participantsList.getItems().clear();
        for (String num : participantsConnectes) {
            participantsList.getItems().add("🟢 " + num + " (connecté)");
        }
        if (groupe != null && groupe.getNumerosMembres() != null) {
            for (String num : groupe.getNumerosMembres()) {
                if (!participantsConnectes.contains(num)) {
                    participantsList.getItems().add("⚪ " + num + " (hors ligne)");
                }
            }
        }
    }

    /** Quitter l'appel proprement et notifier Discussion. */
    public void raccrocherTout() {
        if (audioUDP != null) {
            ClientHandlerAuth.getInstance().quitterAppelGroupe(idGroupe);
            audioUDP.arreter();
            audioUDP = null;
        }
        participantsConnectes.clear();
        Platform.runLater(() -> {
            if (stage != null && stage.isShowing()) stage.close();
        });
        // Notifier Discussion que l'appel est terminé → appelAudioGroupeActif = null
        if (onTermine != null) {
            Platform.runLater(onTermine);
        }
    }

    public void fermerFenetre() {
        raccrocherTout();
    }
}