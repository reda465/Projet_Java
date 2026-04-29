package client;

import model.Conversation;
import model.Utilisateur;
import java.util.ArrayList;
import java.util.List;
public interface EcouteurClient {
    // Quand la connexion réussit
    void connexionReussie(Utilisateur moi);
    void inscriptionReussie(String msg);
    // Quand ça échoue ou autre erreur
    void erreur(String message);
    // Quand on reçoit un message
    void messageRecu(String num ,String contenu);
    // Quand on se déconnecte
    void deconnexion();
    void conversationRecues(List<Conversation>conversations);

    void conversationsRecues(List<Conversation> conversations);
}
