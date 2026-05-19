package model;
import model.enums.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor

public class Appel {
    private int idAppel;
    private int idAppelant;
    private int idConversation;
    private java.time.LocalDateTime dateAppel;
    private int dureeSecondes;
    private TypeAppel TypeAppel;
    private StatutAppel statut;       // manque, accepte, refuse, en_cours
}
