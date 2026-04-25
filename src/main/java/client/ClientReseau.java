
package client;

import network.*;
import model.*;
import java.io.*;
import java.net.*;
import Serveur.*;

public class ClientReseau {

    private Socket tuyau;
    private PrintWriter stylo;
    private BufferedReader yeux;
    private boolean connecte = false;

    private EcouteurClient ecouteur;
    private Utilisateur moi;

    public ClientReseau(EcouteurClient ecouteur){//lier a l'interface graphique pour les signales
        this.ecouteur = ecouteur;
    }
    // ===== CONNEXION NORMALE =====
    public void connecterAuServeur(String ip, int port) {
        try {
            tuyau = new Socket(ip, port);
            stylo = new PrintWriter(tuyau.getOutputStream(), true);
            yeux = new BufferedReader(new InputStreamReader(tuyau.getInputStream()));

            connecte = true;
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


    // ===== ENVOYER =====
    public void envoyer(Packet packet) {
        if (!connecte) {//erreur du connection
            return;
        }

        if (stylo != null) {
            stylo.println(packet.toString());
            System.out.println(" Envoyé : " + packet.getCommande());
        }
    }

    // ===== DÉCONNEXION =====
    public void deconnecter() {
        connecte = false;

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
                    Packet recu = Packet.fromString(ligne);
                    traiterPacket(recu);
                }
            } catch (IOException e) {
                connecte = false;
                if (ecouteur != null) {
                    ecouteur.deconnexion();
                }
            }
        }

        private void traiterPacket(Packet p) {
            switch (p.getCommande()) {
                case LOGIN_OK:
                    String[] infos = p.getData().split("\\|");
                    moi = new Utilisateur();
                    moi.setNomComplet(infos[0]);
                    moi.setNumeroTelephone(infos[1]);
                    if (ecouteur != null) {
                        ecouteur.connexionReussie(moi);
                    }
                    break;

                case LOGIN_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur(p.getData());
                    }
                    break;
                case REGISTER_OK:
                    if (ecouteur != null) {
                        ecouteur.inscriptionReussie(p.getData());
                    }
                    break;
                case REGISTER_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur(p.getData());
                    }
                    break;

            }
        }
    }
}