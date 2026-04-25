package model;
import model.enums.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Utilisateur {
    private int idUtilisateur;
    private String nomComplet;
    private String numeroTelephone;
    private String motDePasse;
    private java.time.LocalDateTime dateInscription;
    private java.time.LocalDateTime derniereConnexion;
    private StatutUtilisateur Status;
    private String photoProfil;
}