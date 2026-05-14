package model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Contact {
    private int idContact;
    private int idUtilisateur;
    private int idContactUtilisateur;
    private String nomAffiche;
    private boolean estBloque;
    private java.time.LocalDateTime dateAjout;
    private String nomComplet;
    private String numeroTelephone;
    private String photoProfil;
    public Contact(int idContact, int idUtilisateur, int idContactUtilisateur,
                   String nomAffiche, boolean estBloque,
                   java.time.LocalDateTime dateAjout) {
        this.idContact = idContact;
        this.idUtilisateur = idUtilisateur;
        this.idContactUtilisateur = idContactUtilisateur;
        this.nomAffiche = nomAffiche;
        this.estBloque = estBloque;
        this.dateAjout = dateAjout;
    }

     public Contact(int idContact, int idUtilisateur, int idContactUtilisateur,
                    String nomAffiche, boolean estBloque,
                    java.time.LocalDateTime dateAjout, String nomComplet,
                    String numeroTelephone, String photoProfil) {
        this.idContact = idContact;
        this.idUtilisateur = idUtilisateur;
        this.idContactUtilisateur = idContactUtilisateur;
        this.nomAffiche = nomAffiche;
        this.estBloque = estBloque;
        this.dateAjout = dateAjout;
        this.nomComplet = nomComplet;
        this.numeroTelephone = numeroTelephone;
        this.photoProfil = photoProfil;
    }
}
