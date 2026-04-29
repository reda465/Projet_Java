package service;

import Serveur.Protocol;
import client.ClientReseau;
import network.*;

public class MessageService {

    private ClientReseau client;

    public MessageService(ClientReseau client) {
        this.client = client;
    }

    // ===== ENVOYER MESSAGE =====
    // Format : MSG_SEND|numeroExp|numeroDest|contenu
    public void envoyerMessage(String numeroDestinataire, String contenu) {
        if (contenu == null || contenu.trim().isEmpty()) {
            System.out.println("Message vide !");
            return;
        }
        String monNumero = "";
        if (client.getMoi() != null) {
            monNumero = client.getMoi().getNumeroTelephone();
        }
        // data = numeroExp|numeroDest|contenu
        String data = numeroDestinataire + "|" + contenu;
        Packet p = new Packet(Protocol.MSG_SEND, data);
        client.envoyer(p);
    }
}
