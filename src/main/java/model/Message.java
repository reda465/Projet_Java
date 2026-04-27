package model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class Message {
    private int idMessage;
    private int idConversation;
    private int idExpediteur;
    private String telephoneExpediteur;
    private String telephoneDestinataire;
    private String typeMessage;
    private String contenuTexte;
    private String urlFichier;
    private String nomFichier;
    private Long tailleFichier;
    private java.time.LocalDateTime dateEnvoi;
}
