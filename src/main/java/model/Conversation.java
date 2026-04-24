package model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Conversation {
    private int idConversation;
    private String typeConversation;
    private String nomGroupe;
    private java.time.LocalDateTime dateCreation;
    private java.time.LocalDateTime dateDernierMessage;
    private Integer idCreateur;
}
