/*package com.ensa;
import client.ClientReseau;
import client.EcouteurClient;
import model.Contact;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import model.enums.TypeAppel;
import service.CallService;

import java.util.List;

public class TestCallMain {

    public static void main(String[] args) throws Exception {

        final ClientReseau[] client2Ref = new ClientReseau[1];

        // ========= CLIENT 1 =========
        ClientReseau client1 = new ClientReseau(new EcouteurClient() {
            @Override
            public void connexionReussie(Utilisateur moi) {
                System.out.println("[CLIENT1] connecté : " + moi.getNumeroTelephone());
            }

            @Override public void inscriptionReussie(String msg) {}
            @Override public void erreur(String message) { System.out.println("[CLIENT1] erreur: " + message); }
            @Override public void messageRecu(String numeroDest, String message) {}
            @Override public void conversationsRecues(List<Conversation> conversations) {}
            @Override public void messagesRecus(List<Message> messages) {}
            @Override public void contactAjoute(Contact contact) {}
            @Override public void listeContactsRecue(List<Contact> contacts) {}
            @Override public void deconnexion() { System.out.println("[CLIENT1] déconnecté"); }

            @Override
            public void appelEntrant(String numero, String type, String ipAppelant, String ip) {}

            @Override
            public void appelAccepte(String numero, String ip) {
                System.out.println("[CLIENT1] appel accepté par " + numero + " ip=" + ip);
            }

            @Override
            public void appelRefuse() {
                System.out.println("[CLIENT1] appel refusé");
            }

            @Override
            public void appelTermine(String numero) {
                System.out.println("[CLIENT1] appel terminé avec " + numero);
            }
        });

        client1.connecterAuServeur("127.0.0.1", 9090);

        // LOGIN
        client1.envoyer(new network.Packet(Serveur.Protocol.LOGIN, "1111|pass"));

        // créer CallService pour client1
        Utilisateur user1 = new Utilisateur();
        user1.setNumeroTelephone("1111");
        user1.setIdUtilisateur(1);

        CallService callService1 = new CallService(client1, user1);
        client1.setCallService(callService1);

        // ========= CLIENT 2 =========
        ClientReseau client2 = new ClientReseau(new EcouteurClient() {

            @Override
            public void connexionReussie(Utilisateur moi) {
                System.out.println("[CLIENT2] connecté : " + moi.getNumeroTelephone());
            }

            @Override public void inscriptionReussie(String msg) {}
            @Override public void erreur(String message) { System.out.println("[CLIENT2] erreur: " + message); }
            @Override public void messageRecu(String numeroDest, String message) {}
            @Override public void conversationsRecues(List<Conversation> conversations) {}
            @Override public void messagesRecus(List<Message> messages) {}
            @Override public void contactAjoute(Contact contact) {}
            @Override public void listeContactsRecue(List<Contact> contacts) {}
            @Override public void deconnexion() { System.out.println("[CLIENT2] déconnecté"); }

            @Override
            public void appelEntrant(String numero, String type, String ipAppelant, String ip) {
                System.out.println("[CLIENT2] appel entrant de " + numero + " type=" + type + " ip=" + ipAppelant);

                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        System.out.println("[CLIENT2] j'accepte l'appel...");
                        client2Ref[0].getCallService().accepte();
                    } catch (Exception ignored) {}
                }).start();
            }

            @Override
            public void appelAccepte(String numero, String ip) {}

            @Override
            public void appelRefuse() {
                System.out.println("[CLIENT2] appel refusé");
            }

            @Override
            public void appelTermine(String numero) {
                System.out.println("[CLIENT2] appel terminé avec " + numero);
            }
        });

        client2Ref[0] = client2;

        client2.connecterAuServeur("127.0.0.1", 9090);

        // LOGIN
        client2.envoyer(new network.Packet(Serveur.Protocol.LOGIN, "2222|pass"));

        // créer CallService pour client2
        Utilisateur user2 = new Utilisateur();
        user2.setNumeroTelephone("2222");
        user2.setIdUtilisateur(2);

        CallService callService2 = new CallService(client2, user2);
        client2.setCallService(callService2);

        // ========= TEST =========
        Thread.sleep(2000);

        System.out.println("\n📞 [TEST] Client1 appelle Client2...\n");
        callService1.appeler("2222", 1, TypeAppel.AUDIO);

        while (true) {
            Thread.sleep(1000);
        }
    }
}*/