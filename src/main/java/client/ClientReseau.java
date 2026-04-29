
package client;

import lombok.Getter;
import lombok.Setter;
import network.*;
import model.*;
import service.MessageService;

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
    private MessageService messageService;


    public ClientReseau(EcouteurClient ecouteur){//lier a l'interface graphique pour les signales
        this.ecouteur = ecouteur;
    }
    public MessageService getMessageService() {
        if (messageService == null) {
            messageService = new MessageService(this);
        }
        return messageService;
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
        if (stylo != null) {
            stylo.println(packet.toString());
            System.out.println(" Envoyé : " + packet.getProtocol());
        }
    }

    // ===== DÉCONNEXION =====
    public void deconnecter() {
        connecte = false;
        try{
        if (tuyau != null) { tuyau.close();}
        } catch (IOException e) {}
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
            String data = p.getData();
            String[] parts = data.split("\\|");
            switch (p.getProtocol()) {
                case LOGIN_OK:
                    if (parts.length >= 2) {
                        moi = new Utilisateur();
                        moi.setNomComplet(parts[0]);
                        moi.setNumeroTelephone(parts[1]);
                    if (ecouteur != null) {
                        ecouteur.connexionReussie(moi);
                    }
                    System.out.println("Connecté : " + moi.getNomComplet());
                    }
                    break;

                case LOGIN_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;

                case REGISTER_OK:
                    if (parts.length >= 2) {
                        moi = new Utilisateur();
                        moi.setNomComplet(parts[0]);
                        moi.setNumeroTelephone(parts[1]);
                        System.out.println("Inscrit : " + moi.getNomComplet());
                        if (ecouteur != null) ecouteur.inscriptionReussie(moi.getNomComplet());
                    }
                    break;
                case REGISTER_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;
                case MSG_RECEIVE:
                    String monTel;
                    if (parts.length >= 2) {
                        Message msg = new Message();
                        msg.setTelephoneExpediteur(parts[0]);
                        if( moi != null){ monTel = moi.getNumeroTelephone(); }
                        else { monTel = "moi"; }
                        msg.setTelephoneDestinataire(monTel);////////
                        msg.setContenuTexte(parts[1]);
                        System.out.println("Message de " + msg.getTelephoneExpediteur() + " : " + msg.getContenuTexte());
                        if (ecouteur != null) ecouteur.messageRecu(msg.getContenuTexte());
                    }
                    break;
                case MSG_SEND: {
                    String[] msgParts = data.split("\\|");
                    if (msgParts.length < 3) {
                        p.setData("MSG_FAIL|Format invalide");
                        envoyer(p);
                        return;
                    }
                    String numExp = msgParts[0];
                    String numDest = msgParts[1];
                    String contenu = msgParts[2];
                    p.setData(numExp +  "|" + numDest + "|" + contenu);
                    envoyer(p);
                    System.out.println("  → Message relayé de " + numExp + " vers " + numDest);
                    break;
                }
                case CALL_REQUEST:
                    if (parts.length >= 2 && ecouteur != null) {
                        ecouteur.appelEntrant(parts[0], parts[1]);
                    }
                    break;
                case CALL_ACCEPT:
                    if (ecouteur != null) ecouteur.appelAccepte(parts[0]);
                    break;

                case CALL_END:
                    if (ecouteur != null) ecouteur.appelTermine(parts[0]);
                    break;
                default:
                    System.out.println("Protocole inconnu : " + p.getProtocol());
                    break;

            }
        }
    }
}