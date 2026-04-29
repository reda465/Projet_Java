package client;

import Serveur.Protocol;
import lombok.Getter;
import lombok.Setter;
import model.Utilisateur;
import network.Packet;
import service.AuthService;
import service.MessageService;
import service.CallService;
@Getter
@Setter

public class ClientHandlerAuth {

    private static ClientHandlerAuth instance;

    private ClientReseau clientReseau;
    private AuthService authService;
    private MessageService messageService;
    private boolean connecteAuServeur = false;
    private CallService callService;

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

        // Connexion normale
        clientReseau = new ClientReseau(ecouteur);
        clientReseau.connecterAuServeur(ip, port);

        if (clientReseau.isConnecte()) {
            authService = new AuthService(clientReseau);
            messageService = new MessageService(clientReseau);
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
    public void onConnexionReussie(Utilisateur moi) {
        // Initialiser CallService après connexion
        this.callService = new CallService(clientReseau, moi);
        clientReseau.setCallService(callService);
    }
    // ===== 3. INSCRIPTION =====
    public String sInscrire(String nomComplet, String numero, String password) {
        if (!verifierConnexion()) {
            return "Erreur : Pas connecté au serveur !";
        }
        return authService.inscrire(nomComplet, numero, password);
    }
    // ===== 3. DÉCONNEXION =====
    public void seDeconnecter() {
        if (authService != null) {
            authService.deconnecter();
        }
        connecteAuServeur = false;
    }
    public void envoyerMessage(String numeroDestinataire, String contenu) {
        if (!verifierConnexion()) {
            System.out.println("Erreur : Pas connecté au serveur !");
            return;
        }
        if (messageService == null) {
            System.out.println("Erreur : Service de messagerie non initialisé !");
            return;
        }
        if (clientReseau.getMoi() == null) {
            System.out.println("Erreur : Pas authentifié !");
            return;
        }
        messageService.envoyerMessage(numeroDestinataire, contenu);
    }
    public void appeler(String numeroDest, int idConv, String type) {
        if (callService == null) {
            System.out.println("Erreur : CallService non initialisé !");
            return;
        }
        callService.appeler(numeroDest, idConv, model.enums.TypeAppel.valueOf(type));
    }
    public void accepterAppel() {
        if (callService != null) callService.accepte();
    }

    public void refuserAppel() {
        if (callService != null) callService.raccrocher();
    }

    public void raccrocher() {
        if (callService != null) callService.raccrocher();
    }
    public boolean isEnAppel() {
        return callService != null && callService.isEnAppel();
    }

    // ===== 7. LISTE UTILISATEURS (NOUVEAU) =====
    public void demanderListeUtilisateurs() {
        if (!verifierConnexion()) return;
        Packet p = new Packet(Protocol.USERS_LIST, "");
        clientReseau.envoyer(p);
    }

    public void demanderConversations() {
        if (!verifierConnexion()) return;

        Packet p = new Packet(Protocol.LISTE_CONVERSATIONS, "");
        clientReseau.envoyer(p);
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
            return false;
        }
        return true;
    }
}