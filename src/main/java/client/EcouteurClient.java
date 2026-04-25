package client;
import
public interface EcouteurClient {

    // Quand la connexion réussit
    void connexionReussie(Utilisateur moi);

    // Quand ça échoue ou autre erreur
    void erreur(String message);

    // Quand on reçoit un message
    void messageRecu(String contenu);

    // Quand on se déconnecte
    void deconnexion();
}
