package service;

import Serveur.Protocol;
import client.AudioUDP;
import client.ClientReseau;
import model.Appel;
import model.Utilisateur;
import model.enums.StatutAppel;
import model.enums.TypeAppel;
import network.Packet;

import java.time.LocalDateTime;

public class CallService {
    private final ClientReseau clientReseau;
    private final Utilisateur localUser;
    private final AudioUDP audioUDP;

    private Appel appelEnCours;
    private boolean communicationActive = false;
    private String ipCorrespondant;

    private static final int PORT_LOCAL = 5001;
    private static final int PORT_DISTANT = 5002;

    public CallService(ClientReseau clientReseau, Utilisateur localUser) {
        this.clientReseau = clientReseau;
        this.localUser = localUser;
        this.audioUDP = new AudioUDP();
    }

    // J'appelle quelqu'un
    public void appeler(String numeroDest, int idConv, TypeAppel type) {
        if (appelEnCours != null) return;

        appelEnCours = new Appel();
        appelEnCours.setIdAppelant(localUser.getIdUtilisateur());
        appelEnCours.setIdConversation(idConv);
        appelEnCours.setTypeAppel(type);
        appelEnCours.setStatut(StatutAppel.en_cours);
        appelEnCours.setDateAppel(LocalDateTime.now());

        envoyer(Protocol.CALL_REQUEST, localUser.getNumeroTelephone() + "|" + numeroDest + "|" + type);
        System.out.println("[APPEL] Demande envoyée à " + numeroDest);
    }

    // On m'appelle (reçu du serveur)
    public void recevoirAppel(String numAppelant, String typeAppel, String ipAppelant) {
        if (appelEnCours != null) {
            envoyer(Protocol.CALL_REFUSE, numAppelant);
            return;
        }

        appelEnCours = new Appel();
        appelEnCours.setTypeAppel(TypeAppel.valueOf(typeAppel));
        appelEnCours.setStatut(StatutAppel.en_cours);
        this.ipCorrespondant = ipAppelant;

        System.out.println("[APPEL] Entrant de " + numAppelant + " (" + ipAppelant + ")");
    }

    // J'accepte l'appel entrant
    public void accepter() {
        if (appelEnCours == null) return;

        appelEnCours.setStatut(StatutAppel.accepte);
        communicationActive = true;

        envoyer(Protocol.CALL_ACCEPT, localUser.getNumeroTelephone());
        audioUDP.demarrer(ipCorrespondant, PORT_DISTANT, PORT_LOCAL);

        System.out.println("[APPEL] Accepté, UDP démarré");
    }

    // L'autre a accepté mon appel
    public void onAccepte(String ipAccepteur) {//////////////////A VERIFIER
        if (appelEnCours == null) return;

        this.ipCorrespondant = ipAccepteur;
        appelEnCours.setStatut(StatutAppel.accepte);
        communicationActive = true;

        audioUDP.demarrer(ipAccepteur, PORT_DISTANT, PORT_LOCAL);
        System.out.println("[APPEL] UDP démarré vers " + ipAccepteur);
    }

    // Refuser / Raccrocher / Terminer
    public void refuser() {
        if (appelEnCours == null) return;
        envoyer(Protocol.CALL_REFUSE, localUser.getNumeroTelephone());
        arreter();
    }

    public void raccrocher() {
        if (appelEnCours == null) return;
        envoyer(Protocol.CALL_END, localUser.getNumeroTelephone());
        arreter();
    }

    public void onTermine() {
        arreter();
        System.out.println("[APPEL] Terminé");
    }

    private void envoyer(Protocol protocol, String data) {
        clientReseau.envoyer(new Packet(protocol, data));
    }

    private void arreter() {
        audioUDP.arreter();
        appelEnCours = null;
        communicationActive = false;
        ipCorrespondant = null;
    }

    public boolean isEnAppel() { return appelEnCours != null; }
    public boolean isCommunicationActive() { return communicationActive; }
}