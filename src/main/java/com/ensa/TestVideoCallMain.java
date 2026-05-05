/*package com.ensa;

import client.ClientReseau;
import client.EcouteurClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Contact;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import model.enums.TypeAppel;
import network.Packet;
import service.CallService;

import java.util.List;

public class TestVideoCallMain extends Application {

    private ClientReseau client1;
    private ClientReseau client2;

    private CallService callService1;
    private CallService callService2;

    @Override
    public void start(Stage stage) throws Exception {

        ImageView view1 = new ImageView();
        view1.setFitWidth(320);
        view1.setFitHeight(240);

        ImageView view2 = new ImageView();
        view2.setFitWidth(320);
        view2.setFitHeight(240);

        HBox root = new HBox(20, view1, view2);
        stage.setScene(new Scene(root, 700, 300));
        stage.setTitle("TEST VIDEO UDP - 2 Clients sur même PC");
        stage.show();

        // ========== CLIENT 1 ==========
        client1 = new ClientReseau(new EcouteurClient() {
            @Override public void connexionReussie(Utilisateur moi) {
                System.out.println("[CLIENT1] connecté : " + moi.getNumeroTelephone());
            }

            @Override public void inscriptionReussie(String msg) {}
            @Override public void erreur(String message) { System.out.println("[CLIENT1] erreur: " + message); }
            @Override public void messageRecu(String numeroDest, String message) {}
            @Override public void conversationsRecues(List<Conversation> conversations) {}
            @Override public void messagesRecus(List<Message> messages) {}
            @Override public void contactAjoute(Contact contact) {}
            @Override public void listeContactsRecue(List<Contact> contacts) {}
            @Override public void deconnexion() {}

            @Override public void appelEntrant(String numero, String type, String ipAppelant, String ip) {}

            @Override
            public void appelAccepte(String numero, String ip) {
                System.out.println("[CLIENT1] appel accepté par " + numero + " ip=" + ip);
            }

            @Override public void appelRefuse() {}
            @Override public void appelTermine(String numero) {}
        });

        client1.connecterAuServeur("127.0.0.1", 9090);

        Utilisateur user1 = new Utilisateur();
        user1.setNumeroTelephone("1111");
        user1.setIdUtilisateur(1);

        callService1 = new CallService(client1, user1);
        callService1.setVideoView(view1);
        client1.setCallService(callService1);

        client1.envoyer(new Packet(Serveur.Protocol.LOGIN, "1111|pass"));

        // ========== CLIENT 2 ==========
        client2 = new ClientReseau(new EcouteurClient() {

            @Override public void connexionReussie(Utilisateur moi) {
                System.out.println("[CLIENT2] connecté : " + moi.getNumeroTelephone());
            }

            @Override public void inscriptionReussie(String msg) {}
            @Override public void erreur(String message) { System.out.println("[CLIENT2] erreur: " + message); }
            @Override public void messageRecu(String numeroDest, String message) {}
            @Override public void conversationsRecues(List<Conversation> conversations) {}
            @Override public void messagesRecus(List<Message> messages) {}
            @Override public void contactAjoute(Contact contact) {}
            @Override public void listeContactsRecue(List<Contact> contacts) {}
            @Override public void deconnexion() {}

            @Override
            public void appelEntrant(String numero, String type, String ipAppelant, String ip) {
                System.out.println("[CLIENT2] appel entrant de " + numero + " type=" + type);

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        System.out.println("[CLIENT2] j'accepte...");
                        callService2.accepte();
                    } catch (Exception ignored) {}
                }).start();
            }

            @Override public void appelAccepte(String numero, String ip) {}
            @Override public void appelRefuse() {}
            @Override public void appelTermine(String numero) {}
        });

        client2.connecterAuServeur("127.0.0.1", 9090);

        Utilisateur user2 = new Utilisateur();
        user2.setNumeroTelephone("2222");
        user2.setIdUtilisateur(2);

        callService2 = new CallService(client2, user2);
        callService2.setVideoView(view2);
        client2.setCallService(callService2);

        client2.envoyer(new Packet(Serveur.Protocol.LOGIN, "2222|pass"));

        // ========== Lancer appel vidéo ==========
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("\n🎥 [TEST] Client1 appelle Client2 en VIDEO...\n");
                callService1.appeler("2222", 1, TypeAppel.video);
            } catch (Exception ignored) {}
        }).start();

        stage.setOnCloseRequest(e -> {
            if (callService1 != null) callService1.raccrocher();
            if (callService2 != null) callService2.raccrocher();
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}*/
