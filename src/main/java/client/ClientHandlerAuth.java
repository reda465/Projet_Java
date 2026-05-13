package client;

import Serveur.Protocol;
import lombok.Getter;
import lombok.Setter;
import model.Utilisateur;
import network.Packet;
import service.*;
import model.Contact;
import javafx.scene.image.ImageView;

@Getter
@Setter

public class ClientHandlerAuth {

    private static ClientHandlerAuth instance;

    private ClientReseau clientReseau;
    private AuthService authService;
    private MessageService messageService;
    private boolean connecteAuServeur = false;
    private ContactService contactService;
    private GroupeService groupeService;
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
            groupeService = new GroupeService(clientReseau);
            contactService = new ContactService(clientReseau);
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
    //gestion des contacts
    public void ajouterContact(String numeroTelephone, String nomAffiche) {
        if (!verifierConnexion()) {
            System.out.println(" Pas connecté au serveur !");
            return;
        }
        if (contactService == null) {
            System.out.println(" Service contact non initialisé !");
            return;
        }
        contactService.ajouterContact(numeroTelephone, nomAffiche);
    }
    public void ajouterContact(String numeroTelephone) {
        ajouterContact(numeroTelephone, numeroTelephone);
    }

    public void accepterDemandeContact(String numeroDemandeur) {
        if (!verifierConnexion() || contactService == null) return;
        contactService.accepterDemandeContact(numeroDemandeur);
    }

    public void bloquerNumeroContact(String numero) {
        if (!verifierConnexion() || contactService == null) return;
        contactService.bloquerNumero(numero);
    }
    //gestion des groupes
    public void creerGroupe(String nomGroupe, String... numerosMembres) {
        if (!verifierConnexion()) return;
        if (groupeService == null) { System.out.println("❌ GroupeService non initialisé"); return; }
        groupeService.creerGroupe(nomGroupe, numerosMembres);
    }
    public void ajouterMembreAuGroupe(int idGroupe, String numeroMembre) {
        if (!verifierConnexion()) return;
        groupeService.ajouterMembre(idGroupe, numeroMembre);
    }

    public void retirerMembreDuGroupe(int idGroupe, String numeroMembre) {
        if (!verifierConnexion()) return;
        groupeService.retirerMembre(idGroupe, numeroMembre);
    }
    public void quitterGroupe(int idGroupe) {
        if (!verifierConnexion()) return;
        groupeService.quitterGroupe(idGroupe);
    }

    public void supprimerGroupe(int idGroupe) {
        if (!verifierConnexion()) return;
        groupeService.supprimerGroupe(idGroupe);
    }
    public void modifierNomGroupe(int idGroupe, String nouveauNom) {
        if (!verifierConnexion()) return;
        groupeService.modifierNomGroupe(idGroupe, nouveauNom);
    }

    public void demanderListeGroupes() {
        if (!verifierConnexion()) return;
        groupeService.demanderGroupes();
    }

    public void demanderMembresGroupe(int idGroupe) {
        if (!verifierConnexion()) return;
        groupeService.demanderMessagesGroupe(idGroupe);
    }
    public void envoyerMessageGroupe(int idGroupe, String contenu) {
        if (!verifierConnexion()) return;
        groupeService.envoyerMessageGroupe(idGroupe, contenu);
    }
    public void demanderMessagesGroupe(int idGroupe) {
        if (!verifierConnexion()) return;
        groupeService.demanderMessagesGroupe(idGroupe);
    }

    // =====  DÉCONNEXION =====
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
        if (callService != null) callService.refuser();
    }

    public void raccrocher() {
        if (callService != null) callService.raccrocher();
    }

    public void setVideoView(ImageView view) {
        if (callService != null) callService.setVideoView(view);
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
    public void demanderMessages(int idConversation) {
        if (!verifierConnexion()) {
            System.out.println("❌ Pas connecté au serveur !");
            return;
        }
        if (clientReseau == null) {
            System.out.println("❌ Client réseau non initialisé !");
            return;
        }
        clientReseau.demanderMessages(idConversation);
    }

    public void demanderConversations() {
        if (!verifierConnexion()) {
            System.out.println("❌ Pas connecté au serveur !");
            return;
        }
        if (clientReseau == null) {
            System.out.println("❌ Client réseau non initialisé !");
            return;
        }
        clientReseau.demanderConversations();
    }
    // Dans ClientHandlerAuth.java
    public boolean connecterAuServeurTest(String ip, int port, EcouteurClient ecouteur) {
        // Utiliser le même code mais pointer vers le mock server
        return connecterAuServeur(ip, port, ecouteur);
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