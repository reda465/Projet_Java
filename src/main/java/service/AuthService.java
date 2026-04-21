package service;

import client.ClientReseau;
import model.Utilisateur;
import network.*;

public class AuthService {
    private ClientReseau client;

    // Constructeur : on reçoit le client réseau déjà connecté
    public AuthService(ClientReseau client) {
        this.client = client;
    }

    // ===== INSCRIPTION =====
    // Remplir le formulaire et l'envoyer
    public void inscrire(String nom, String prenom, String numero, String password) {

        // 1. Vérifier que tout est rempli
        if (nom.isEmpty() || prenom.isEmpty() || numero.isEmpty() || password.isEmpty()) {
            System.out.println(" Tous les champs sont obligatoires !");
            return;
        }

        // 2. Vérifier le numéro (10 chiffres par exemple)
        if (numero.length() != 10) {
            System.out.println(" Le numéro doit faire 10 chiffres !");
            return;
        }

        // 3. Préparer les données
        // Format : "nom|prenom|numero|password"
        String data = nom + "|" + prenom + "|" + numero + "|" + password;

        // 4. Créer le colis
        Packet p = new Packet(Commande.INSCRIPTION, data);

        // 5. Envoyer
        client.envoyer(p);
        System.out.println(" Inscription envoyée pour " +nom + prenom);
    }

    // ===== CONNEXION =====
    public void connecter(String numero, String password) {

        // 1. Vérifier
        if (numero.isEmpty() || password.isEmpty()) {
            System.out.println(" Remplis tous les champs !");
            return;
        }

        // 2. Préparer
        String data = numero + "|" + password;

        // 3. Créer le colis
        Packet p = new Packet(Commande.CONNEXION, data);

        // 4. Envoyer
        client.envoyer(p);
        System.out.println(" Connexion demandee pour " + numero);
    }

    // ===== DÉCONNEXION =====
    public void deconnecter() {
        Packet p = new Packet(Commande.DECONNEXION, "");
        client.envoyer(p);
        client.deconnecter();
    }
}