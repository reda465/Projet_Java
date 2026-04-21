import client.*;
import model.Utilisateur;
import service.AuthService;
import java.util.Scanner;

public class TestConnexion implements EcouteurClient {

    private ClientReseau client;
    private AuthService auth;
    private Scanner clavier;
    private boolean authentifie = false;
    private boolean serveurConnecte = false;

    public static void main(String[] args) {
        new TestConnexion().demarrer();
    }

    public void demarrer() {
        clavier = new Scanner(System.in);

        // Boucle : tant que pas connecté, on redemande
        while (!serveurConnecte) {
            if (!tenterConnexion()) {
                System.out.println("\n❌ Échec de connexion.");
                System.out.println("1. Réessayer");
                System.out.println("2. Quitter");
                System.out.print("Choix : ");

                String choix = clavier.nextLine().trim();
                if (choix.equals("2")) {
                    System.out.println("Au revoir !");
                    return;
                }
            }
        }

        // On est connecté (vrai ou simulation), créer auth
        auth = new AuthService(client);

        // Boucle principale
        while (true) {
            if (!authentifie) {
                menuNonAuthentifie();
            } else {
                menuAuthentifie();
            }
        }
    }

    // ===== TENTER CONNEXION =====
    private boolean tenterConnexion() {
        System.out.print("\nIP du serveur (tape 'simul' pour tester sans serveur) : ");
        String ip = clavier.nextLine().trim();

        // ===== MODE SIMULATION =====
        if (ip.equalsIgnoreCase("simul")) {
            System.out.println("🎮 Activation du mode simulation...");

            client = new ClientReseau(this);
            client.activerSimulation();  // Active la simulation !

            serveurConnecte = true;
            return true;
        }

        // ===== CONNEXION NORMALE =====
        if (ip.isEmpty()) {
            ip = "localhost";
        }

        System.out.print("Port (Entrée pour 8080) : ");
        String portStr = clavier.nextLine().trim();
        int port = 8080;

        if (!portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("Port invalide, utilisation de 8080.");
            }
        }

        client = new ClientReseau(this);
        client.connecterAuServeur(ip, port);

        if (client.isConnecte()) {
            serveurConnecte = true;
            return true;
        }

        return false;
    }

    // ===== MENU AVANT AUTH =====
    private void menuNonAuthentifie() {
        System.out.println("\n╔════════════════════════════╗");
        System.out.println("║     MESSAGERIE - ACCUEIL   ║");
        System.out.println("╠════════════════════════════╣");
        System.out.println("║  1. Se connecter           ║");
        System.out.println("║  2. S'inscrire             ║");
        System.out.println("║  3. Quitter                ║");
        System.out.println("╚════════════════════════════╝");
        System.out.print("Choix : ");

        String choix = clavier.nextLine().trim();

        switch (choix) {
            case "1":
                faireConnexion();
                break;
            case "2":
                faireInscription();
                break;
            case "3":
                quitter();
                break;
            default:
                System.out.println("❌ Choix invalide !");
        }
    }

    // ===== MENU APRÈS AUTH =====
    private void menuAuthentifie() {
        System.out.println("\n╔════════════════════════════╗");
        System.out.println("║      ESPACE MESSAGERIE     ║");
        System.out.println("║      Bienvenue " + client.getMoi().getNomComplet());
        System.out.println("╠════════════════════════════╣");
        System.out.println("║  1. Envoyer un message     ║");
        System.out.println("║  2. Se déconnecter         ║");
        System.out.println("║  3. Quitter                ║");
        System.out.println("╚════════════════════════════╝");
        System.out.print("Choix : ");

        String choix = clavier.nextLine().trim();

        switch (choix) {
            case "1":
                envoyerMessage();
                break;
            case "2":
                faireDeconnexion();
                break;
            case "3":
                quitter();
                break;
            default:
                System.out.println("❌ Choix invalide !");
        }
    }

    // ===== ACTIONS =====
    private void faireConnexion() {
        System.out.print("Numéro de téléphone : ");
        String num = clavier.nextLine().trim();

        System.out.print("Mot de passe : ");
        String pass = clavier.nextLine();

        // Vérification
        if (!client.isConnecte()) {
            System.out.println("❌ Pas de connexion !");
            return;
        }

        auth.connecter(num, pass);
        System.out.println("⏳ Attente...");
        attendre(1000);
    }

    private void faireInscription() {
        System.out.print("Nom : ");
        String nom = clavier.nextLine().trim();

        System.out.print("Prénom : ");
        String prenom = clavier.nextLine().trim();

        System.out.print("Numéro (10 chiffres) : ");
        String numero = clavier.nextLine().trim();

        if (numero.length() != 10 || !numero.matches("\\d+")) {
            System.out.println("❌ Numéro invalide !");
            return;
        }

        System.out.print("Username : ");
        String user = clavier.nextLine().trim();

        System.out.print("Mot de passe : ");
        String mdp = clavier.nextLine();

        if (nom.isEmpty() || prenom.isEmpty() || user.isEmpty() || mdp.isEmpty()) {
            System.out.println("❌ Tous les champs sont obligatoires !");
            return;
        }

        if (!client.isConnecte()) {
            System.out.println("❌ Pas de connexion !");
            return;
        }

        auth.inscrire(nom, prenom, numero, mdp);
        System.out.println("⏳ Attente...");
        attendre(1000);
    }

    private void envoyerMessage() {
        System.out.print("Message : ");
        String msg = clavier.nextLine();

        if (msg.isEmpty()) {
            System.out.println("❌ Message vide !");
            return;
        }

        System.out.println("📨 Message : " + msg);
    }

    private void faireDeconnexion() {
        if (auth != null) {
            auth.deconnecter();
        }
        authentifie = false;
        System.out.println("👋 Déconnecté.");
    }

    private void quitter() {
        if (client != null) {
            client.deconnecter();
        }
        System.out.println("👋 Au revoir !");
        System.exit(0);
    }

    private void attendre(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    // ===== ÉCOUTEUR CLIENT =====

    @Override
    public void connexionReussie(Utilisateur moi) {
        System.out.println("\n🎉 CONNEXION RÉUSSIE !");
        System.out.println("   Bienvenue " + moi.getNomComplet());
        System.out.println("   ID : " + moi.getId());
        System.out.println("   Username : @" + moi.getNom());
        authentifie = true;
    }

    @Override
    public void erreur(String message) {
        System.out.println("\n💥 ERREUR : " + message);
    }

    @Override
    public void messageRecu(String contenu) {
        System.out.println("\n💬 Message : " + contenu);
    }

    @Override
    public void deconnexion() {
        System.out.println("\n🔌 Déconnecté du serveur.");
        authentifie = false;
        serveurConnecte = false;
    }
}