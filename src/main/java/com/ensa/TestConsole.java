package com.ensa;

import client.ClientHandlerAuth;
import client.EcouteurClient;
import model.Contact;
import model.Conversation;
import model.Message;
import model.Utilisateur;

import java.util.List;
import java.util.Scanner;

public class TestConsole implements EcouteurClient {

    private ClientHandlerAuth facade;
    private boolean running = true;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new TestConsole().demarrer();
    }

    public void demarrer() {
        facade = ClientHandlerAuth.getInstance();

        System.out.println("========================================");
        System.out.println("  TEST CONSOLE - WhatsApp Clone");
        System.out.println("========================================\n");



        System.out.println("\n--- Connexion au serveur ---");
        boolean connected = facade.connecterAuServeur("10.100.106.228", 5000, this);

        if (!connected) {
           // System.out.println("❌ Impossible de se connecter au serveur " + ip + ":" + port);
            return;
        }

        System.out.println("✅ Connecté au serveur !");
        attendre(300);

        // Menu principal
        while (running) {
            afficherMenu();
            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1": testerConnexion(); break;
                case "2": testerInscription(); break;
                case "3": testerConversations(); break;
                case "4": testerEnvoiMessage(); break;
                case "5": testerListeUtilisateurs(); break;
                case "6": testerDeconnexion(); break;
                case "0": running = false; System.out.println("Au revoir !"); break;
                default: System.out.println("❌ Choix invalide");
            }
        }
        scanner.close();
    }

    private void afficherMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║           MENU PRINCIPAL             ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Se connecter                     ║");
        System.out.println("║  2. S'inscrire                       ║");
        System.out.println("║  3. Voir mes conversations           ║");
        System.out.println("║  4. Envoyer un message               ║");
        System.out.println("║  5. Liste des utilisateurs           ║");
        System.out.println("║  6. Se déconnecter                   ║");
        System.out.println("║  0. Quitter                          ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Ton choix : ");
    }

    private void testerConnexion() {
        System.out.println("\n--- CONNEXION ---");
        System.out.print("Numéro de téléphone : ");
        String numero = scanner.nextLine().trim();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine().trim();

        System.out.println("→ Envoi CONNEXION : " + numero);
        String resultat = facade.seConnecter(numero, password);
        System.out.println("   Résultat : " + resultat);
        attendre(1000);
    }

    private void testerInscription() {
        System.out.println("\n--- INSCRIPTION ---");
        System.out.print("Nom complet : ");
        String nom = scanner.nextLine().trim();
        System.out.print("Numéro : ");
        String numero = scanner.nextLine().trim();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine().trim();

        System.out.println("→ Envoi INSCRIPTION : " + numero);
        String resultat = facade.sInscrire(nom, numero, password);
        System.out.println("   Résultat : " + resultat);
        attendre(1000);
    }

    private void testerConversations() {
        System.out.println("\n--- DEMANDE CONVERSATIONS ---");
        System.out.println("→ Envoi LISTE_CONVERSATIONS");
        facade.demanderConversations();
        attendre(1500);
    }

    private void testerEnvoiMessage() {
        System.out.println("\n--- ENVOI MESSAGE ---");
        System.out.println("Numéro du destinataire : ");
        String destinataire = scanner.nextLine().trim();
        System.out.println("Message : ");
        String contenu = scanner.nextLine().trim();

        System.out.println("→ Envoi MSG à " + destinataire);
        facade.envoyerMessage(destinataire, contenu);
        attendre(500);
    }

    private void testerListeUtilisateurs() {
        System.out.println("\n--- LISTE UTILISATEURS ---");
        System.out.println("→ Envoi USERS_LIST");
        facade.demanderListeUtilisateurs();
        attendre(1000);
    }

    private void testerDeconnexion() {
        System.out.println("\n--- DÉCONNEXION ---");
        facade.seDeconnecter();
        attendre(500);
    }

    // ============ ÉCOUTEUR ============

    @Override
    public void connexionReussie(Utilisateur moi) {
        System.out.println("\n✅ CONNEXION RÉUSSIE !");
        System.out.println("   Nom : " + moi.getNomComplet());
        System.out.println("   Numéro : " + moi.getNumeroTelephone());
    }

    @Override
    public void inscriptionReussie(String msg) {

    }

    @Override
    public void conversationsRecues(List<Conversation> conversations) {
        System.out.println("\n📨 CONVERSATIONS REÇUES (" + conversations.size() + ")");

        if (conversations.isEmpty()) {
            System.out.println("   (Aucune conversation)");
            return;
        }

        for (Conversation c : conversations) {
            String nonLus = c.getMessagesNonLus() > 0 ? " [" + c.getMessagesNonLus() + " non lus]" : "";
            System.out.println("   📱 " + c.getNomContact() + " (" + c.getNumeroContact() + ")");
            System.out.println("      └─ " + c.getApercu() + nonLus);
        }
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
    public void messageRecu(String numeroDest, String message) {
        System.out.println("\n📩 NOUVEAU MESSAGE de " + numeroDest);
        System.out.println("   " + message);
    }

    @Override
    public void deconnexion() {
        System.out.println("\n🔌 DÉCONNECTÉ DU SERVEUR");
    }

    @Override
    public void appelEntrant(String numero, String type, String ipAppelant , String ip) {
            System.out.println("\n📞 APPEL ENTRANT de " + numero);
            System.out.println("   Type : " + type);
            System.out.println("   IP : " + ipAppelant);
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