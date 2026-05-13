package service;

import Serveur.Protocol;
import client.ClientReseau;
import lombok.Getter;
import lombok.Setter;
import network.Packet;

@Getter
@Setter
public class ContactService {

    private ClientReseau client;

    public ContactService(ClientReseau client) {
        this.client = client;
    }

    // ===== AJOUTER UN CONTACT =====
    // Format: ADD_CONTACT|numeroContact|nomAffiche (nomAffiche optionnel)
    public void ajouterContact(String numeroTelephone, String nomAffiche) {
        String data = numeroTelephone + "|" + (nomAffiche != null ? nomAffiche : numeroTelephone);
        Packet p = new Packet(Protocol.ADD_CONTACT, data);
        client.envoyer(p);
        System.out.println(" Demande d'ajout contact envoyée: " + numeroTelephone);
    }

    // Surcharge: ajouter avec le nom par défaut (résolu par le serveur)
    public void ajouterContact(String numeroTelephone) {
        ajouterContact(numeroTelephone, numeroTelephone);
    }

    public void accepterDemandeContact(String numeroDemandeur) {
        if (client == null) return;
        String d = numeroDemandeur != null ? numeroDemandeur.trim() : "";
        client.envoyer(new Packet(Protocol.CONTACT_ACCEPTED, d));
    }

    public void bloquerNumero(String numero) {
        if (client == null) return;
        String n = numero != null ? numero.trim() : "";
        client.envoyer(new Packet(Protocol.BLOCK_CONTACT, n));
    }
}