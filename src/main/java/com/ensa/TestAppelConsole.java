package com.ensa;

import client.ClientHandlerAuth;
import client.EcouteurClient;
import model.*;

import java.util.List;
import java.util.Scanner;

public class TestAppelConsole implements EcouteurClient {

    private ClientHandlerAuth facade;
    private Scanner scanner = new Scanner(System.in);
    private boolean running = true;
    private String monNumero = null;

    public static void main(String[] args) {
        new TestAppelConsole().demarrer();
    }

    public void demarrer() {
        facade = ClientHandlerAuth.getInstance();

        System.out.println("========================================");
        System.out.println("  🧪 TEST APPEL - CLIENT CONSOLE");
        System.out.println("========================================\n");

        // Connexion au serveur
        System.out.print("IP serveur (localhost) : ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "localhost";

        System.out.print("Port (8080) : ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? 8080 : Integer.parseInt(portStr);

        System.out.println("\n--- Connexion au serveur ---");
        boolean connected = facade.connecterAuServeur(ip, port, this);

        if (!connected) {
            System.out.println("❌ Impossible de se connecter au serveur " + ip + ":" + port);
            return;
        }

        System.out.println("✅ Connecté au serveur !\n");

        // Authentification
        System.out.print("Numéro téléphone : ");
        String numero = scanner.nextLine().trim();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine().trim();

        System.out.println("→ Authentification...");
        facade.seConnecter(numero, password);
        attendre(1000);

        // Menu principal
        while (running) {
            afficherMenu();
            String choix = scanner.nextLine().trim();
            traiterChoix(choix);
        }

        System.out.println("👋 Au revoir !");
    }

    private void afficherMenu() {
        boolean enAppel = facade.isEnAppel();

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║         📱 MENU PRINCIPAL            ║");
        System.out.println("╠══════════════════════════════════════╣");

        if (!enAppel) {
            System.out.println("║  1. 📞 Appeler (Audio)               ║");
            System.out.println("║  2. 📹 Appeler (Vidéo)               ║");
            System.out.println("║  3. 💬 Voir conversations            ║");
            System.out.println("║  4. 📨 Envoyer message               ║");
        } else {
            System.out.println("║  📞 APPEL EN COURS...                ║");
            System.out.println("║  5. ✅ Accepter l'appel              ║");
            System.out.println("║  6. ❌ Refuser l'appel               ║");
            System.out.println("║  7. 📞 Raccrocher                    ║");
        }

        System.out.println("║  8. 👥 Liste utilisateurs            ║");
        System.out.println("║  9. 🚪 Se déconnecter                ║");
        System.out.println("║  0. ❌ Quitter                       ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("👉 Ton choix : ");
    }

    private void traiterChoix(String choix) {
        switch (choix) {
            case "1": appeler("audio"); break;
            case "2": appeler("video"); break;
            case "3": voirConversations(); break;
            case "4": envoyerMessage(); break;
            case "5": accepterAppel(); break;
            case "6": refuserAppel(); break;
            case "7": raccrocher(); break;
            case "8": listeUtilisateurs(); break;
            case "9": seDeconnecter(); break;
            case "0": running = false; break;
            default: System.out.println("❌ Choix invalide");
        }
    }

    // ========== ACTIONS ==========

    private void appeler(String type) {
        System.out.print("Numéro du destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("ID conversation (1) : ");
        String idStr = scanner.nextLine().trim();
        int idConv = idStr.isEmpty() ? 1 : Integer.parseInt(idStr);

        System.out.println("→ Appel " + type + " vers " + dest + "...");
        facade.appeler(dest, idConv, type);
    }

    private void accepterAppel() {
        System.out.println("→ Acceptation de l'appel...");
        facade.accepterAppel();
    }

    private void refuserAppel() {
        System.out.println("→ Refus de l'appel...");
        facade.refuserAppel();
    }

    private void raccrocher() {
        System.out.println("→ Raccrochage...");
        facade.raccrocher();
    }

    private void voirConversations() {
        System.out.println("→ Demande des conversations...");
        facade.demanderConversations();
        attendre(500);
    }

    private void envoyerMessage() {
        System.out.print("Destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("Message : ");
        String msg = scanner.nextLine().trim();

        facade.envoyerMessage(dest, msg);
    }

    private void listeUtilisateurs() {
        System.out.println("→ Demande liste utilisateurs...");
        facade.demanderListeUtilisateurs();
        attendre(500);
    }

    private void seDeconnecter() {
        System.out.println("→ Déconnexion...");
        facade.seDeconnecter();
        monNumero = null;
    }

    // ========== ÉCOUTEUR CLIENT ==========

    @Override
    public void connexionReussie(Utilisateur moi) {
        System.out.println("\n✅ CONNEXION RÉUSSIE !");
        System.out.println("   Nom : " + moi.getNomComplet());
        System.out.println("   Numéro : " + moi.getNumeroTelephone());
        this.monNumero = moi.getNumeroTelephone();

        // Initialiser le CallService
        facade.onConnexionReussie(moi);
    }

    @Override
    public void appelEntrant(String numAppelant, String nomAppelant, String type, String ip) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      📞 APPEL ENTRANT !              ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  De : " + padRight(nomAppelant + " (" + numAppelant + ")", 28) + "║");
        System.out.println("║  Type : " + padRight(type.toUpperCase(), 26) + "║");
        System.out.println("║                                      ║");
        System.out.println("║  Tape 5 pour ACCEPTER                ║");
        System.out.println("║  Tape 6 pour REFUSER                 ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    @Override
    public void appelAccepte(String ipAccepteur) {
        System.out.println("\n✅ APPEL ACCEPTÉ !");
        System.out.println("   IP du correspondant : " + ipAccepteur);
        System.out.println("   Communication active 🎤");
    }

    @Override
    public void appelRefuse() {
        System.out.println("\n❌ APPEL REFUSÉ");
    }

    @Override
    public void appelTermine(String telephone) {
        System.out.println("\n📞 APPEL TERMINÉ par " + telephone);
    }

    @Override
    public void messageRecu(String expediteur, String contenu) {
        System.out.println("\n💬 Message de " + expediteur + " : " + contenu);
    }

    @Override
    public void conversationsRecues(List<Conversation> conversations) {
        System.out.println("\n📨 CONVERSATIONS (" + conversations.size() + ")");
        if (conversations.isEmpty()) {
            System.out.println("   (Aucune conversation)");
            return;
        }

        System.out.println("┌────┬─────────────┬────────────────┬────────┐");
        System.out.println("│ ID │   Contact   │  Dernier Msg   │ Non Lus│");
        System.out.println("├────┼─────────────┼────────────────┼────────┤");

        for (Conversation c : conversations) {
            String msg = c.getApercu();
            if (msg.length() > 14) msg = msg.substring(0, 14) + "...";
            System.out.printf("│ %2d │ %-11s │ %-14s │ %6d │%n",
                    c.getIdConversation(), c.getNomContact(), msg, c.getMessagesNonLus());
        }
        System.out.println("└────┴─────────────┴────────────────┴────────┘");
    }

    @Override
    public void messagesRecus(List<Message> messages) {

    }

    @Override
    public void contactAjoute(Contact contact) {

    }

    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }

    @Override
    public void erreur(String message) {
        System.out.println("\n❌ ERREUR : " + message);
    }

    @Override
    public void deconnexion() {
        System.out.println("\n🔌 DÉCONNECTÉ DU SERVEUR");
        monNumero = null;
    }

    @Override
    public void inscriptionReussie(String nom) {
        System.out.println("\n✅ INSCRIPTION RÉUSSIE : " + nom);
    }

    // ========== UTILITAIRES ==========

    private String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }

    private void attendre(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}
