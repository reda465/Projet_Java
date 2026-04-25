package network;

import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

@Getter
@Setter
public class Packet {

    private Commande commande;
    private Object data;
    private int expediteurId;       // Qui envoie (0 si pas encore connecté)

    public Packet(Commande commande, Object data) {
        this.commande = commande;
        this.data = data;
        this.expediteurId = 0;     // Par défaut, pas d'ID , sera modifier apres connexion au serveur
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

        return commande + "|" + expediteurId + "|" + type + "|" + contenu;
    }

    // Reconstruire un Packet depuis un String reçu
    public static Packet fromString(String ligne) {
        String[] parts = ligne.split("\\|",4);  // Coupe aux "|"

        Commande cmd = Commande.valueOf(parts[0]);
        int id = Integer.parseInt(parts[1]);// String → enum
        String type = (parts.length > 2) ? parts[2] : "STRING";
        String contenu = (parts.length > 3) ? parts[3] : "";

        Object donnees;

        if (type.equals("BYTES")) {
            donnees = Base64.getDecoder().decode(contenu);
        } else {
            donnees = contenu;
        }

        Packet p = new Packet(cmd, donnees);
        p.setExpediteurId(id);

        return p;
    }
}
