/*package client;

import network.*;
import model.*;

import java.io.*;
import java.net.*;

public class ClientReseauCALL {

    private Socket tuyau;
    private PrintWriter stylo;
    private BufferedReader yeux;

    private boolean connecte = false;
    private boolean modeSimulation = false;

    private EcouteurClient ecouteur;
    private Utilisateur moi;

    public ClientReseauCALL(EcouteurClient ecouteur) {
        this.ecouteur = ecouteur;
    }

    // ===== MODE SIMULATION =====
    public void activerSimulation() {
        this.modeSimulation = true;
        this.connecte = true;
        System.out.println("[SIMULATION] Mode simulation activé");
    }

    // ===== CONNEXION NORMALE =====
    public void connecterAuServeur(String ip, int port) {
        try {
            System.out.println("Connexion au serveur " + ip + ":" + port + " ...");

            tuyau = new Socket(ip, port);
            stylo = new PrintWriter(tuyau.getOutputStream(), true);
            yeux = new BufferedReader(new InputStreamReader(tuyau.getInputStream()));

            connecte = true;
            modeSimulation = false;

            System.out.println("✅ Connecté au serveur !");

            Thread ami = new Thread(new Ecouteur());
            ami.start();

        } catch (IOException e) {
            System.out.println("❌ Erreur connexion : " + e.getMessage());
            connecte = false;

            if (ecouteur != null) {
                ecouteur.erreur("Impossible de se connecter au serveur");
            }
        }
    }

    // ===== ENVOYER =====
    public void envoyer(Packet packet) {

        if (!connecte) {
            System.out.println("❌ Pas connecté !");
            return;
        }
        if (moi != null) {
            packet.setExpediteurId(moi.getIdUtilisateur());
        }


        if (stylo != null) {
            stylo.println(packet.toString());
            System.out.println("📤 Envoyé : " + packet.getCommande());
        }
    }

    // ===== SIMULATION =====
    private void simulerReponse(Packet envoye) {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}

        Packet reponse = null;

        switch (envoye.getCommande()) {

            case INSCRIPTION:
                reponse = new Packet(Commande.SUCCES, "99|nouveauuser|Nom|Prenom");
                System.out.println("📥 [SIMULATION] Réponse : SUCCES");
                break;

            case CONNEXION:
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
                String data = (String) p.getData();
                String[] infos = data.split("\\|");

                moi = new Utilisateur();
                moi.setIdUtilisateur(Integer.parseInt(infos[0]));
                moi.setNomComplet(infos[1]);
                moi.setNumeroTelephone(infos[2]);
                moi.setStatus(true);

                System.out.println("✅ [SIMULATION] Authentifié ! ID = " + moi.getIdUtilisateur());

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

        try {
            if (tuyau != null) {
                tuyau.close();
            }
        } catch (IOException e) {}

        if (ecouteur != null) {
            ecouteur.deconnexion();
        }
    }

    // ===== GETTERS =====
    public boolean isConnecte() {
        return connecte;
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

                // =============================
                // AUTH SUCCES
                // =============================
                case SUCCES:
                    String data = (String) p.getData();
                    String[] infos = data.split("\\|");

                    moi = new Utilisateur();
                    moi.setId(Integer.parseInt(infos[0]));
                    moi.setNom(infos[1]);
                    moi.setPrenom(infos[2]);
                    moi.setEnLigne(true);

                    System.out.println("✅ Authentifié ! ID = " + moi.getIdUtilisateur());

                    if (ecouteur != null) {
                        ecouteur.connexionReussie(moi);
                    }
                    break;

                // =============================
                // ERREUR
                // =============================
                case ERREUR:
                    System.out.println("❌ Refusé : " + p.getData());

                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;

                // =============================
                // MESSAGE CHAT
                // =============================
                case RECEPTION_MESSAGE:
                    if (ecouteur != null) {
                        ecouteur.messageRecu((String) p.getData());
                    }
                    break;

                // =============================
                // AUDIO DATA (BYTES)
                // =============================
                case Data_AUDIO:
                    byte[] audio = (byte[]) p.getData();

                    System.out.println("[AUDIO] Données reçues : " + audio.length + " octets");

                    if (ecouteur != null) {
                        ecouteur.audioRecu(audio);
                    }
                    break;

                // =============================
                // AUDIO START
                // =============================
                case Debuter_AUDIO_CALL:
                    System.out.println("[AUDIO] Appel entrant : " + p.getData());

                    if (ecouteur != null) {
                        ecouteur.audioCallStart((String) p.getData());
                    }
                    break;

                // =============================
                // AUDIO STOP
                // =============================
                case Arreter_AUDIO_CALL:
                    System.out.println("[AUDIO] Appel terminé : " + p.getData());

                    if (ecouteur != null) {
                        ecouteur.audioCallStop((String) p.getData());
                    }
                    break;
                case Data_Appel_Video: {
                    byte[] frame = (byte[]) p.getData();
                    if (ecouteur != null) {
                        ecouteur.videoRecu(frame);
                    }
                    break;
                }
                    case Debuter_Video_CALL:{
                        System.out.println("[VIDEO] Appel vidéo entrant : " + p.getData());
                        break;
                }

                case Arreter_Video_CALL: {
                    System.out.println("[VIDEO] Appel vidéo terminé : " + p.getData());
                    break;
                }
                default:
                    System.out.println("⚠️ Commande non gérée : " + p.getCommande());
                    break;
            }

        }
    }
}