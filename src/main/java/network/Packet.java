package network;

import Serveur.Protocol;
import lombok.Getter;
import lombok.Setter;
import Serveur.Protocol;

@Getter
@Setter
public class Packet {

    private Protocol commande;
    private String data;

    public Packet(Protocol commande, String data) {
        this.commande = commande;
        this.data = data;
    }
    // ===== Transformer en String pour le réseau/serveur =====
    // Format : COMMANDE|expediteurId|données
    public String toString() {
        return commande + "|" + data;
    }

    // Reconstruire un Packet depuis un String reçu
    public static Packet fromString(String ligne) {
        String[] parts = ligne.split("\\|",2);  // Coupe aux "|"

        Protocol cmd = Protocol.valueOf(parts[0]);  // String → enum
        String donnees = (parts.length > 1) ? parts[1] : "";
        Packet p = new Packet(cmd, donnees);

        return p;
    }
}
