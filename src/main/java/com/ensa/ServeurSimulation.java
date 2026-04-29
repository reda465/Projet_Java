package com.ensa;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Serveur de simulation pour tester l'application WhatsApp Clone
 * Supporte : Auth, Messages, Conversations, Appels Audio/Vidéo
 * Port par défaut : 8080
 */
public class ServeurSimulation {

    public static final int PORT = 8080;
    public static final int UDP_PORT_LOCAL = 5001;
    public static final int UDP_PORT_DISTANT = 5002;

    private static final Map<String, ClientHandler> clientsConnectes = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> messagesEnAttente = new ConcurrentHashMap<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   SERVEUR SIMULATION WHATSAPP        ║");
        System.out.println("║   Port : " + PORT + "                    ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Serveur démarré et en écoute...");
            System.out.println("📡 IP Locale : " + getLocalIpAddress() + "\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                System.out.println("[" + heure() + "] 🔗 Nouveau client : " + clientId);

                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        }
    }

    private static String heure() {
        return LocalDateTime.now().format(formatter);
    }

    /**
     * Obtient l'IP locale réelle (pas localhost)
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // Ignorer loopback et interfaces inactives
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Prendre seulement IPv4
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    /**
     * Gestionnaire de client (1 thread par client)
     */
    static class ClientHandler extends Thread {
        private final Socket socket;
        private final String clientId;
        private PrintWriter out;
        private BufferedReader in;
        private String telephoneConnecte = null;
        private String nomComplet = null;
        private boolean running = true;

        // Données simulées en mémoire
        private static final Map<String, UtilisateurSimule> utilisateurs = new HashMap<>();
        private static final Map<String, List<ConversationSimulee>> conversations = new HashMap<>();
        private static final Map<String, AppelEnCours> appelsActifs = new ConcurrentHashMap<>();

        static {
            // Utilisateurs de test
            utilisateurs.put("0611221122", new UtilisateurSimule(1, "Maryam", "0611221122", "pass123"));
            utilisateurs.put("0611111111", new UtilisateurSimule(2, "Ali", "0611111111", "pass123"));
            utilisateurs.put("0622222222", new UtilisateurSimule(3, "Sara", "0622222222", "pass123"));
            utilisateurs.put("0633333333", new UtilisateurSimule(4, "Mohamed", "0633333333", "pass123"));

            // Conversations de test pour Maryam
            List<ConversationSimulee> convsMaryam = new ArrayList<>();
            convsMaryam.add(new ConversationSimulee(1, "Ali", "0611111111",
                    "Salut Maryam !", LocalDateTime.now().minusMinutes(30), 2));
            convsMaryam.add(new ConversationSimulee(2, "Sara", "0622222222",
                    "Ça va ?", LocalDateTime.now().minusHours(2), 0));
            convsMaryam.add(new ConversationSimulee(3, "Mohamed", "0633333333",
                    "On se voit demain ?", LocalDateTime.now().minusDays(1), 5));
            conversations.put("0611221122", convsMaryam);

            // Conversations de test pour Ali
            List<ConversationSimulee> convsAli = new ArrayList<>();
            convsAli.add(new ConversationSimulee(1, "Maryam", "0611221122",
                    "Salut Maryam !", LocalDateTime.now().minusMinutes(30), 0));
            conversations.put("0611111111", convsAli);

            // Conversations pour Sara
            List<ConversationSimulee> convsSara = new ArrayList<>();
            convsSara.add(new ConversationSimulee(1, "Maryam", "0611221122",
                    "Ça va ?", LocalDateTime.now().minusHours(2), 1));
            conversations.put("0622222222", convsSara);
        }

        public ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String ligne;
                while (running && (ligne = in.readLine()) != null) {
                    System.out.println("[" + heure() + "] 📥 [" + clientId + "] " + ligne);
                    traiterCommande(ligne);
                }
            } catch (IOException e) {
                System.out.println("[" + heure() + "] ⚠️ Client déconnecté : " + clientId);
            } finally {
                deconnecter();
            }
        }

        private void traiterCommande(String ligne) {
            if (ligne == null || ligne.trim().isEmpty()) return;

            String[] parts = ligne.split("\\|", -1);
            String commande = parts[0];

            try {
                switch (commande) {
                    case "LOGIN":
                        handleLogin(parts);
                        break;
                    case "REGISTER":
                        handleRegister(parts);
                        break;
                    case "LOGOUT":
                        handleLogout();
                        break;
                    case "MSG_SEND":
                        handleMessage(parts);
                        break;
                    case "LISTE_CONVERSATIONS":
                        handleListeConversations();
                        break;
                    case "USERS_LIST":
                        handleUsersList();
                        break;
                    case "CALL_REQUEST":
                        handleCallRequest(parts);
                        break;
                    case "CALL_ACCEPT":
                        handleCallAccept(parts);
                        break;
                    case "CALL_REFUSE":
                        handleCallRefuse(parts);
                        break;
                    case "CALL_END":
                        handleCallEnd(parts);
                        break;
                    case "CALL_CANCEL":
                        handleCallCancel(parts);
                        break;
                    default:
                        System.out.println("[" + heure() + "] ⚠️ Commande inconnue : " + commande);
                        // Ne pas envoyer d'erreur pour éviter de casser le parser client
                }
            } catch (Exception e) {
                System.out.println("[" + heure() + "] ❌ Erreur traitement : " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ========== AUTHENTIFICATION ==========

        private void handleLogin(String[] parts) {
            if (parts.length < 3) {
                envoyer("LOGIN_FAIL|Format invalide");
                return;
            }

            String tel = parts[1];
            String password = parts[2];

            UtilisateurSimule u = utilisateurs.get(tel);
            if (u != null && u.password.equals(password)) {
                telephoneConnecte = tel;
                nomComplet = u.nomComplet;

                // Enregistrer dans la map des clients connectés
                clientsConnectes.put(tel, this);

                System.out.println("[" + heure() + "] ✅ " + nomComplet + " connecté (" + tel + ")");

                // Envoyer succès : LOGIN_OK|nom_complet|numero_telephone
                envoyer("LOGIN_OK|" + nomComplet + "|" + tel);

                // Envoyer messages en attente
                List<String> attente = messagesEnAttente.get(tel);
                if (attente != null && !attente.isEmpty()) {
                    for (String msg : attente) {
                        envoyer(msg);
                    }
                    messagesEnAttente.remove(tel);
                }

                // Broadcast liste utilisateurs à tous
                broadcastUsersList();

            } else {
                envoyer("LOGIN_FAIL|Numéro ou mot de passe incorrect");
            }
        }

        private void handleRegister(String[] parts) {
            if (parts.length < 4) {
                envoyer("REGISTER_FAIL|Format invalide");
                return;
            }

            String nomComplet = parts[1];
            String tel = parts[2];
            String password = parts[3];

            if (utilisateurs.containsKey(tel)) {
                envoyer("REGISTER_FAIL|TELEPHONE_EXISTE");
                return;
            }

            int newId = utilisateurs.size() + 1;
            utilisateurs.put(tel, new UtilisateurSimule(newId, nomComplet, tel, password));
            conversations.put(tel, new ArrayList<>());

            System.out.println("[" + heure() + "] ✅ Nouvel utilisateur : " + nomComplet + " (" + tel + ")");
            envoyer("REGISTER_OK|Inscription_Avec_Succes");
        }

        private void handleLogout() {
            if (telephoneConnecte != null) {
                System.out.println("[" + heure() + "] 👋 Déconnexion : " + nomComplet);
                clientsConnectes.remove(telephoneConnecte);

                // Terminer les appels en cours
                AppelEnCours appel = appelsActifs.remove(telephoneConnecte);
                if (appel != null) {
                    String autre = appel.appelant.equals(telephoneConnecte) ? appel.destinataire : appel.appelant;
                    appelsActifs.remove(autre);
                    ClientHandler autreHandler = clientsConnectes.get(autre);
                    if (autreHandler != null) {
                        autreHandler.envoyer("CALL_END|" + telephoneConnecte + "|DECONNEXION");
                    }
                }

                telephoneConnecte = null;
                nomComplet = null;
                broadcastUsersList();
            }
            running = false;
            envoyer("LOGOUT|");
        }

        // ========== MESSAGERIE ==========

        private void handleMessage(String[] parts) {
            if (parts.length < 3) {
                envoyer("MSG_FAIL|Format invalide");
                return;
            }

            if (telephoneConnecte == null) {
                envoyer("MSG_FAIL|Non authentifié");
                return;
            }

            String dest = parts[1];
            String contenu = parts[2];

            System.out.println("[" + heure() + "] 💬 " + nomComplet + " → " + dest + " : " + contenu);

            // Construire le message à envoyer au destinataire
            String msgAEnvoyer = "MSG_RECEIVE|" + telephoneConnecte + "|" + contenu;

            // Envoyer au destinataire s'il est connecté
            ClientHandler destHandler = clientsConnectes.get(dest);
            if (destHandler != null) {
                destHandler.envoyer(msgAEnvoyer);
                System.out.println("[" + heure() + "] 📤 Message livré à " + dest);
            } else {
                // Stocker pour plus tard
                messagesEnAttente.computeIfAbsent(dest, k -> new ArrayList<>()).add(msgAEnvoyer);
                System.out.println("[" + heure() + "] 📥 Message en attente pour " + dest);
            }

            // Confirmer à l'expéditeur
            envoyer("MSG_OK|");
        }

        // ========== CONVERSATIONS ==========

        private void handleListeConversations() {
            if (telephoneConnecte == null) {
                envoyer("ERREUR|Non authentifié");
                return;
            }

            List<ConversationSimulee> convs = conversations.getOrDefault(telephoneConnecte, new ArrayList<>());

            StringBuilder sb = new StringBuilder();
            sb.append("CONVERSATIONS_RECUES|");

            for (int i = 0; i < convs.size(); i++) {
                ConversationSimulee c = convs.get(i);
                if (i > 0) sb.append(";");
                sb.append(c.id).append("|")
                        .append(c.nomContact).append("|")
                        .append(c.numeroContact).append("|")
                        .append(c.dernierMessage).append("|")
                        .append(c.dateDernierMessage).append("|")
                        .append(c.messagesNonLus);
            }

            envoyer(sb.toString());
            System.out.println("[" + heure() + "] 📋 Conversations envoyées à " + nomComplet + " (" + convs.size() + ")");
        }

        // ========== LISTE UTILISATEURS ==========

        private void handleUsersList() {
            StringBuilder sb = new StringBuilder();
            sb.append("USERS_LIST|");

            boolean first = true;
            for (Map.Entry<String, ClientHandler> entry : clientsConnectes.entrySet()) {
                if (!first) sb.append(";");
                first = false;

                String tel = entry.getKey();
                UtilisateurSimule u = utilisateurs.get(tel);
                if (u != null) {
                    sb.append(u.nomComplet).append("|").append(tel);
                }
            }

            envoyer(sb.toString());
        }

        private void broadcastUsersList() {
            StringBuilder sb = new StringBuilder();
            sb.append("USERS_LIST|");

            boolean first = true;
            for (Map.Entry<String, ClientHandler> entry : clientsConnectes.entrySet()) {
                if (!first) sb.append(";");
                first = false;

                String tel = entry.getKey();
                UtilisateurSimule u = utilisateurs.get(tel);
                if (u != null) {
                    sb.append(u.nomComplet).append("|").append(tel);
                }
            }

            // Envoyer à tous les clients connectés
            for (ClientHandler handler : clientsConnectes.values()) {
                handler.envoyer(sb.toString());
            }
        }

        // ========== APPELS ==========

        private void handleCallRequest(String[] parts) {
            if (parts.length < 3) {
                envoyer("CALL_END|Format invalide");
                return;
            }

            if (telephoneConnecte == null) {
                envoyer("CALL_END|Non authentifié");
                return;
            }

            String dest = parts[1];
            String typeAppel = parts[2]; // "audio" ou "video"

            System.out.println("[" + heure() + "] 📞 " + nomComplet + " appelle " + dest + " (" + typeAppel + ")");

            ClientHandler destHandler = clientsConnectes.get(dest);

            if (destHandler == null) {
                System.out.println("[" + heure() + "] ❌ " + dest + " hors ligne");
                envoyer("CALL_END|" + dest + "|HORS_LIGNE");
                return;
            }

            // Vérifier si le destinataire est déjà en appel
            if (appelsActifs.containsKey(dest)) {
                System.out.println("[" + heure() + "] ❌ " + dest + " occupé");
                envoyer("CALL_END|" + dest + "|OCCUPE");
                return;
            }

            // Enregistrer l'appel
            String idAppel = telephoneConnecte + "_" + dest + "_" + System.currentTimeMillis();
            appelsActifs.put(telephoneConnecte, new AppelEnCours(idAppel, telephoneConnecte, dest));
            appelsActifs.put(dest, new AppelEnCours(idAppel, telephoneConnecte, dest));

            // Récupérer la VRAIE IP de l'appelant (pour réseau local)
            String ipAppelant = getRealClientIp();

            // Notifier le destinataire
            // Format : CALL_REQUEST|numAppelant|nomAppelant|typeAppel|idAppel|ipAppelant
            destHandler.envoyer("CALL_REQUEST|" + telephoneConnecte + "|" + nomComplet + "|" + typeAppel + "|" + idAppel + "|" + ipAppelant);

            System.out.println("[" + heure() + "] 📤 Notification envoyée à " + dest + " (IP appelant: " + ipAppelant + ")");
        }

        private void handleCallAccept(String[] parts) {
            if (parts.length < 2) return;

            String appelant = parts[1];

            AppelEnCours appel = appelsActifs.get(telephoneConnecte);
            if (appel == null) {
                System.out.println("[" + heure() + "] ⚠️ Pas d'appel trouvé pour " + telephoneConnecte);
                return;
            }

            System.out.println("[" + heure() + "] ✅ " + nomComplet + " a accepté l'appel de " + appelant);

            // Récupérer la VRAIE IP de l'accepteur
            String ipAccepteur = getRealClientIp();

            // Notifier l'appelant
            ClientHandler appelantHandler = clientsConnectes.get(appelant);
            if (appelantHandler != null) {
                // Format : CALL_ACCEPT|telephoneAccepteur|ipAccepteur
                appelantHandler.envoyer("CALL_ACCEPT|" + telephoneConnecte + "|" + ipAccepteur);
                System.out.println("[" + heure() + "] 📤 Accept envoyé à " + appelant + " (IP: " + ipAccepteur + ")");
            }

            // Notifier aussi l'accepteur avec l'IP de l'appelant
            String ipAppelant = (appelantHandler != null) ? appelantHandler.getRealClientIp() : "127.0.0.1";
            envoyer("CALL_ACCEPT|" + appelant + "|" + ipAppelant);
        }

        private void handleCallRefuse(String[] parts) {
            if (parts.length < 2) return;

            String appelant = parts[1];

            AppelEnCours appel = appelsActifs.remove(telephoneConnecte);
            if (appel != null) {
                appelsActifs.remove(appel.appelant);
            }

            System.out.println("[" + heure() + "] ❌ " + nomComplet + " a refusé l'appel de " + appelant);

            // Notifier l'appelant
            ClientHandler appelantHandler = clientsConnectes.get(appelant);
            if (appelantHandler != null) {
                appelantHandler.envoyer("CALL_REFUSE|" + telephoneConnecte);
            }

            envoyer("CALL_REFUSE|");
        }

        private void handleCallEnd(String[] parts) {
            if (parts.length < 2) return;

            String autre = parts[1];

            AppelEnCours appel = appelsActifs.remove(telephoneConnecte);
            if (appel != null) {
                appelsActifs.remove(autre);
            }

            System.out.println("[" + heure() + "] 📞 " + nomComplet + " a raccroché avec " + autre);

            // Notifier l'autre
            ClientHandler autreHandler = clientsConnectes.get(autre);
            if (autreHandler != null) {
                autreHandler.envoyer("CALL_END|" + telephoneConnecte);
            }

            envoyer("CALL_END|");
        }

        private void handleCallCancel(String[] parts) {
            if (parts.length < 2) return;

            String dest = parts[1];

            AppelEnCours appel = appelsActifs.remove(telephoneConnecte);
            if (appel != null) {
                appelsActifs.remove(dest);
            }

            System.out.println("[" + heure() + "] 🚫 " + nomComplet + " a annulé l'appel vers " + dest);

            // Notifier le destinataire
            ClientHandler destHandler = clientsConnectes.get(dest);
            if (destHandler != null) {
                destHandler.envoyer("CALL_END|" + telephoneConnecte + "|ANNULE");
            }
        }

        // ========== UTILITAIRES RÉSEAU ==========

        /**
         * Récupère la vraie IP du client (pas 127.0.0.1 si possible)
         */
        private String getRealClientIp() {
            String ip = socket.getInetAddress().getHostAddress();

            // Si c'est localhost, essayer d'obtenir l'IP locale du serveur
            if (ip.equals("127.0.0.1") || ip.equals("::1")) {
                return getLocalIpAddress();
            }

            return ip;
        }

        // ========== UTILITAIRES ==========

        private void envoyer(String message) {
            System.out.println("[" + heure() + "] 📤 [" + clientId + "] " + message);
            if (out != null) {
                out.println(message);
            }
        }

        private void deconnecter() {
            if (telephoneConnecte != null) {
                clientsConnectes.remove(telephoneConnecte);

                // Terminer les appels en cours
                AppelEnCours appel = appelsActifs.remove(telephoneConnecte);
                if (appel != null) {
                    String autre = appel.appelant.equals(telephoneConnecte) ? appel.destinataire : appel.appelant;
                    appelsActifs.remove(autre);
                    ClientHandler autreHandler = clientsConnectes.get(autre);
                    if (autreHandler != null) {
                        autreHandler.envoyer("CALL_END|" + telephoneConnecte + "|DECONNEXION");
                    }
                }

                broadcastUsersList();
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    // ========== CLASSES SIMULÉES ==========

    static class UtilisateurSimule {
        int id;
        String nomComplet;
        String telephone;
        String password;

        UtilisateurSimule(int id, String nomComplet, String telephone, String password) {
            this.id = id;
            this.nomComplet = nomComplet;
            this.telephone = telephone;
            this.password = password;
        }
    }

    static class ConversationSimulee {
        int id;
        String nomContact;
        String numeroContact;
        String dernierMessage;
        LocalDateTime dateDernierMessage;
        int messagesNonLus;

        ConversationSimulee(int id, String nomContact, String numeroContact,
                            String dernierMessage, LocalDateTime dateDernierMessage, int messagesNonLus) {
            this.id = id;
            this.nomContact = nomContact;
            this.numeroContact = numeroContact;
            this.dernierMessage = dernierMessage;
            this.dateDernierMessage = dateDernierMessage;
            this.messagesNonLus = messagesNonLus;
        }
    }

    static class AppelEnCours {
        String idAppel;
        String appelant;
        String destinataire;

        AppelEnCours(String idAppel, String appelant, String destinataire) {
            this.idAppel = idAppel;
            this.appelant = appelant;
            this.destinataire = destinataire;
        }
    }
}