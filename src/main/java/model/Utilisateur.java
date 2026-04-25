package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Utilisateur {

    private int id;
    private String nom;
    private String prenom;
    private String numeroTelephone;
    private String motDePasse;
    private boolean enLigne;

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}