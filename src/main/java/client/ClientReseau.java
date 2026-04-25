package client;

import network.*;
import model.*;
import java.io.*;
import java.net.*;

public class ClientReseau {

    private Socket tuyau;
    private PrintWriter stylo;
    private BufferedReader yeux;
    private boolean connecte = false;
    private boolean modeSimulation = false;

    private EcouteurClient ecouteur;
    private Utilisateur moi;

    public ClientReseau(EcouteurClient ecouteur){//lier a l'interface graphique pour les signales
        this.ecouteur = ecouteur;
    }
    // ===== CONNEXION NORMALE =====
    public void connecterAuServeur(String ip, int port) {
        try {
            System.out.println("Connexion au serveur" + ip + ":" + port + "...");
            tuyau = new Socket(ip, port);
            stylo = new PrintWriter(tuyau.getOutputStream(), true);
            yeux = new BufferedReader(new InputStreamReader(tuyau.getInputStream()));

            connecte = true;
            modeSimulation = false;
            System.out.println(" Connecté au serveur !");

            Thread ami = new Thread(new Ecouteur());
            ami.start();

        } catch (IOException e) {
            System.out.println(" Erreur connexion : " + e.getMessage());
            connecte = false;

            if (ecouteur != null) {//ecouteur est null signifie que y'a pas de liaison entre UI et Client
                ecouteur.erreur("Impossible de se connecter au serveur");
            }
        }
    }

    // ===== MODE SIMULATION (pour tester sans serveur) =====
    public void activerSimulation() {
        this.modeSimulation = true;
        this.connecte = true;  // Forcer à true !
        System.out.println("🎮 Mode simulation activé");
    }

    // ===== ENVOYER =====
    public void envoyer(Packet packet) {
        if (!connecte) {//erreur du connection
            System.out.println("❌ Pas connecté !");
            return;
        }

        if (modeSimulation) {
            // En simulation, on simule une réponse immédiate
            System.out.println("📤 [SIMULATION] Envoyé : " + packet.getCommande());
            simulerReponse(packet);
            return;
        }

        // Vrai réseau
        if (stylo != null) {
            stylo.println(packet.toString());
            System.out.println("📤 Envoyé : " + packet.getCommande());
        }
    }

    // ===== SIMULER UNE RÉPONSE DU SERVEUR =====
    private void simulerReponse(Packet envoye) {
        // Attendre un peu pour simuler le réseau
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}

        Packet reponse = null;

        switch (envoye.getCommande()) {
            case INSCRIPTION:
                // Simuler : inscription réussie
                reponse = new Packet(Commande.SUCCES, "99|nouveauuser|Nom|Prénom");
                System.out.println("📥 [SIMULATION] Réponse : SUCCES");
                break;

            case CONNEXION:
                // Simuler : connexion réussie
                reponse = new Packet(Commande.SUCCES, "1|onk|nom|mer");
                System.out.println("📥 [SIMULATION] Réponse : SUCCES");
                break;

            case DECONNEXION:
                reponse = new Packet(Commande.SUCCES, "deconnecte");
                break;
        }

        if (reponse != null && ecouteur != null) {
            traiterPacketSimulation(reponse);
        }
    }

    private void traiterPacketSimulation(Packet p) {
        switch (p.getCommande()) {
            case SUCCES:
                String[] infos = p.getData().split("\\|");
                moi = new Utilisateur();
                moi.setId(Integer.parseInt(infos[0]));
               // moi.setUsername(infos[1]);
                moi.setNom(infos[1]);
                moi.setPrenom(infos[2]);
                moi.setEnLigne(true);

                System.out.println("✅ Authentifié ! ID = " + moi.getId());
                if (ecouteur != null) {
                    ecouteur.connexionReussie(moi);
                }
                break;
        }
    }

    // ===== DÉCONNEXION =====
    public void deconnecter() {
        connecte = false;
        modeSimulation = false;

        if (tuyau != null) {
            try {
                tuyau.close();
            } catch (IOException e) {}
        }

        if (ecouteur != null) {
            ecouteur.deconnexion();
        }
    }

    // ===== GETTERS =====
    public boolean isConnecte() {
        return connecte;  // Maintenant true en simulation !
    }

    public Utilisateur getMoi() {
        return moi;
    }

    public void setMoi(Utilisateur moi) {
        this.moi = moi;
    }

    // ===== CLASSE INTERNE : ÉCOUTEUR RÉSEAU =====
    private class Ecouteur implements Runnable {

        public void run() {
            try {
                String ligne;
                while (connecte && (ligne = yeux.readLine()) != null) {
                    System.out.println("📥 Reçu : " + ligne);
                    Packet recu = Packet.fromString(ligne);
                    traiterPacket(recu);
                }
            } catch (IOException e) {
                System.out.println("🔌 Connexion perdue.");
            }

            connecte = false;
            if (ecouteur != null) {
                ecouteur.deconnexion();
            }
        }

        private void traiterPacket(Packet p) {
            switch (p.getCommande()) {
                case SUCCES:
                    String[] infos = p.getData().split("\\|");
                    moi = new Utilisateur();
                    moi.setId(Integer.parseInt(infos[0]));
                   // moi.setUsername(infos[1]);
                    moi.setNom(infos[1]);
                    moi.setPrenom(infos[2]);
                    moi.setEnLigne(true);

                    System.out.println("✅ Authentifié ! ID = " + moi.getId());
                    if (ecouteur != null) {
                        ecouteur.connexionReussie(moi);
                    }
                    break;

                case ERREUR:
                    System.out.println("❌ Refusé : " + p.getData());
                    if (ecouteur != null) {
                        ecouteur.erreur(p.getData());
                    }
                    break;

                case RECEPTION_MESSAGE:
                    if (ecouteur != null) {
                        ecouteur.messageRecu(p.getData());
                    }
                    break;
            }
        }
    }
}