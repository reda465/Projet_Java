package client;

import model.*;

import javafx.scene.image.ImageView;
import java.util.List;
public interface EcouteurClient {
    // Quand la connexion réussit
    void connexionReussie(Utilisateur moi);
    void inscriptionReussie(String msg);
    // Quand ça échoue ou autre erreur
    void erreur(String message);
    // Quand on reçoit un message
    void messageRecu(String numeroDest, String message);
    void conversationsRecues(List<Conversation> conversations);
    void messagesRecus(List<Message> messages); // Pour une conversation donnée
    //Contact
    void contactAjoute(Contact contact);           // Quand un contact est ajouté
    /** Demande entrante (autre utilisateur souhaite vous ajouter). */
    default void demandeContactRecue(String numeroDemandeur, String nomDemandeur) {}
    /** Vous avez accepté une demande : rafraîchir les conversations / contacts. */
    default void contactAcceptationConfirmee() {}
    void listeContactsRecue(List<Contact> contacts);

    // Quand on se déconnecte
    void deconnexion();

    // (Methode fluxVideoGroupeRecu deplacee plus bas)
    void appelEntrant(String numero, String type, String ipAppelant, String name);
    void appelAccepte(String numero, String ip);
     void appelRefuse();
     void appelTermine(String numero);

    // ===== GROUPES (NOUVEAU) =====
    void groupeCree(Groupe groupe);                    // CREATE_GROUP_OK
    void creationGroupeEchouee(String raison);          // CREATE_GROUP_FAIL
    void listeGroupesRecue(List<Groupe> groupes);       // GROUPS_LIST
    default void membresGroupeRecus(int idGroupe, List<Utilisateur> membres) {}
    void messageGroupeRecu(MessageGroupe message);      // GROUP_MSG_RECEIVE
    void membreAjoute(int idGroupe, String numero);     // ADD_MEMBER_OK
    void membreRetire(int idGroupe, String numero);     // REMOVE_MEMBER_OK
    void aQuitteGroupe(int idGroupe);                   // LEAVE_GROUP_OK
    void groupeSupprime(int idGroupe);                  // DELETE_GROUP_OK
    void nomGroupeModifie(int idGroupe, String nouveauNom);

    //fichier
    void fichierRecu(String telephoneExp, String fileName, String base64);
    void appelGroupeEntrant(int idGroupe, String nomGroupe, String type, String initiateurNom);
    void appelGroupeDemarre(int idGroupe, String type);
    void membreRejointAppelGroupe(int idGroupe, String numeroMembre, String nomMembre, String ip, String type, int port, int portAudio, boolean isReply);
    void membreQuitteAppelGroupe(int idGroupe, String numeroMembre);
    void appelGroupeTermine(int idGroupe);
    void signalisationAppelGroupe(int idGroupe, String numeroSource, String typeSignal, String payload);
    void fluxVideoGroupeRecu(int idGroupe, String numeroExpediteur, ImageView videoNode);
    void fluxVideoGroupeArrete(int idGroupe, String numeroExpediteur);
    //pour les ppels audio et video

}
