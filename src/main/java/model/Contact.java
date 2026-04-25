package model;

import lombok.Data;
import lombok.NoArgsConstruct;

@Data
@NoArgsConstructor
public class Contact {
    private int idContact;
    private int idUtilisateur;
    private int idContactUtilisateur;
    private String nomAffiche;
    private boolean estBloque;
    private java.time.LocalDateTime dateAjout;
}
