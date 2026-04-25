package Serveur; // Ou le package de ton choix

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurSimulation {

    public static void main(String[] args) {
        int port = 5000; // Le port utilisé par ton ClientHandlerAuth

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✅ [SERVEUR SIMULATION] Démarré sur le port " + port);
            System.out.println("⏳ En attente de la connexion du client (UI)...");

            while (true) {
                // Le serveur se met en pause ici jusqu'à ce que ton UI tente de se connecter
                Socket clientSocket = serverSocket.accept();
                System.out.println("📱 [SERVEUR SIMULATION] Un client s'est connecté : " + clientSocket.getInetAddress());

                // On gère ce client dans un Thread séparé pour ne pas bloquer le serveur
                new Thread(() -> gererClient(clientSocket)).start();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur de démarrage du serveur : " + e.getMessage());
        }
    }

    private static void gererClient(Socket clientSocket) {
        // IMPORTANT : Toujours initialiser le OutputStream AVANT le InputStream avec les ObjectStreams
        try (
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            System.out.println("🎧 [SERVEUR SIMULATION] Prêt à écouter les paquets du client...");

            while (true) {
                // On attend de recevoir un objet envoyé par ton client (UI)
                Object requete = in.readObject();
                System.out.println("📥 [SERVEUR SIMULATION] Reçu du client : " + requete.toString());

                /* * ICI : SIMULATION DES RÉPONSES
                 * Si tu utilises une classe Packet, tu devras adapter ce code.
                 * Par exemple :
                 * Packet reponse = new Packet();
                 * reponse.setType("REGISTER_OK");
                 * out.writeObject(reponse);
                 * out.flush();
                 */

                // Pour l'instant, on se contente de renvoyer un simple accusé de réception
                out.writeObject("Message reçu par la simulation !");
                out.flush();
            }

        } catch (Exception e) {
            System.out.println("🔌 [SERVEUR SIMULATION] Le client s'est déconnecté.");
        }
    }
}
