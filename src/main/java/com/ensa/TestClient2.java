package com.ensa;

import client.ClientHandlerAuth;
import client.EcouteurClient;
import model.*;

import java.util.List;

public class TestClient2 implements EcouteurClient {

    public static void main(String[] args) {
        // Attendre un peu que le client 1 se connecte
        try { Thread.sleep(500); } catch (Exception e) {}
        new TestClient2().demarrer();
    }

    public void demarrer() {
        ClientHandlerAuth facade = ClientHandlerAuth.getInstance();

        System.out.println("👤 CLIENT 2 - Ali");
        System.out.println("=======================");

        boolean connected = facade.connecterAuServeur("localhost", (5000), this);
        if (!connected) {
            System.out.println("❌ Connexion échouée");
            return;
        }

        System.out.println("→ Connexion avec 0611111111");
        facade.seConnecter("0611111111", "pass123");

        attendre(100);
        System.out.println("\n✅ Client 2 prêt !");

        java.util.Scanner sc = new java.util.Scanner(System.in);
        while (true) {
            String cmd = sc.nextLine();
            switch (cmd) {
                case "3": facade.demanderConversations(); break;
                case "4":
                    System.out.print("Destinataire: ");
                    String dest = sc.nextLine();
                    System.out.print("Message: ");
                    String msg = sc.nextLine();
                    facade.envoyerMessage(dest, msg);
                    break;
                case "0": return;
            }
        }
    }

    @Override public void connexionReussie(Utilisateur moi) {
        System.out.println("✅ Connecté : " + moi.getNomComplet());
    }

    @Override
    public void inscriptionReussie(String msg) {

    }

    @Override public void conversationsRecues(List<Conversation> convs) {
        System.out.println("📨 " + convs.size() + " conversations");
        for (Conversation c : convs) {
            System.out.println("   - " + c.getNomContact() + ": " + c.getApercu());
        }
    }

    @Override
    public void contactAjoute(Contact contact) {

    }

    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }

    @Override public void messageRecu(String num, String msg) {
        System.out.println("📩 Message de " + num+ ": " + msg);
    }
    @Override public void erreur(String msg) { System.out.println("❌ " + msg); }


    @Override public void deconnexion() { System.out.println("🔌 Déconnecté"); }

    @Override
    public void appelEntrant(String numero, String type, String ipAppelant, String ip) {
        System.out.println("📞 Appel entrant de " + numero + " (" + type + ")");
        System.out.println("   Tape '3' pour accepter, '4' pour refuser");
    }


    @Override
    public void appelAccepte(String numero) {

    }

    @Override
    public void appelRefuse() {

    }


    @Override
    public void appelTermine(String numero) {

    }

    private void attendre(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}