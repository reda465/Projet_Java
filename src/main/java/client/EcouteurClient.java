package client;

import model.Utilisateur;

public interface EcouteurClient {
    // Quand la connexion réussit
    void connexionReussie(Utilisateur moi);
    void inscriptionReussie(String msg);

    // Quand ça échoue ou autre erreur
    void erreur(String message);

    // Quand on reçoit un message
    void messageRecu(String contenu);

    // Quand on se déconnecte
    void deconnexion();
    void appelEntrant(String numero,String type);
    void appelAccepte(String numero);
     void appelRefuse(String numero);
     void appelTermine(String numero);

}
