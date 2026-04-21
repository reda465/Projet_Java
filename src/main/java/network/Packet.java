package network;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Packet {

    // ===== ATTRIBUTS (private = encapsulation, tu connais !) =====
    private Commande commande;    // L'action demandée
    private String data;           // Les données (ex: "0600000000|password")
    private int expediteurId;       // Qui envoie (0 si pas encore connecté)

    // ===== CONSTRUCTEUR =====
    public Packet(Commande commande, String data) {
        this.commande = commande;
        this.data = data;
        this.expediteurId = 0;     // Par défaut, pas d'ID , sera modifier apres connexion au serveur
    }
    // ===== MÉTHODE CLÉ : Transformer en String pour le réseau =====

    // On envoie : "CONNEXION|0600000000|password|0"
    // Format : COMMANDE|données|expediteurId
    public String toString() {
        return commande + "|" + expediteurId + "|" + data;
    }

    // Reconstruire un Packet depuis un String reçu
    public static Packet fromString(String ligne) {
        String[] parts = ligne.split("\\|",3);  // Coupe aux "|"

        // parts[0] = "CONNEXION"
        // parts[1] = "0600000000|password"
        // parts[2] = "0"

        Commande cmd = Commande.valueOf(parts[0]);  // String → enum
        String donnees = (parts.length > 2) ? parts[2] : "";
        Packet p = new Packet(cmd, donnees);
        p.setExpediteurId(Integer.parseInt(parts[1]));

        return p;
    }
}
