package client;

import model.Message;
import model.Utilisateur;
import model.Contact;
import model.Conversation;
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

}
