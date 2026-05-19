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