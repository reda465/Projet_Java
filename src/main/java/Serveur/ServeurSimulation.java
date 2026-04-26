package Serveur;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Serveur de simulation pour tester la liaison UI ↔ réseau
 * Simule LOGIN_OK, LOGIN_FAIL, REGISTER_OK, REGISTER_FAIL
 * Sans base de données — utilisateurs stockés en mémoire
 */
public class ServeurSimulation {

    private static final int PORT = 8080;

    // Base de données en mémoire : numero → "nomComplet|password"
    private static final Map<String, String[]> utilisateurs = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // Compte de test préchargé
        utilisateurs.put("0612345678", new String[]{"Alice Dupont", "1234"});
        utilisateurs.put("0698765432", new String[]{"Bob Martin",  "abcd"});

        ServerSocket serveur = new ServerSocket(PORT);
        System.out.println("===========================================");
        System.out.println("  Serveur de simulation démarré");
        System.out.println("  Port : " + PORT);
        System.out.println("  Comptes préchargés :");
        System.out.println("    0612345678 / 1234  (Alice Dupont)");
        System.out.println("    0698765432 / abcd  (Bob Martin)");
        System.out.println("===========================================\n");

        while (true) {
            Socket client = serveur.accept();
            System.out.println("✅ Nouveau client connecté : " + client.getInetAddress());
            new Thread(new GestionnaireClient(client)).start();
        }
    }

    // ─── Gestionnaire d'un client ──────────────────────────────────────────
    static class GestionnaireClient implements Runnable {

        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        GestionnaireClient(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String ligne;
                while ((ligne = in.readLine()) != null) {
                    System.out.println("📨 Reçu : " + ligne);
                    traiter(ligne);
                }

            } catch (IOException e) {
                System.out.println("❌ Client déconnecté : " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void traiter(String ligne) {
            // Format Packet : "COMMANDE|data"  (selon Packet.toString() de ton collègue)
            String[] parts = ligne.split("\\|", 2);
            if (parts.length == 0) return;

            String commande = parts[0].trim();
            String data     = parts.length > 1 ? parts[1].trim() : "";

            switch (commande) {

                // ─── LOGIN ──────────────────────────────────────────────
                case "LOGIN": {
                    String[] creds = data.split("\\|");
                    if (creds.length < 2) {
                        envoyer("LOGIN_FAIL|Format invalide");
                        return;
                    }
                    String numero   = creds[0];
                    String password = creds[1];

                    if (utilisateurs.containsKey(numero)) {
                        String[] infos = utilisateurs.get(numero);
                        if (infos[1].equals(password)) {
                            // LOGIN_OK|nomComplet|numero
                            envoyer("LOGIN_OK|" + infos[0] + "|" + numero);
                            System.out.println("  → LOGIN_OK pour " + infos[0]);
                        } else {
                            envoyer("LOGIN_FAIL|Mot de passe incorrect");
                            System.out.println("  → LOGIN_FAIL (mauvais password)");
                        }
                    } else {
                        envoyer("LOGIN_FAIL|Numéro introuvable");
                        System.out.println("  → LOGIN_FAIL (numéro inconnu)");
                    }
                    break;
                }

                // ─── REGISTER ───────────────────────────────────────────
                case "REGISTER": {
                    String[] infos = data.split("\\|");
                    if (infos.length < 3) {
                        envoyer("REGISTER_FAIL|Format invalide");
                        return;
                    }
                    String nomComplet = infos[0];
                    String numero     = infos[1];
                    String password   = infos[2];

                    if (utilisateurs.containsKey(numero)) {
                        envoyer("REGISTER_FAIL|Ce numéro est déjà utilisé");
                        System.out.println("  → REGISTER_FAIL (numéro existant)");
                    } else {
                        utilisateurs.put(numero, new String[]{nomComplet, password});
                        envoyer("REGISTER_OK|Compte créé avec succès !");
                        System.out.println("  → REGISTER_OK pour " + nomComplet);
                    }
                    break;
                }

                // ─── LOGOUT ─────────────────────────────────────────────
                case "LOGOUT": {
                    System.out.println("  → LOGOUT reçu, fermeture connexion");
                    try { socket.close(); } catch (IOException ignored) {}
                    break;
                }

                default:
                    System.out.println("  → Commande inconnue : " + commande);
            }
        }

        private void envoyer(String message) {
            out.println(message);
            System.out.println("📤 Envoyé : " + message);
        }
    }
}