package model;
import model.enums.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class Appel {
    private int idAppel;
    private int idAppelant;
    private int idConversation;
    private java.time.LocalDateTime dateAppel;
    private TypeAppel type_appel;
    private int dureeSecondes;
    private TypeAppel TypeAppel;
    private StatutAppel statut;       // manque, accepte, refuse, en_cours
}
