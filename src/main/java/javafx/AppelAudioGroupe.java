package javafx;

import client.ClientHandlerAuth;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
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

public class AppelAudioGroupe {

    // Participants connectés (numero -> nom)
    private final Set<String> participantsConnectes = ConcurrentHashMap.newKeySet();
    private Label statutLabel;
    private ListView<String> participantsList;
    private Stage stage;  // [NOUVEAU] Référence au stage pour fermeture externe

    public static void demarrer(Stage parent, Groupe groupe, int idConversationGroupe) {
        new AppelAudioGroupe().afficherFenetre(parent, groupe, idConversationGroupe);
    }

    private void afficherFenetre(Stage parent, Groupe groupe, int idConversationGroupe) {
        this.stage = new Stage();  // [NOUVEAU] Stocker la référence
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Appel audio groupe — " + groupe.getNomGroupe());
        stage.setOnCloseRequest(e -> raccrocherTout(stage));

        // UI Principal
        Label icone = new Label("👥📞");
        icone.setFont(Font.font("Segoe UI", 48));

        Label nomGroupe = new Label(groupe.getNomGroupe());
        nomGroupe.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        nomGroupe.setTextFill(Color.web("#111B21"));

        statutLabel = new Label("Connexion au serveur vocal...");
        statutLabel.setFont(Font.font("Segoe UI", 14));
        statutLabel.setTextFill(Color.web("#667781"));

        // Liste participants
        participantsList = new ListView<>();
        participantsList.setPrefHeight(150);
        participantsList.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        majListeParticipants(groupe);

        Button btnRaccrocher = new Button("📵 Quitter l'appel");
        btnRaccrocher.setStyle(
                "-fx-background-color: #EA2424; -fx-text-fill: white; -fx-font-size: 14px;" +
                        "-fx-background-radius: 30px; -fx-padding: 10px 28px; -fx-cursor: hand;"
        );
        btnRaccrocher.setOnAction(e -> raccrocherTout(stage));

        VBox root = new VBox(15, icone, nomGroupe, statutLabel, new Label("Participants:"), participantsList, btnRaccrocher);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F0F2F5;");

        stage.setScene(new Scene(root, 350, 500));
        stage.show();

        // [NOUVEAU] Notifier le serveur que l'appel est démarré
        // ClientHandlerAuth.getInstance().demarrerAppelGroupe(groupe.getIdGroupe(), "AUDIO");

        // [NOUVEAU] Ajouter l'utilisateur local comme participant
        Utilisateur moi = ClientHandlerAuth.getInstance().getUtilisateurConnecte();
        if (moi != null) {
            participantsConnectes.add(moi.getNumeroTelephone());
        }

        statutLabel.setText("Appel en cours • " + participantsConnectes.size() + " participants");
    }

    /** Appelé par EcouteurClient quand un membre rejoint l'appel */
    public void notifierMembreRejoint(String numero, String nom) {
        participantsConnectes.add(numero);
        Platform.runLater(() -> {
            majListeParticipants(null); // Rafraîchir
            if (statutLabel != null) statutLabel.setText("Appel en cours • " + participantsConnectes.size() + " participants");
        });
    }

    /** Appelé par EcouteurClient quand un membre quitte */
    public void notifierMembreParti(String numero) {
        participantsConnectes.remove(numero);
        Platform.runLater(() -> {
            majListeParticipants(null);
            if (statutLabel != null) statutLabel.setText("Appel en cours • " + participantsConnectes.size() + " participants");
        });
    }

    private void majListeParticipants(Groupe groupe) {
        if (participantsList == null) return;
        participantsList.getItems().clear();

        // [NOUVEAU] Afficher les participants connectés avec indicateur vert
        for (String num : participantsConnectes) {
            participantsList.getItems().add("🟢 " + num + " (connecté)");
        }

        // [NOUVEAU] Afficher les membres du groupe non connectés avec indicateur gris
        if (groupe != null && groupe.getNumerosMembres() != null) {
            for (String num : groupe.getNumerosMembres()) {
                if (!participantsConnectes.contains(num)) {
                    participantsList.getItems().add("⚪ " + num + " (hors ligne)");
                }
            }
        }
    }

    private void raccrocherTout(Stage stage) {
        // [NOUVEAU] Notifier le serveur que l'on quitte l'appel
        // ClientHandlerAuth.getInstance().quitterAppelGroupe();

        participantsConnectes.clear();
        stage.close();
    }

    // [NOUVEAU] Méthode pour fermer la fenêtre depuis l'extérieur (quand l'appel est terminé par le serveur)
    public void fermerFenetre() {
        if (stage != null) {
            Platform.runLater(() -> stage.close());
        }
    }

    // Helper pour Platform.runLater sans importer javafx.application.Platform partout
    private static void runLater(Runnable r) { javafx.application.Platform.runLater(r); }
}