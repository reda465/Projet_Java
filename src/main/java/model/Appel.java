package model;
import model.enums.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Appel {
    // SUPPRIMER les enums locaux ! Utiliser ceux de model.enums

    private int idAppel;
    private int idAppelant;
    private int idConversation;
    private java.time.LocalDateTime dateAppel;
    private int dureeSecondes;
    private TypeAppel TypeAppel;
    private StatutAppel statut;       // manque, accepte, refuse, en_cours
}
