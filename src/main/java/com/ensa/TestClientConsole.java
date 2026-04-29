package com.ensa;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TestClientConsole {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Scanner scanner;
    private String telephoneConnecte;
    private boolean running = true;

    public static void main(String[] args) {
        new TestClientConsole().start();
    }

    public void start() {
        scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     TEST CLIENT CHAT - CONSOLE       ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Connexion au serveur
        System.out.print("\nIP serveur (localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Port (5000): ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? 5000 : Integer.parseInt(portStr);

        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("\n✓ Connecté au serveur " + host + ":" + port);

            // Thread pour écouter les messages du serveur
            Thread listener = new Thread(this::ecouterServeur);
            listener.setDaemon(true);
            listener.start();

            // Menu principal
            menuPrincipal();

        } catch (IOException e) {
            System.err.println("✗ Erreur connexion: " + e.getMessage());
        }
    }

    private void menuPrincipal() {
        while (running) {
            System.out.println("\n┌─────────────────────────────────────┐");
            System.out.println("│  1. Login                           │");
            System.out.println("│  2. Register                        │");
            System.out.println("│  3. Quitter                         │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.print("Choix: ");

            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> quitter();
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void login() {
        System.out.print("\nTéléphone: ");
        String tel = scanner.nextLine().trim();
        System.out.print("Mot de passe: ");
        String mdp = scanner.nextLine().trim();

        envoyer("LOGIN|" + tel + "|" + mdp);
        telephoneConnecte = tel;

        // Attendre la réponse (gérée par le thread listener)
        attendre(1000);

        // Si connecté, passer au menu chat
        if (telephoneConnecte != null) {
            menuChat();
        }
    }

    private void register() {
        System.out.print("\nNom complet: ");
        String nom = scanner.nextLine().trim();
        System.out.print("Téléphone: ");
        String tel = scanner.nextLine().trim();
        System.out.print("Mot de passe: ");
        String mdp = scanner.nextLine().trim();

        envoyer("REGISTER|" + nom + "|" + tel + "|" + mdp);
        attendre(1000);
    }

    private void menuChat() {
        while (running && telephoneConnecte != null) {
            System.out.println("\n┌─────────────────────────────────────┐");
            System.out.println("│  CHAT - " + padRight(telephoneConnecte, 24) + "│");
            System.out.println("├─────────────────────────────────────┤");
            System.out.println("│  1. Voir mes conversations          │");
            System.out.println("│  2. Voir messages d'une conversation│");
            System.out.println("│  3. Envoyer message                 │");
            System.out.println("│  4. Appel audio                     │");
            System.out.println("│  5. Appel vidéo                     │");
            System.out.println("│  6. Logout                          │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.print("Choix: ");

            String choix = scanner.nextLine().trim();

            switch (choix) {
                case "1" -> getConversations();
                case "2" -> getMessages();
                case "3" -> envoyerMessage();
                case "4" -> appel("audio");
                case "5" -> appel("video");
                case "6" -> logout();
                default -> System.out.println("Choix invalide");
            }
        }
    }

    // ── COMMANDES ───────────────────────────────────────────────────────────

    private void getConversations() {
        System.out.println("\n--- Récupération des conversations ---");
        envoyer("GET_CONVERSATIONS");
        attendre(1500);
    }

    private void getMessages() {
        System.out.print("\nID conversation: ");
        String id = scanner.nextLine().trim();
        System.out.println("--- Récupération des messages ---");
        envoyer("GET_MESSAGES|" + id);
        attendre(1500);
    }

    private void envoyerMessage() {
        System.out.print("\nTéléphone destinataire: ");
        String dest = scanner.nextLine().trim();
        System.out.print("Message: ");
        String msg = scanner.nextLine();

        envoyer("MSG_SEND|" + dest + "|" + msg);
        System.out.println("→ Message envoyé à " + dest);
    }

    private void appel(String type) {
        System.out.print("\nTéléphone destinataire: ");
        String dest = scanner.nextLine().trim();

        envoyer("CALL_REQUEST|" + dest + "|" + type);
        System.out.println("→ Appel " + type + " en cours vers " + dest + "...");
        System.out.println("Attendez la réponse (ACCEPT/REFUSE/END)");

        // Attendre la réponse d'appel
        attendre(5000);
    }

    private void logout() {
        envoyer("LOGOUT");
        telephoneConnecte = null;
        System.out.println("→ Déconnecté");
    }

    private void quitter() {
        running = false;
        envoyer("LOGOUT");
        try {
            socket.close();
        } catch (IOException ignored) {}
        System.out.println("Au revoir !");
    }

    // ── COMMUNICATION ───────────────────────────────────────────────────────

    private void envoyer(String message) {
        writer.println(message);
        System.out.println("[ENVOYÉ] " + message);
    }

    private void ecouterServeur() {
        try {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                traiterReponse(ligne);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("\n✗ Connexion perdue avec le serveur");
            }
        }
    }

    private void traiterReponse(String ligne) {
        System.out.println("\n[REÇU] " + ligne);

        String[] parts = ligne.split("\\|", -1);
        String cmd = parts[0];

        switch (cmd) {
            case "LOGIN_OK" -> {
                if (parts.length >= 3) {
                    System.out.println("✓ Connecté: " + parts[1] + " (" + parts[2] + ")");
                }
            }
            case "LOGIN_FAIL" -> {
                System.out.println("✗ Échec login");
                telephoneConnecte = null;
            }
            case "REGISTER_OK" -> System.out.println("✓ Inscription réussie");
            case "REGISTER_FAIL" -> System.out.println("✗ Échec inscription: " +
                    (parts.length > 1 ? parts[1] : ""));

            case "CONVERSATIONS_LIST" -> afficherConversations(parts);
            case "MESSAGES_LIST" -> afficherMessages(parts);

            case "MSG_RECEIVE" -> {
                if (parts.length >= 3) {
                    System.out.println("\n💬 Nouveau message de " + parts[1] + ": " + parts[2]);
                }
            }

            case "USERS_LIST" -> {
                if (parts.length > 1) {
                    System.out.println("👥 En ligne: " + parts[1]);
                }
            }

            case "CALL_REQUEST" -> {
                if (parts.length >= 5) {
                    System.out.println("\n📞 APPEL ENTRANT de " + parts[2] +
                            " (" + parts[1] + ") - Type: " + parts[3]);
                    System.out.println("Tapez: CALL_ACCEPT|" + parts[1] +
                            " ou CALL_REFUSE|" + parts[1]);
                }
            }

            case "CALL_ACCEPT" -> {
                System.out.println("\n✓ Appel accepté par " + parts[1]);
                System.out.println("Tapez: CALL_END|" + parts[1] + " pour raccrocher");
            }

            case "CALL_REFUSE" -> System.out.println("\n✗ Appel refusé par " + parts[1]);

            case "CALL_END" -> {
                if (parts.length > 2) {
                    System.out.println("\n☎ Appel terminé: " + parts[2]);
                } else {
                    System.out.println("\n☎ Appel terminé par " + parts[1]);
                }
            }
        }
    }

    // ── AFFICHAGE FORMATÉ ─────────────────────────────────────────────────

    private void afficherConversations(String[] parts) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MES CONVERSATIONS                                 ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════╣");

        if (parts.length < 2 || parts[1].isEmpty()) {
            System.out.println("║  Aucune conversation                                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
            return;
        }

        // Format: id;type;nom;dateDernierMsg;nbNonLus;dernierMessage
        String[] convs = parts[1].split("\\|");

        System.out.println(String.format("║ %-4s %-15s %-20s %-19s %-6s %s",
                "ID", "Type", "Nom", "Date", "NonLus", "Dernier msg"));
        System.out.println("╠══════════════════════════════════════════════════════════════════════╣");

        for (String conv : convs) {
            String[] champs = conv.split(";");
            if (champs.length >= 6) {
                String nonLus = champs[4].equals("0") ? " " : "🔴" + champs[4];
                String dernier = champs[5].length() > 20 ? champs[5].substring(0, 20) + "..." : champs[5];

                System.out.println(String.format("║ %-4s %-15s %-20s %-19s %-6s %s",
                        champs[0], champs[1], champs[2], champs[3], nonLus, dernier));
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
    }

    private void afficherMessages(String[] parts) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MESSAGES                                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════╣");

        if (parts.length < 2 || parts[1].isEmpty()) {
            System.out.println("║  Aucun message                                                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
            return;
        }

        // Format: id;telExp;nomExp;contenu;date
        String[] msgs = parts[1].split("\\|");

        for (String msg : msgs) {
            String[] champs = msg.split(";");
            if (champs.length >= 5) {
                String estMoi = champs[1].equals(telephoneConnecte) ? " (MOI)" : "";
                System.out.println("┌─ " + champs[2] + estMoi + " (" + champs[1] + ") ─ " + champs[4]);
                System.out.println("│ " + champs[3]);
                System.out.println("└──────────────────────────────────────────────────────────────────────");
            }
        }
    }

    // ── UTILITAIRES ────────────────────────────────────────────────────────

    private void attendre(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return String.format("%-" + n + "s", s);
    }
}