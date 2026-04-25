package service;

import client.ClientReseau;
import network.*;

public class AuthService {
    private ClientReseau client;

    // Constructeur : on reçoit le client réseau déjà connecté
    public AuthService(ClientReseau client) {
        this.client = client;
    }

    // ===== INSCRIPTION =====
    public String inscrire(String nom, String prenom, String numero, String password) {

        if (nom.isEmpty() || prenom.isEmpty() || numero.isEmpty() || password.isEmpty()) {
            return "Tous les champs sont obligatoires !"; // Retourne l'erreur à l'UI
        }

        if (numero.length() != 10) {
            return "Le numéro doit faire 10 chiffres !"; // Retourne l'erreur à l'UI
        }

        String data = nom + "|" + prenom + "|" + numero + "|" + password;
        Packet p = new Packet(Commande.INSCRIPTION, data);
        client.envoyer(p);

        return "OK"; // Indique que la demande est bien partie
    }

    // ===== CONNEXION =====
    public String connecter(String numero, String password) {

        if (numero.isEmpty() || password.isEmpty()) {
            return "Remplis tous les champs !";
        }

        String data = numero + "|" + password;
        Packet p = new Packet(Commande.CONNEXION, data);
        client.envoyer(p);

        return "OK";
    }
    // ===== DÉCONNEXION =====
    public void deconnecter() {
        Packet p = new Packet(Commande.DECONNEXION, "");
        client.envoyer(p);
        client.deconnecter();
    }
}