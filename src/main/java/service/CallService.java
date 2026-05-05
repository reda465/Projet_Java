package service;

import Serveur.Protocol;
import client.AudioUDP;
import client.VideoUDP;
import javafx.scene.image.ImageView;
import client.ClientReseau;
import lombok.Getter;
import lombok.Setter;
import model.Appel;
import model.Utilisateur;
import model.enums.StatutAppel;
import model.enums.TypeAppel;
import network.Packet;

import java.time.LocalDateTime;
@Getter
@Setter

public class CallService {
    private final ClientReseau clientReseau;
    private final Utilisateur localUser;
    private final AudioUDP audioUDP;
    private final VideoUDP videoUDP;

    private Appel appelEnCours;
    private boolean communicationActive = false;
    private String ipCorrespondant;
    private String numeroCorrespondant;

    // JavaFX ImageView pour afficher la vidéo reçue
    private ImageView videoView;
    // Si la UI n'est pas encore prête, on garde une demande de démarrage en attente
    private boolean videoStartPending = false;
    private String pendingIp = null;
    private int pendingRemotePort = -1;
    private int pendingLocalPort = -1;

    private static final int PORT_AUDIO_A  = 5001;
    private static final int PORT_AUDIO_B  = 5002;
    private static final int PORT_VIDEO_A  = 5003;
    private static final int PORT_VIDEO_B  = 5004;

    public CallService(ClientReseau clientReseau, Utilisateur localUser) {
        this.clientReseau = clientReseau;
        this.localUser = localUser;
        this.audioUDP = new AudioUDP();
        this.videoUDP = new VideoUDP();
    }

    public void setVideoView(ImageView view) {
        this.videoView = view;
        // Si une négociation/acceptation a déjà eu lieu, démarrer dès que la vue existe
        if (videoView != null && videoStartPending && pendingIp != null) {
            videoStartPending = false;
            demarrerVideoSiPossible(pendingIp, pendingRemotePort, pendingLocalPort);
            pendingIp = null;
            pendingRemotePort = -1;
            pendingLocalPort = -1;
        }
    }

    private void demarrerVideoSiPossible(String ip, int portDistant, int monPort) {
        if (appelEnCours == null || appelEnCours.getTypeAppel() != TypeAppel.VIDEO) return;
        if (ip == null || ip.isBlank()) return;
        // Eviter double bind si déjà actif
        if (videoUDP != null && videoUDP.isActif()) return;
        if (videoView == null) {
            System.out.println("⚠️ VideoView null : vidéo en attente d'affichage...");
            videoStartPending = true;
            pendingIp = ip;
            pendingRemotePort = portDistant;
            pendingLocalPort = monPort;
            return;
        }
        videoUDP.demarrer(ip, portDistant, monPort, videoView);
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

    // J'accepte l'appel entrant
    public void accepte(){
        if (appelEnCours == null) return;

        appelEnCours.setStatut(StatutAppel.accepte);
        communicationActive = true;

        envoyer(Protocol.CALL_ACCEPT, numeroCorrespondant); // celui qui a appelé
        audioUDP.demarrer(ipCorrespondant, PORT_AUDIO_A, PORT_AUDIO_B);

        // Video seulement si VIDEO
        if (appelEnCours.getTypeAppel() == TypeAppel.VIDEO) {
            // Côté appelé (rôle B): j'écoute sur B et j'envoie vers A
            demarrerVideoSiPossible(ipCorrespondant, PORT_VIDEO_A, PORT_VIDEO_B);
        }

        System.out.println("[APPEL] Accepté, UDP démarré");
    }

    // L'autre a accepté mon appel
    public void onAccepte(String ipAccepteur) {
        if (appelEnCours == null) return;

        this.ipCorrespondant = ipAccepteur;
        appelEnCours.setStatut(StatutAppel.accepte);
        communicationActive = true;

        audioUDP.demarrer(ipAccepteur, PORT_AUDIO_B, PORT_AUDIO_A);
        // Video seulement si VIDEO
        if (appelEnCours.getTypeAppel() == TypeAppel.VIDEO) {
            // Côté appelant (rôle A): j'écoute sur A et j'envoie vers B
            demarrerVideoSiPossible(ipAccepteur, PORT_VIDEO_B, PORT_VIDEO_A);
        }


        System.out.println("[APPEL] UDP démarré vers " + ipAccepteur);
    }

    // Refuser
    public void refuser() {
        if (appelEnCours == null) return;
        envoyer(Protocol.CALL_REFUSE, numeroCorrespondant);
        arreter();
    }
 //  Je Raccrocher
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
  //arreter l'audio et vidau
    private void arreter() {
        audioUDP.arreter();
        videoUDP.arreter();
        appelEnCours = null;
        communicationActive = false;
        ipCorrespondant = null;
        numeroCorrespondant = null;
        videoView = null;
        videoStartPending = false;
        pendingIp = null;
        pendingRemotePort = -1;
        pendingLocalPort = -1;
    }

    public boolean isEnAppel() { return appelEnCours != null; }
    public boolean isCommunicationActive() { return communicationActive; }
    public boolean isAppelEntrant() {
        return appelEnCours != null && !communicationActive && ipCorrespondant != null;
    }
}
