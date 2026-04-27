package model;
import model.enums.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class Appel {
    enum TypeAppel {audio,video}
    enum Status{manque, accepte, refuse, en_cours}
    private int idAppel;
    private int idAppelant;
    private int idConversation;
    private java.time.LocalDateTime dateAppel;
    private TypeAppel type_appel;
    private int dureeSecondes;
    private TypeAppel TypeAppel;
    private StatutAppel statut;       // manque, accepte, refuse, en_cours
    private TypeAppel statut;
}
