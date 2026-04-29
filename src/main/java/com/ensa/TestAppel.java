package com.ensa;

import client.ClientHandlerAuth;
import client.EcouteurClient;
import model.*;

import java.util.List;
import java.util.Scanner;

public class TestAppel implements EcouteurClient {

    private ClientHandlerAuth facade;
    private Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    public static void main(String[] args) {
        new TestAppel().demarrer();
    }

    public void demarrer() {
        facade = ClientHandlerAuth.getInstance();

        System.out.println("========================================");
        System.out.println("  TEST APPEL AUDIO/VIDÉO");
        System.out.println("========================================\n");

        // Connexion
        System.out.print("IP serveur (localhost) : ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "localhost";

        System.out.print("Port (8080) : ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? 8080 : Integer.parseInt(portStr);

        System.out.println("\n--- Connexion ---");
        boolean connected = facade.connecterAuServeur(ip, port, this);
        if (!connected) {
            System.out.println("❌ Impossible de se connecter");
            return;
        }

        // Authentification
        System.out.print("\nNuméro téléphone : ");
        String numero = scanner.nextLine().trim();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine().trim();

        System.out.println("→ Connexion...");
        facade.seConnecter(numero, password);
        attendre(1000);

        // Menu
        while (running) {
            afficherMenu();
            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1": testerAppelAudio(); break;
                case "2": testerAppelVideo(); break;
                case "3": accepterAppel(); break;
                case "4": refuserAppel(); break;
                case "5": raccrocher(); break;
                case "6": testerMessage(); break;
                case "0": running = false; break;
                default: System.out.println("❌ Choix invalide");
            }
        }
        System.out.println("Au revoir !");
    }

    private void afficherMenu() {
        boolean enAppel = facade.isEnAppel();

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║           MENU APPEL                 ║");
        System.out.println("╠══════════════════════════════════════╣");

        if (!enAppel) {
            System.out.println("║  1. 📞 Appeler (Audio)               ║");
            System.out.println("║  2. 📹 Appeler (Vidéo)               ║");
        } else {
            System.out.println("║  3. ✅ Accepter l'appel              ║");
            System.out.println("║  4. ❌ Refuser l'appel               ║");
            System.out.println("║  5. 📞 Raccrocher                    ║");
        }

        System.out.println("║  6. 💬 Envoyer message               ║");
        System.out.println("║  0. Quitter                          ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Ton choix : ");
    }

    private void testerAppelAudio() {
        System.out.print("Numéro du destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("ID conversation : ");
        int idConv = Integer.parseInt(scanner.nextLine().trim());

        facade.appeler(dest, idConv, "audio");
    }

    private void testerAppelVideo() {
        System.out.print("Numéro du destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("ID conversation : ");
        int idConv = Integer.parseInt(scanner.nextLine().trim());

        facade.appeler(dest, idConv, "video");
    }

    private void accepterAppel() {
        facade.accepterAppel();
    }

    private void refuserAppel() {
        facade.refuserAppel();
    }

    private void raccrocher() {
        facade.raccrocher();
    }

    private void testerMessage() {
        System.out.print("Destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("Message : ");
        String msg = scanner.nextLine().trim();
        facade.envoyerMessage(dest, msg);
    }

    // ========== ÉCOUTEUR ==========

    @Override
    public void connexionReussie(Utilisateur moi) {
        System.out.println("\n✅ Connecté : " + moi.getNomComplet());
        facade.onConnexionReussie(moi);
    }

    @Override
    public void appelEntrant(String numAppelant, String nomAppelant, String type, String ip) {
        System.out.println("\n📞 APPEL ENTRANT !");
        System.out.println("   De : " + nomAppelant + " (" + numAppelant + ")");
        System.out.println("   Type : " + type);
        System.out.println("   Tape '3' pour accepter, '4' pour refuser");
    }

    @Override
    public void appelAccepte(String ipAccepteur) {
        System.out.println("✅ Appel accepté ! IP : " + ipAccepteur);
    }

    @Override
    public void appelRefuse() {
        System.out.println("❌ Appel refusé");
    }

    @Override
    public void appelTermine(String telephone) {
        System.out.println("📞 Appel terminé par " + telephone);
    }

    @Override
    public void messageRecu(String expediteur, String contenu) {
        System.out.println("\n💬 Message de " + expediteur + " : " + contenu);
    }

    @Override
    public void conversationsRecues(List<Conversation> conversations) {
        System.out.println("📨 Conversations : " + conversations.size());
    }

    @Override
    public void contactAjoute(Contact contact) {

    }

    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }

    @Override
    public void erreur(String message) {
        System.out.println("❌ Erreur : " + message);
    }

    @Override
    public void deconnexion() {
        System.out.println("🔌 Déconnecté");
    }


    @Override
    public void inscriptionReussie(String nom) {
        System.out.println("✅ Inscrit : " + nom);
    }

    private void attendre(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}