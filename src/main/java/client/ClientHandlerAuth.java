package client;

import model.Utilisateur;
import service.AuthService;


public class ClientHandlerAuth {

    // ===== SINGLETON : une seule instance =====
    private static ClientHandlerAuth instance;

    private ClientReseau clientReseau;
    private AuthService authService;
    private boolean connecteAuServeur = false;

    // Constructeur privé (personne ne peut créer directement)
    private ClientHandlerAuth() {}

    // Obtenir l'instance unique
    public static ClientHandlerAuth getInstance() {
        if (instance == null) {
            instance = new ClientHandlerAuth();
        }
        return instance;
    }

    // ===== 1. CONNEXION AU SERVEUR =====
    public boolean connecterAuServeur(String ip, int port, EcouteurClient ecouteur) {

        // Mode simulation pour tester sans vrai serveur
        if (ip.equalsIgnoreCase("simul")) {
            clientReseau = new ClientReseau(ecouteur);
            clientReseau.activerSimulation();
            authService = new AuthService(clientReseau);
            connecteAuServeur = true;
            return true;
        }

        // Connexion normale
        clientReseau = new ClientReseau(ecouteur);
        clientReseau.connecterAuServeur(ip, port);

        if (clientReseau.isConnecte()) {
            authService = new AuthService(clientReseau);
            connecteAuServeur = true;
            return true;
        }

        return false;
    }

    // ===== 2. AUTHENTIFICATION =====
    public String seConnecter(String numero, String password) {
        if (!verifierConnexion()) {
            return "Erreur : Pas connecté au serveur !";
        }
        return authService.connecter(numero, password);
    }
    // ===== 3. INSCRIPTION =====
    public String sInscrire(String nom, String prenom, String numero, String password) {
        if (!verifierConnexion()) {
            return "Erreur : Pas connecté au serveur !";
        }
        return authService.inscrire(nom, prenom, numero, password);
    }

    // ===== 3. DÉCONNEXION =====
    public void seDeconnecter() {
        if (authService != null) {
            authService.deconnecter();
        }
        connecteAuServeur = false;
    }

    // ===== 4. INFOS =====
    public boolean isConnecteAuServeur() {
        return connecteAuServeur;
    }

    public Utilisateur getUtilisateurConnecte() {
        if (clientReseau != null) {
            return clientReseau.getMoi();
        }
        return null;
    }

    // ===== VÉRIFICATION =====
    private boolean verifierConnexion() {
        if (!connecteAuServeur || authService == null) {
            System.out.println(" Pas connecté au serveur !");
            return false;
        }
        return true;
    }
}