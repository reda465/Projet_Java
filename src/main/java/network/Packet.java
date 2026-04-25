package network;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Packet {

    private Commande commande;
    private String data;
    private int expediteurId;       // Qui envoie (0 si pas encore connecté)

    public Packet(Commande commande, String data) {
        this.commande = commande;
        this.data = data;
        this.expediteurId = 0;     // Par défaut, pas d'ID , sera modifier apres connexion au serveur
    }
    // ===== Transformer en String pour le réseau/serveur =====
    // Format : COMMANDE|expediteurId|données
    public String toString() {
        return commande + "|" + expediteurId + "|" + data;
    }

    // Reconstruire un Packet depuis un String reçu
    public static Packet fromString(String ligne) {
        String[] parts = ligne.split("\\|",3);  // Coupe aux "|"

        Commande cmd = Commande.valueOf(parts[0]);  // String → enum
        String donnees = (parts.length > 2) ? parts[2] : "";
        Packet p = new Packet(cmd, donnees);
        p.setExpediteurId(Integer.parseInt(parts[1]));

        return p;
    }
}
