package network;

import Serveur.Protocol;
import lombok.Getter;
import lombok.Setter;
import Serveur.Protocol;

import java.util.Base64;

@Getter
@Setter
public class Packet {

    private Protocol protocol;
    private Object data;
    private int expediteurId;// Qui envoie (0 si pas encore connecté)
    private String type;



    public Packet(Protocol protocol, Object data) {
        this.protocol = protocol;
        this.data = data;
        this.expediteurId = 0;     // Par défaut, pas d'ID , sera modifier apres connexion au serveur
        this.type = (data instanceof byte[]) ? "BYTES" : "STRING";
    }



    // ===== Transformer en String pour le réseau/serveur =====
    // Format : COMMANDE|expediteurId|données
    public String toString() {
        String type;
        String contenu;
        if (data instanceof byte[]) {
            type = "BYTES";
            contenu = Base64.getEncoder().encodeToString((byte[]) data);
        } else {
            type = "STRING";
            contenu = (data != null) ? data.toString() : "";
        }

        return protocol + "|" + expediteurId + "|" + type + "|" + contenu;
    }

    // Reconstruire un Packet depuis un String reçu
    public static Packet fromString(String ligne) {
        String[] parts = ligne.split("\\|",4);  // Coupe aux "|"

        Protocol protocol = Protocol.valueOf(parts[0]);
        int expediteurId = Integer.parseInt(parts[1]);
        String type = (parts.length > 2) ? parts[2] : "STRING";
        String contenu = (parts.length > 3) ? parts[3] : "";
        //String[] parts = ligne.split("\\|",2);  // Coupe aux "|"

        Object data;

        if (type.equals("BYTES")) {
            data = Base64.getDecoder().decode(contenu);
        } else {
            data = contenu;
        }

      //  Packet p = Protocol.valueOf(parts[0]);  // String → enum
       // String donnees = (parts.length > 1) ? parts[1] : "";
        //Packet p = new Packet(cmd, donnees);
        //p.setExpediteurId(id);
        Packet p = new Packet(protocol, data);
        p.setExpediteurId(expediteurId);
        return p;
    }
}
