/*package service;

import Serveur.Protocol;
import client.ClientReseau;
import network.Packet;

public class ContactService {

    private ClientReseau client;

    public ContactService(ClientReseau client) {
        this.client = client;
    }

    public void ajouterContact(String numeroTelephone) {

        // Format : AJOUTER_CONTACT|monNumero|numeroAAjouter
        String monNumero = client.getMoi() != null ? client.getMoi().getNumeroTelephone() : "";
        if (monNumero.isEmpty()) {
            return;
        }
        String data = monNumero + "|" + numeroTelephone;
        Packet p = new Packet(Protocol.AJOUTER_CONTACT, data);
        client.envoyer(p);

        System.out.println(" Demande d'ajout envoyée pour " + numeroTelephone);
    }

    // ===== 2. SUPPRIMER UN CONTACT =====
    public void supprimerContact(String numeroTelephone) {
        String monNumero = client.getMoi() != null ? client.getMoi().getNumeroTelephone() : "";

        String data = monNumero + "|" + numeroTelephone;
        Packet p = new Packet(Protocol.SUPPRIMER_CONTACT, data);
        client.envoyer(p);

        System.out.println(" Demande de suppression envoyée pour " + numeroTelephone);
    }

    public void demanderListeContacts(){
        String monNumero = client.getMoi() != null ? client.getMoi().getNumeroTelephone() : "";

        Packet p = new Packet(Protocol.LISTE_CONTACTS, monNumero);
        client.envoyer(p);

        System.out.println(" Demande de liste des contacts envoyée");
    }
}*/