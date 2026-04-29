package model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private LocalDateTime dateEnvoi;
    private boolean estMoi;
    private String nomExpediteur;

    public String getDateFormatee() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateEnvoi.format(formatter);
    }

    public String toString() {
        return "[" + getDateFormatee() + "] " + contenuTexte;
    }
}
