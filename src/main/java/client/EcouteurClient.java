package client;

import model.*;

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
    void listeContactsRecue(List<Contact> contacts);

    // Quand on se déconnecte
    void deconnexion();
    void appelEntrant(String numero,String type, String ipAppelant, String ip);
    void appelAccepte(String numero, String ip);
     void appelRefuse();
     void appelTermine(String numero);

    // ===== GROUPES (NOUVEAU) =====
    void groupeCree(Groupe groupe);                    // CREATE_GROUP_OK
    void creationGroupeEchouee(String raison);          // CREATE_GROUP_FAIL
    void listeGroupesRecue(List<Groupe> groupes);       // GROUPS_LIST
    void membresGroupeRecus(int idGroupe, List<Utilisateur> membres); // GROUP_MEMBERS_LIST
    void messageGroupeRecu(MessageGroupe message);      // GROUP_MSG_RECEIVE
    void membreAjoute(int idGroupe, String numero);     // ADD_MEMBER_OK
    void membreRetire(int idGroupe, String numero);     // REMOVE_MEMBER_OK
    void aQuitteGroupe(int idGroupe);                   // LEAVE_GROUP_OK
    void groupeSupprime(int idGroupe);                  // DELETE_GROUP_OK
    void nomGroupeModifie(int idGroupe, String nouveauNom);
}
