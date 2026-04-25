package model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class ParticipantConversation {
    private int idParticipant;
    private int idConversation;
    private int idUtilisateur;
    private java.time.LocalDateTime dateJoin;
    private boolean estAdmin;
}
