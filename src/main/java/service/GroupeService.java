package service;

import Serveur.Protocol;
import client.ClientReseau;
import lombok.Getter;
import lombok.Setter;
import network.Packet;

@Getter
@Setter
public class GroupeService {

    private ClientReseau client;

    public GroupeService(ClientReseau client) {
        this.client = client;
    }

    // ===== CRÉER UN GROUPE =====
    // Format: CREATE_GROUP|nomGroupe|numeroMembre1,numeroMembre2,...
    public void creerGroupe(String nomGroupe, String... numerosMembres) {
        StringBuilder data = new StringBuilder(nomGroupe);
        if (numerosMembres != null && numerosMembres.length > 0) {
            data.append("|");
            for (int i = 0; i < numerosMembres.length; i++) {
                if (i > 0) data.append(",");
                data.append(numerosMembres[i]);
            }
        }
        Packet p = new Packet(Protocol.CREATE_GROUP, data.toString());
        client.envoyer(p);
        System.out.println(" Demande création groupe: " + nomGroupe);
    }

    // ===== AJOUTER MEMBRE =====
    // Format: ADD_MEMBER|idGroupe|numeroMembre
    public void ajouterMembre(int idGroupe, String numeroMembre) {
        if (!verifier()) return;
        String data = idGroupe + "|" + numeroMembre;
        Packet p = new Packet(Protocol.ADD_MEMBER, data);
        client.envoyer(p);
    }

    // Format: REMOVE_MEMBER|idGroupe|numeroMembre
    public void retirerMembre(int idGroupe, String numeroMembre) {
        if (!verifier()) return;
        String data = idGroupe + "|" + numeroMembre;
        Packet p = new Packet(Protocol.REMOVE_MEMBER, data);
        client.envoyer(p);
    }

    // Format: LEAVE_GROUP|idGroupe
    public void quitterGroupe(int idGroupe) {
        if (!verifier()) return;
        Packet p = new Packet(Protocol.LEAVE_GROUP, String.valueOf(idGroupe));
        client.envoyer(p);
    }

    // ===== SUPPRIMER GROUPE =====
    // Format: DELETE_GROUP|idGroupe
    public void supprimerGroupe(int idGroupe) {
        if (!verifier()) return;
        Packet p = new Packet(Protocol.DELETE_GROUP, String.valueOf(idGroupe));
        client.envoyer(p);
    }

    // ===== MODIFIER NOM =====
    // Format: UPDATE_GROUP_NAME|idGroupe|nouveauNom
    public void modifierNomGroupe(int idGroupe, String nouveauNom) {
        if (!verifier()) return;
        if (nouveauNom == null || nouveauNom.trim().isEmpty()) {
            System.out.println("❌ Nom vide !");
            return;
        }
        String data = idGroupe + "|" + nouveauNom;
        Packet p = new Packet(Protocol.UPDATE_GROUP_NAME, data);
        client.envoyer(p);
    }

    // ===== DEMANDER LISTE GROUPES =====
    // Format: GET_GROUPS|
    public void demanderGroupes() {
        if (!verifier()) return;
        Packet p = new Packet(Protocol.GET_GROUPS, "");
        client.envoyer(p);
    }

    // ===== DEMANDER MEMBRES =====
    // Format: GET_GROUP_MEMBERS|idGroupe
    public void demanderMembres(int idGroupe) {
        if (!verifier()) return;
        Packet p = new Packet(Protocol.GET_GROUP_MEMBERS, String.valueOf(idGroupe));
        client.envoyer(p);
    }

    // ===== ENVOYER MESSAGE GROUPE =====
    // Format: GROUP_MSG_SEND|idGroupe|contenu
    public void envoyerMessageGroupe(int idGroupe, String contenu) {
        if (!verifier()) return;
        if (contenu == null || contenu.trim().isEmpty()) {
            System.out.println(" Message vide !");
            return;
        }
        String data = idGroupe + "|" + contenu;
        Packet p = new Packet(Protocol.GROUP_MSG_SEND, data);
        client.envoyer(p);
    }

    private boolean verifier() {
        if (client == null || !client.isConnecte()) {
            System.out.println(" Non connecté au serveur !");
            return false;
        }
        return true;
    }
}