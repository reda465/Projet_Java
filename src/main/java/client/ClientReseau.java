
package client;

import lombok.Getter;
import lombok.Setter;
import network.*;
import model.*;
import java.io.*;
import java.net.*;
//import Serveur.*;
@Setter
@Getter
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
            System.out.println("Connexion au serveur " + ip + ":" + port);
            connecte = true;
            System.out.println("✅ Connecté au serveur");
            Thread ami = new Thread(new Ecouteur());
            ami.start();

        } catch (IOException e) {
            connecte = false;
            System.out.println(" Erreur connexion : " + e.getMessage());


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
        if (moi != null) {
            packet.setExpediteurId(moi.getIdUtilisateur());
        }

        if (stylo != null) {
            stylo.println(packet.toString());
            System.out.println(" Envoyé : " + packet.getProtocol());
        }
    }

    // ===== DÉCONNEXION =====
    public void deconnecter() {
        connecte = false;
        try{
        if (tuyau != null) {
            tuyau.close();
        } } catch (IOException e) {}

        if (ecouteur != null) {
            ecouteur.deconnexion();
        }
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
            switch (p.getProtocol()) {
                case LOGIN_OK:
                    String[] infos = ((String) p.getData()).split("\\|");
                    moi = new Utilisateur();
                    moi.setNomComplet(infos[0]);
                    moi.setNumeroTelephone(infos[1]);
                    if (ecouteur != null) {
                        ecouteur.connexionReussie(moi);
                    }
                    break;

                case LOGIN_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;
                case REGISTER_OK:
                    if (ecouteur != null) {
                        ecouteur.inscriptionReussie((String) p.getData());
                    }
                    break;
                case REGISTER_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;
                case MSG_RECEIVE:
                    if (ecouteur != null) {
                        ecouteur.messageRecu((String) p.getData());
                    }
                    break;
                /*case VIDEO_FRAME:
                    if (ecouteur != null) {
                        ecouteur.videoRecu((byte[]) p.getData());
                    }
                    break;*/
                case CALL_REQUEST:
                    System.out.println("[CALL] Demande appel reçue : " + p.getData());
                    // tu peux appeler une méthode dans l'interface pour afficher popup
                    break;

                case CALL_ACCEPT:
                    System.out.println("[CALL] Appel accepté : " + p.getData());
                    break;

                case CALL_REFUSE:
                    System.out.println("[CALL] Appel refusé : " + p.getData());
                    break;

                case CALL_END:
                    System.out.println("[CALL] Appel terminé : " + p.getData());
                    break;
                case Call_AUDIO_DATA:
                    byte[] audio = (byte[]) p.getData();
                    System.out.println("[AUDIO] Données reçues : " + audio.length + " octets");
                    if (ecouteur != null) {
                        ecouteur.audioRecu(audio);
                    }
                    break;
                case Call_VIDEO_DATA:
                    byte[] video = (byte[]) p.getData();
                    System.out.println("[VIDEO] Données reçues : " + video.length + " octets");
                    if (ecouteur != null) {
                        ecouteur.videoRecu(video);
                    }
                    break;

                default:
                    System.out.println("⚠️ Packet non géré : " + p.getProtocol());
                    break;

            }
        }
    }
}