package com.ensa;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Serveur de simulation complet pour tester toutes les fonctionnalités
 * Port : 5557
 */
public class MockServer {

    private static final int PORT = 5557;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static int msgIdCounter = 100;

    // Données de test
    private static final Map<String, String[]> USERS = Map.of(
            "0612345678", new String[]{"Jean", "pass123"},
            "0611221122", new String[]{"Maryam", "pass123"},
            "0600112211", new String[]{"Samira", "pass123"}
    );

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   MOCK SERVER COMPLET                ║");
        System.out.println("║   Port: " + PORT + "                    ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("\nUtilisateurs de test :");
        USERS.forEach((tel, info) ->
                System.out.println("  📱 " + info[0] + " | " + tel + " | mdp: " + info[1]));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String telephone;
        private String nom;
        private boolean connecte = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String ligne;
                while ((ligne = in.readLine()) != null) {
                    System.out.println("[" + telephone + "] 📥 " + ligne);
                    traiterCommande(ligne);
                }
            } catch (IOException e) {
                System.out.println("❌ Client déconnecté : " + nom);
            } finally {
                deconnecter();
            }
        }

        private void traiterCommande(String ligne) {
            String[] parts = ligne.split("\\|", -1);
            String cmd = parts[0];

            try {
                switch (cmd) {
                    case "LOGIN" -> handleLogin(parts);
                    case "REGISTER" -> handleRegister(parts);
                    case "LOGOUT" -> handleLogout();
                    case "MSG_SEND" -> handleMessage(parts);
                    case "GET_CONVERSATIONS" -> handleGetConversations();
                    case "GET_MESSAGES" -> handleGetMessages(parts);
                    case "CALL_REQUEST" -> handleCallRequest(parts);
                    case "CALL_ACCEPT" -> handleCallAccept(parts);
                    case "CALL_REFUSE" -> handleCallRefuse(parts);
                    case "CALL_END" -> handleCallEnd(parts);
                    default -> envoyer("UNKNOWN_COMMAND");
                }
            } catch (Exception e) {
                envoyer("ERROR|" + e.getMessage());
            }
        }

        private void handleLogin(String[] parts) {
            if (parts.length < 3) {
                envoyer("LOGIN_FAIL|Format invalide");
                return;
            }

            String tel = parts[1];
            String pass = parts[2];

            if (USERS.containsKey(tel) && USERS.get(tel)[1].equals(pass)) {
                this.telephone = tel;
                this.nom = USERS.get(tel)[0];
                this.connecte = true;
                clients.put(tel, this);

                envoyer("LOGIN_OK|" + nom + "|" + tel);

                // Simuler messages en attente
                sleep(300);
                envoyer("MSG_RECEIVE|0611221122|Salut " + nom + " ! Bienvenue 👋");

            } else {
                envoyer("LOGIN_FAIL|Téléphone ou mot de passe incorrect");
            }
        }

        private void handleRegister(String[] parts) {
            if (parts.length < 4) {
                envoyer("REGISTER_FAIL|Format invalide");
                return;
            }
            envoyer("REGISTER_OK|Inscription réussie|" + parts[2]);
        }

        private void handleLogout() {
            deconnecter();
        }

        private void handleMessage(String[] parts) {
            if (parts.length < 3 || !connecte) return;

            String dest = parts[1];
            String contenu = parts[2];

            // Si destinataire connecté, lui envoyer directement
            ClientHandler destHandler = clients.get(dest);
            if (destHandler != null) {
                destHandler.envoyer("MSG_RECEIVE|" + telephone + "|" + contenu);
            }

            // Echo à l'expéditeur pour confirmation
            System.out.println("💬 [" + nom + " → " + dest + "] : " + contenu);
        }

        private void handleGetConversations() {
            if (!connecte) return;

            StringBuilder sb = new StringBuilder("CONVERSATIONS_LIST|");

            // Générer des conversations selon l'utilisateur connecté
            switch (telephone) {
                case "0612345678" -> { // Jean
                    sb.append("1;individuelle;Maryam;")
                            .append(LocalDateTime.now().minusMinutes(5))
                            .append(";3;Salut Jean ça va ?|");
                    sb.append("2;individuelle;Samira;")
                            .append(LocalDateTime.now().minusHours(2))
                            .append(";0;À demain alors|");
                    sb.append("3;groupe;Projet Java;")
                            .append(LocalDateTime.now().minusDays(1))
                            .append(";5;Réunion demain 14h|");
                }
                case "0611221122" -> { // Maryam
                    sb.append("1;individuelle;Jean;")
                            .append(LocalDateTime.now().minusMinutes(5))
                            .append(";0;Oui super et toi ?|");
                    sb.append("4;individuelle;Samira;")
                            .append(LocalDateTime.now().minusMinutes(30))
                            .append(";2;Tu viens ce soir ?|");
                }
                case "0600112211" -> { // Samira
                    sb.append("2;individuelle;Jean;")
                            .append(LocalDateTime.now().minusHours(2))
                            .append(";1;Ok merci|");
                    sb.append("4;individuelle;Maryam;")
                            .append(LocalDateTime.now().minusMinutes(30))
                            .append(";0;Oui j'arrive|");
                }
            }

            envoyer(sb.toString());
        }

        private void handleGetMessages(String[] parts) {
            if (parts.length < 2 || !connecte) return;

            String idConv = parts[1];
            StringBuilder sb = new StringBuilder("MESSAGES_LIST|");

            // Messages prédéfinis selon la conversation
            switch (idConv) {
                case "1" -> { // Jean ↔ Maryam
                    if ("0612345678".equals(telephone)) { // Jean voit
                        sb.append("1;0611221122;Maryam;Salut Jean !;")
                                .append(LocalDateTime.now().minusMinutes(20)).append("|");
                        sb.append("2;0612345678;Jean;Hey Maryam;")
                                .append(LocalDateTime.now().minusMinutes(18)).append("|");
                        sb.append("3;0611221122;Maryam;Tu fais quoi ?;")
                                .append(LocalDateTime.now().minusMinutes(15)).append("|");
                        sb.append("4;0612345678;Jean;Je teste mon app;")
                                .append(LocalDateTime.now().minusMinutes(10)).append("|");
                        sb.append("5;0611221122;Maryam;Cool ! Ça marche ?;")
                                .append(LocalDateTime.now().minusMinutes(5)).append("|");
                    } else { // Maryam voit
                        sb.append("1;0611221122;Maryam;Salut Jean !;")
                                .append(LocalDateTime.now().minusMinutes(20)).append("|");
                        sb.append("2;0612345678;Jean;Hey Maryam;")
                                .append(LocalDateTime.now().minusMinutes(18)).append("|");
                        sb.append("3;0611221122;Maryam;Tu fais quoi ?;")
                                .append(LocalDateTime.now().minusMinutes(15)).append("|");
                        sb.append("4;0612345678;Jean;Je teste mon app;")
                                .append(LocalDateTime.now().minusMinutes(10)).append("|");
                        sb.append("5;0611221122;Maryam;Cool ! Ça marche ?;")
                                .append(LocalDateTime.now().minusMinutes(5)).append("|");
                    }
                }
                case "2" -> { // Jean ↔ Samira
                    sb.append("10;0600112211;Samira;Hello Jean;")
                            .append(LocalDateTime.now().minusHours(3)).append("|");
                    sb.append("11;0612345678;Jean;Salut Samira;")
                            .append(LocalDateTime.now().minusHours(2)).append("|");
                    sb.append("12;0600112211;Samira;On avance sur le projet ?;")
                            .append(LocalDateTime.now().minusHours(1)).append("|");
                }
                case "3" -> { // Groupe
                    sb.append("20;0612345678;Jean;Réunion demain 14h;")
                            .append(LocalDateTime.now().minusDays(1)).append("|");
                    sb.append("21;0611221122;Maryam;Ok pour moi;")
                            .append(LocalDateTime.now().minusDays(1).plusMinutes(10)).append("|");
                    sb.append("22;0600112211;Samira;Pareil;")
                            .append(LocalDateTime.now().minusDays(1).plusMinutes(15)).append("|");
                    sb.append("23;0612345678;Jean;Parfait, je prépare le doc;")
                            .append(LocalDateTime.now().minusDays(1).plusMinutes(20)).append("|");
                }
                case "4" -> { // Maryam ↔ Samira
                    sb.append("30;0611221122;Maryam;Tu viens ce soir ?;")
                            .append(LocalDateTime.now().minusMinutes(45)).append("|");
                    sb.append("31;0600112211;Samira;Oui j'arrive vers 20h;")
                            .append(LocalDateTime.now().minusMinutes(30)).append("|");
                }
            }

            envoyer(sb.toString());
        }

        private void handleCallRequest(String[] parts) {
            if (parts.length < 3) return;
            String dest = parts[1];
            String type = parts[2];

            ClientHandler handler = clients.get(dest);
            if (handler != null) {
                handler.envoyer("CALL_REQUEST|" + telephone + "|" + nom + "|" + type + "|" + msgIdCounter++);
            } else {
                envoyer("CALL_END|" + dest + "|HORS_LIGNE");
            }
        }

        private void handleCallAccept(String[] parts) {
            if (parts.length < 2) return;
            ClientHandler h = clients.get(parts[1]);
            if (h != null) h.envoyer("CALL_ACCEPT|" + telephone + "|192.168.1.50");
        }

        private void handleCallRefuse(String[] parts) {
            if (parts.length < 2) return;
            ClientHandler h = clients.get(parts[1]);
            if (h != null) h.envoyer("CALL_REFUSE|" + telephone);
        }

        private void handleCallEnd(String[] parts) {
            if (parts.length < 2) return;
            ClientHandler h = clients.get(parts[1]);
            if (h != null) h.envoyer("CALL_END|" + telephone);
        }

        private void deconnecter() {
            if (telephone != null) {
                clients.remove(telephone);
                connecte = false;
                System.out.println("👋 " + nom + " déconnecté");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }

        private void envoyer(String msg) {
            if (out != null) {
                out.println(msg);
                String preview = msg.length() > 80 ? msg.substring(0, 80) + "..." : msg;
                System.out.println("[" + nom + "] 📤 " + preview);
            }
        }

        private void sleep(int ms) {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
        }
    }
}