package service;

import Serveur.Protocol;
import client.ClientReseau;
import network.Packet;

public class GroupeService {
    private ClientReseau clientReseau;

    private static String normTel(String t) {
        if (t == null) return "";
        return t.trim().replaceAll("\\s+", "").replace("-", "");
    }

    public GroupeService(ClientReseau clientReseau) {
        this.clientReseau = clientReseau;
    }

    // ── GROUPES ─────────────────────────────────────────────────────────────
    public void creerGroupe(String nomGroupe, String... numerosMembres) {
        if (!verifier()) return;
        String numCreateur = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        StringBuilder data = new StringBuilder();
        data.append(nomGroupe != null ? nomGroupe : "").append("|").append(numCreateur);
        if (numerosMembres != null) {
            for (String numero : numerosMembres) {
                String n = normTel(numero);
                if (!n.isEmpty()) data.append("|").append(n);
            }
        }
        Packet p = new Packet(Protocol.CREATE_GROUP, data.toString());
        clientReseau.envoyer(p);
    }

    public void demanderGroupes() {
        if (!verifier()) return;
        String numero = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        clientReseau.envoyer(new Packet(Protocol.GET_GROUPS, numero));
    }

    public void demanderMessagesGroupe(int idGroupe) {
        if (!verifier()) return;
        clientReseau.envoyer(new Packet(Protocol.GET_GROUP_MESSAGES, String.valueOf(idGroupe)));
    }

    public void envoyerMessageGroupe(int idGroupe, String contenu) {
        if (!verifier()) return;
        String expediteur = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        String data = idGroupe + "|" + expediteur + "|" + (contenu != null ? contenu : "");
        clientReseau.envoyer(new Packet(Protocol.SEND_GROUP_MESSAGE, data));
    }

    public void envoyerFichierGroupe(int idGroupe, java.io.File file,
                                     java.util.function.Consumer<Integer> onProgress) {
        if (!verifier()) return;
        Fileservice fs = new Fileservice(clientReseau);
        fs.envoyerFichierGroupe(idGroupe, file, onProgress);
    }

    public void ajouterMembre(int idGroupe, String numeroMembre) {
        if (!verifier()) return;
        String admin = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        String data = idGroupe + "|" + admin + "|" + normTel(numeroMembre);
        clientReseau.envoyer(new Packet(Protocol.ADD_GROUP_MEMBER, data));
    }

    public void retirerMembre(int idGroupe, String numeroMembre) {
        if (!verifier()) return;
        String admin = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        String data = idGroupe + "|" + admin + "|" + normTel(numeroMembre);
        clientReseau.envoyer(new Packet(Protocol.REMOVE_GROUP_MEMBER, data));
    }

    public void quitterGroupe(int idGroupe) {
        if (!verifier()) return;
        String numero = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        clientReseau.envoyer(new Packet(Protocol.QUIT_GROUP, idGroupe + "|" + numero));
    }

    public void supprimerGroupe(int idGroupe) {
        if (!verifier()) return;
        String admin = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        clientReseau.envoyer(new Packet(Protocol.DELETE_GROUP, idGroupe + "|" + admin));
    }

    public void modifierNomGroupe(int idGroupe, String nouveauNom) {
        if (!verifier()) return;
        String admin = clientReseau.getMoi() != null ? normTel(clientReseau.getMoi().getNumeroTelephone()) : "";
        String data = idGroupe + "|" + admin + "|" + (nouveauNom != null ? nouveauNom : "");
        clientReseau.envoyer(new Packet(Protocol.RENAME_GROUP, data));
    }

    private boolean verifier() {
        if (clientReseau == null || !clientReseau.isConnecte()) {
            System.out.println(" Non connecté au serveur !");
            return false;
        }
        return true;
    }
}