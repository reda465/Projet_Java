package com.ensa;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;

import static Serveur.Protocol.*;

public class ServeurSimulation {
    public static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Serveur de simulation démarré sur le port " + PORT);
            System.out.println("En attente de connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Client connecté : " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean connected = false;
        private String currentUser = "";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String ligne;
                while ((ligne = in.readLine()) != null) {
                    System.out.println("📥 RECU : " + ligne);
                    traiterCommande(ligne);
                }
            } catch (IOException e) {
                System.out.println("⚠️ Client déconnecté : " + currentUser);
            } finally {
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }

        private void traiterCommande(String ligne) {
            if (ligne == null || ligne.trim().isEmpty()) return;

            String[] parts = ligne.split("\\|", 2);
            String commande = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (commande) {
                case "LOGIN":
                    handleConnexion(data);
                    break;
                case "REGISTER":
                    handleInscription(data);
                    break;
                case "LISTE_CONVERSATIONS":
                    handleListeConversations();
                    break;
                case "MSG_SEND":
                    handleMessage(data);
                    break;
                case "USERS_LIST":
                    handleUsersList();
                    break;
                case "LOGOUT":
                    handleDeconnexion();
                    break;
                default:
                    envoyer("ERREUR|Commande inconnue : " + commande);
            }
        }

        private void handleConnexion(String data) {
            // Format attendu : numero|password
            String[] creds = data.split("\\|");
            if (creds.length >= 2) {
                String numero = creds[0];
                String password = creds[1];

                // Simulation : tout passe
                if (!numero.isEmpty() && !password.isEmpty()) {
                    connected = true;
                    currentUser = numero;

                    // ⚠️ IMPORTANT : Ajuste ce format selon ton parseur Utilisateur côté client
                    // Format : SUCCES|id|nom|prenom|numero|email|dateInscription
                    String reponse = "LOGIN_OK|Jean Dopun|" + numero;
                    envoyer(reponse);
                    return;
                }
            }
            envoyer("ERREUR|Numéro ou mot de passe incorrect");
        }

        private void handleInscription(String data) {
            // Format attendu : nom|prenom|numero|password|email
            String[] creds = data.split("\\|");
            if (creds.length >= 5) {
                String nom = creds[0];
                String numero = creds[1];

                connected = true;
                currentUser = numero;

                String reponse = "REGISTER_OK|2|" + nom +"|" + numero;
                envoyer(reponse);
            } else {
                envoyer("ERREUR|Données d'inscription incomplètes");
            }
        }

        private void handleListeConversations() {
            if (!connected) {
                envoyer("ERREUR|Non authentifié");
                return;
            }

            // Format : id|nom|numero|dernierMsg|date|nonLus;id|nom|...
            String conversations =
                    "1|Ali|0611111111|Salut !|2025-04-27T14:30:00|2;" +
                            "2|Sara|0622222222|Ça va ?|2025-04-27T15:00:00|0;" +
                            "3|Mohamed|0633333333|On se voit demain ?|2025-04-27T16:45:00|5";

            envoyer("CONVERSATIONS_RECUES|" + conversations);
        }

        private void handleMessage(String data) {
            if (!connected) {
                envoyer("ERREUR|Non authentifié");
                return;
            }
            // Format : destinataire|contenu
            envoyer("MSG_SEND|");

            // Simulation : renvoie un message reçu après 1 seconde
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    envoyer("MSG_RECEIVE|" + currentUser + "|Réponse automatique du serveur|" + LocalDateTime.now());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void handleUsersList() {
            if (!connected) {
                envoyer("ERREUR|Non authentifié");
                return;
            }
            String users = "Ali|0611111111;Sara|0622222222;Mohamed|0633333333";
            envoyer("USERS_LIST|" + users);
        }

        private void handleDeconnexion() {
            envoyer("DECONNEXION|");
            connected = false;
        }

        private void envoyer(String message) {
            System.out.println("📤 ENVOI : " + message);
            out.println(message);
        }
    }
}