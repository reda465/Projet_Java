/*package javafx;

import Serveur.Protocol;
import client.ClientReseau;
import client.EcouteurClient;
import model.Contact;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import network.Packet;
import service.FileService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class FichierUI extends Application implements EcouteurClient {

    private ClientReseau clientReseau;
    private FileService fileService;

    private TextArea logs;
    private TextField txtTelephone;
    private File fichierSelectionne;

    private String monTelephone;

    @Override
    public void start(Stage stage) {

        logs = new TextArea();
        logs.setEditable(false);
        logs.setPrefHeight(300);

        txtTelephone = new TextField();
        txtTelephone.setPromptText("Téléphone destinataire (ex: 2222)");

        Button btnChoisir = new Button("📂 Choisir fichier");
        Button btnEnvoyer = new Button("📤 Envoyer fichier");

        btnChoisir.setOnAction(e -> choisirFichier(stage));

        btnEnvoyer.setOnAction(e -> envoyerFichier());

        VBox root = new VBox(10,
                new Label("📎 Test Transfert Fichier"),
                new Label("Destinataire :"),
                txtTelephone,
                btnChoisir,
                btnEnvoyer,
                new Label("Logs :"),
                logs
        );

        root.setPadding(new Insets(15));

        stage.setTitle("Test Fichier JavaFX");
        stage.setScene(new Scene(root, 500, 450));
        stage.show();

        // ===== CONNEXION AU MOCK SERVER =====
        clientReseau = new ClientReseau(this);
        clientReseau.connecterAuServeur("127.0.0.1", 9091);

        fileService = new FileService(clientReseau);

        // ===== LOGIN AUTOMATIQUE =====
        monTelephone = "1111";
        clientReseau.envoyer(new Packet(Protocol.LOGIN, monTelephone + "|pass"));
        log("Connexion en cours...");
    }

    private void choisirFichier(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");

        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            fichierSelectionne = file;
            log("📂 Fichier sélectionné : " + file.getAbsolutePath());
        } else {
            log("⚠️ Aucun fichier sélectionné");
        }
    }

    private void envoyerFichier() {
        if (fichierSelectionne == null) {
            log("❌ Choisir un fichier d'abord !");
            return;
        }

        String telDest = txtTelephone.getText().trim();
        if (telDest.isEmpty()) {
            log("❌ Entrer le numéro destinataire !");
            return;
        }

        log("📤 Envoi fichier vers " + telDest + " ...");
        fileService.envoyerFichier(telDest, fichierSelectionne);
    }

    private void log(String msg) {
        Platform.runLater(() -> logs.appendText(msg + "\n"));
    }

    // ================== EcouteurClient ==================

    @Override
    public void connexionReussie(Utilisateur moi) {
        log("✅ Connecté : " + moi.getNumeroTelephone());
    }

    @Override
    public void inscriptionReussie(String msg) {}

    @Override
    public void erreur(String message) {
        log("❌ ERREUR : " + message);
    }

    @Override
    public void messageRecu(String numeroDest, String message) {}

    @Override
    public void conversationsRecues(List<Conversation> conversations) {}

    @Override
    public void messagesRecus(List<Message> messages) {}

    @Override
    public void contactAjoute(Contact contact) {}

    @Override
    public void listeContactsRecue(List<Contact> contacts) {}

    @Override
    public void deconnexion() {
        log("⚠️ Déconnecté du serveur");
    }

    @Override
    public void appelEntrant(String numero, String type, String ipAppelant, String ip) {}

    @Override
    public void appelAccepte(String numero, String ip) {}

    @Override
    public void appelRefuse() {}

    @Override
    public void appelTermine(String numero) {}

    // ========== IMPORTANT : réception fichier ==========
    @Override
    public void fichierRecu(String telephoneExp, String fileName, String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);

            File dossier = new File("downloads");
            if (!dossier.exists()) dossier.mkdirs();

            File outFile = new File(dossier, "RECU_" + fileName);

            Files.write(outFile.toPath(), data);

            log("📥 Fichier reçu de " + telephoneExp + " => " + outFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            log("❌ Erreur sauvegarde fichier");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}*/