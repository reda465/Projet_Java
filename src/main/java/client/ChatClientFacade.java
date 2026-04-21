package client;

import model.Utilisateur;
import service.AuthService;


public class ChatClientFacade {

    // ===== SINGLETON : une seule instance =====
    // (comme le président, il n'y en a qu'un)
    private static ChatClientFacade instance;

    private ClientReseau clientReseau;
    private AuthService authService;
    private boolean connecteAuServeur = false;

    // Constructeur privé (personne ne peut créer directement)
    private ChatClientFacade() {}

    // Obtenir l'instance unique
    public static ChatClientFacade getInstance() {
        if (instance == null) {
            instance = new ChatClientFacade();
        }
        return instance;
    }

    // ===== 1. CONNEXION AU SERVEUR =====
    /**
     * L'UI appelle ça au démarrage (bouton "Se connecter au serveur")
     *
     * @param ip Adresse du serveur (ex: "localhost", "192.168.1.5")
     * @param port Port (ex: 8080)
     * @param ecouteur L'UI qui veut être prévenue des événements
     * @return true si connecté, false sinon
     */
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
    /**
     * L'UI appelle ça quand l'utilisateur clique "Se connecter"
     *
     * @param numero Numéro de téléphone
     * @param password Mot de passe
     */
    public void seConnecter(String numero, String password) {
        if (!verifierConnexion()) return;
        authService.connecter(numero, password);
    }

    /**
     * L'UI appelle ça quand l'utilisateur clique "S'inscrire"
     */
    public void sInscrire(String nom, String prenom, String numero, String password) {
        if (!verifierConnexion()) return;
        authService.inscrire(nom, prenom, numero, password);
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