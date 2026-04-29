package com.ensa;

import client.ClientHandlerAuth;
import client.EcouteurClient;
import model.*;

import java.util.List;
import java.util.Scanner;

public class AppelTest implements EcouteurClient {

    private ClientHandlerAuth facade;
    private Scanner scanner = new Scanner(System.in);
    private boolean running = true;
    private boolean appelEntrant = false;

    public static void main(String[] args) {
        new AppelTest().demarrer();
    }

    public void demarrer() {
        facade = ClientHandlerAuth.getInstance();

        System.out.println("========================================");
        System.out.println("  📱 TEST COMPLET - VRAI SERVEUR");
        System.out.println("========================================\n");

        // Connexion
        System.out.print("🌐 IP serveur (localhost) : ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) ip = "localhost";

        System.out.print("📡 Port (8080) : ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? 8080 : Integer.parseInt(portStr);

        System.out.println("\n--- Connexion ---");
        boolean connected = facade.connecterAuServeur(ip, port, this);
        if (!connected) {
            System.out.println("❌ Impossible de se connecter");
            return;
        }
        System.out.println("✅ Connecté au serveur !\n");

        // Login
        System.out.print("📱 Numéro : ");
        String numero = scanner.nextLine().trim();
        System.out.print("🔑 Password : ");
        String password = scanner.nextLine().trim();

        facade.seConnecter(numero, password);
        attendre(1500);

        // Menu
        while (running) {
            afficherMenu();
            String choix = scanner.nextLine().trim();
            traiterChoix(choix);
        }
    }

    private void afficherMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║           📋 MENU PRINCIPAL          ║");
        System.out.println("╠══════════════════════════════════════╣");

        if (appelEntrant) {
            System.out.println("║  🚨 APPEL ENTRANT !                  ║");
            System.out.println("║  5. ✅ Accepter                      ║");
            System.out.println("║  6. ❌ Refuser                       ║");
        } else if (facade.isEnAppel()) {
            System.out.println("║  📞 APPEL EN COURS                   ║");
            System.out.println("║  7. 📞 Raccrocher                    ║");
        } else {
            System.out.println("║  1. 📞 Appeler (Audio)               ║");
            System.out.println("║  2. 📹 Appeler (Vidéo)               ║");
            System.out.println("║  3. 💬 Conversations                 ║");
            System.out.println("║  4. ✉️  Envoyer message              ║");
        }

        System.out.println("║  8. 👥 Utilisateurs connectés        ║");
        System.out.println("║  9. 🚪 Déconnexion                   ║");
        System.out.println("║  0. ❌ Quitter                       ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("👉 Choix : ");
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
        System.out.print("📱 Numéro destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("🆔 ID conversation (1) : ");
        String idStr = scanner.nextLine().trim();
        int idConv = idStr.isEmpty() ? 1 : Integer.parseInt(idStr);

        System.out.println("→ Appel " + type + " vers " + dest + "...");
        facade.appeler(dest, idConv, type);
    }

    private void accepterAppel() {
        System.out.println("→ Acceptation...");
        facade.accepterAppel();
        appelEntrant = false;
    }

    private void refuserAppel() {
        System.out.println("→ Refus...");
        facade.refuserAppel();
        appelEntrant = false;
    }

    private void raccrocher() {
        System.out.println("→ Raccrochage...");
        facade.raccrocher();
    }

    private void voirConversations() {
        System.out.println("→ Demande conversations...");
        facade.demanderConversations();
        attendre(500);
    }

    private void envoyerMessage() {
        System.out.print("📱 Destinataire : ");
        String dest = scanner.nextLine().trim();
        System.out.print("✉️  Message : ");
        String msg = scanner.nextLine().trim();

        facade.envoyerMessage(dest, msg);
    }

    private void listeUtilisateurs() {
        System.out.println("→ Demande liste...");
        facade.demanderListeUtilisateurs();
        attendre(500);
    }

    private void seDeconnecter() {
        facade.seDeconnecter();
        System.out.println("👋 Déconnecté");
    }

    // ========== ÉCOUTEUR ==========

    @Override
    public void connexionReussie(Utilisateur moi) {
        System.out.println("\n✅ Connecté : " + moi.getNomComplet());
        facade.onConnexionReussie(moi); // Initialiser CallService
    }

    @Override
    public void appelEntrant(String num, String nom, String type, String ip) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      📞 APPEL ENTRANT !              ║");
        System.out.println("║  De : " + pad(nom + " (" + num + ")", 28) + "║");
        System.out.println("║  Type : " + pad(type, 26) + "║");
        System.out.println("║  Tape 5 pour ACCEPTER                ║");
        System.out.println("║  Tape 6 pour REFUSER                 ║");
        System.out.println("╚══════════════════════════════════════╝");
        appelEntrant = true;
    }

    @Override
    public void appelAccepte(String ip) {
        System.out.println("\n✅ APPEL ACCEPTÉ ! IP : " + ip);
    }

    @Override
    public void appelRefuse() {
        System.out.println("\n❌ APPEL REFUSÉ");
    }

    @Override
    public void appelTermine(String tel) {
        System.out.println("\n📞 APPEL TERMINÉ par " + tel);
        appelEntrant = false;
    }

    @Override
    public void messageRecu(String exp, String contenu) {
        System.out.println("\n💬 " + exp + " : " + contenu);
    }

    @Override
    public void conversationsRecues(List<Conversation> convs) {
        System.out.println("\n📨 " + convs.size() + " conversations");
        for (Conversation c : convs) {
            String nonLus = c.getMessagesNonLus() > 0 ? " [" + c.getMessagesNonLus() + "✉️]" : "";
            System.out.println("   " + c.getNomContact() + " : " + c.getApercu() + nonLus);
        }
    }

    @Override
    public void contactAjoute(Contact contact) {

    }

    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }

    @Override
    public void erreur(String msg) {
        System.out.println("\n❌ " + msg);
    }

    @Override
    public void deconnexion() {
        System.out.println("\n🔌 Déconnecté");
    }

    @Override
    public void inscriptionReussie(String nom) {}

    private String pad(String s, int n) {
        return String.format("%-" + n + "s", s.length() > n ? s.substring(0, n) : s);
    }

    private void attendre(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}