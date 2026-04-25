package service;

import Serveur.*;
import client.ClientReseau;
import network.*;

public class AuthService {
    private ClientReseau client;

    // Constructeur : on reçoit le client réseau déjà connecté
    public AuthService(ClientReseau client) {
        this.client = client;
    }

    // ===== INSCRIPTION =====
    public String inscrire(String nomComplet, String numero, String password) {

        if (nomComplet.isEmpty() || numero.isEmpty() || password.isEmpty()) {
            return "Tous les champs sont obligatoires !"; // Retourne l'erreur à l'UI
        }

        if (numero.length() != 10) {
            return "Le numéro doit faire 10 chiffres !"; // Retourne l'erreur à l'UI
        }

        String data = nomComplet+ "|" + numero + "|" + password;
        Packet p = new Packet(Protocol.REGISTER, data);
        client.envoyer(p);

        return "OK"; // Indique que la demande est bien partie
    }

    // ===== CONNEXION =====
    public String connecter(String numero, String password) {


        String data = numero + "|" + password;
        Packet p = new Packet(Protocol.LOGIN, data);
        client.envoyer(p);

        return "OK";
    }
    // ===== DÉCONNEXION =====
    public void deconnecter() {
        Packet p = new Packet(Protocol.LOGOUT, "");
        client.envoyer(p);
        client.deconnecter();
    }
}