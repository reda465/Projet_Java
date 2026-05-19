package service;

import Serveur.Protocol;
import client.ClientReseau;
import lombok.Getter;
import lombok.Setter;
import network.*;
@Getter
@Setter

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
        String dest = numeroDestinataire != null
                ? numeroDestinataire.trim().replaceAll("\\s+", "").replace("-", "")
                : "";
        String data = dest + "|" + contenu;
        Packet p = new Packet(Protocol.MSG_SEND, data);
        client.envoyer(p);
    }
}
