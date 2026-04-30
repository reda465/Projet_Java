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
    private String numeroCorrespondant;

    // (Ports retirés d'ici pour être attribués dynamiquement selon le rôle appelant/appelé)

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

        envoyer(Protocol.CALL_REQUEST, numeroDest + "|" + type);
        System.out.println("[APPEL] Demande envoyée à " + numeroDest);
    }

    // On m'appelle (reçu du serveur)
    public void recevoirAppel(String numAppelant, String nomAppelant, String typeAppel, String ipAppelant){
        if (appelEnCours != null) {
            envoyer(Protocol.CALL_REFUSE, numAppelant);
            return;
        }

        appelEnCours = new Appel();
        appelEnCours.setTypeAppel(TypeAppel.valueOf(typeAppel));
        appelEnCours.setStatut(StatutAppel.en_cours);
        this.numeroCorrespondant = numAppelant;
        this.ipCorrespondant = ipAppelant;

        System.out.println("[APPEL] Entrant de " + numAppelant + " (" + ipAppelant + ")");
    }

    public void accepte(){
        if (appelEnCours == null) return;

        appelEnCours.setStatut(StatutAppel.accepte);
        communicationActive = true;

        envoyer(Protocol.CALL_ACCEPT, numeroCorrespondant); // celui qui a appelé
        
        // L'appelé écoute sur le port 5002 et envoie vers le port 5001
        audioUDP.demarrer(ipCorrespondant, 5001, 5002);

        System.out.println("[APPEL] Accepté, UDP démarré");
    }

    // L'autre a accepté mon appel (Je suis l'appelant)
    public void onAccepte(String ipAccepteur) {
        if (appelEnCours == null) return;

        this.ipCorrespondant = ipAccepteur;
        appelEnCours.setStatut(StatutAppel.accepte);
        communicationActive = true;

        // L'appelant écoute sur le port 5001 et envoie vers le port 5002
        audioUDP.demarrer(ipAccepteur, 5002, 5001);
        System.out.println("[APPEL] UDP démarré vers " + ipAccepteur);
    }

    // Refuser / Raccrocher / Terminer
    public void refuser() {
        if (appelEnCours == null) return;
        envoyer(Protocol.CALL_REFUSE, numeroCorrespondant);
        arreter();
    }

    public void raccrocher() {
        if (appelEnCours == null) return;
        envoyer(Protocol.CALL_END, numeroCorrespondant);
        arreter();
    }
    // ========== L'AUTRE A RACCROCHÉ ==========
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
        numeroCorrespondant = null;
    }

    public boolean isEnAppel() { return appelEnCours != null; }
    public boolean isCommunicationActive() { return communicationActive; }
    public boolean isAppelEntrant() {
        return appelEnCours != null && !communicationActive && ipCorrespondant != null;
    }
}
