package model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class Conversation {
    private int idConversation;
    private String typeConversation;
    private String nomGroupe;
    private java.time.LocalDateTime dateCreation;
    private java.time.LocalDateTime dateDernierMessage;
    private Integer idCreateur;
    private String nomContact;
    private String numeroContact;
    private String dernierMessage;
    private int messagesNonLus;

    // Pour l'affichage dans la liste
    public String getApercu() {
        if (dernierMessage == null) return "Pas encore de message";
        if (dernierMessage.length() > 30) {
            return dernierMessage.substring(0, 30) + "...";
        }
        return dernierMessage;
    }
}
