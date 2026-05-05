package model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageGroupe extends Message {
    private int idGroupe;
    private String nomGroupe;        // Transient - pour affichage

    // Format réseau: idMessage;idGroupe;telephoneExp;nomExp;contenu;date;nomGroupe
    @Override
    public String toNetworkString() {
        return getIdMessage() + ";"
                + idGroupe + ";"
                + getTelephoneExpediteur() + ";"
                + getNomExpediteur() + ";"
                + getContenuTexte() + ";"
                + (getDateEnvoi() != null ? getDateEnvoi().toString() : "") + ";"
                + (nomGroupe != null ? nomGroupe : "");
    }

    public static MessageGroupe fromNetworkString(String data) {
        String[] parts = data.split(";", -1);
        MessageGroupe mg = new MessageGroupe();
        mg.setIdMessage(Integer.parseInt(parts[0]));
        mg.setIdGroupe(Integer.parseInt(parts[1]));
        mg.setTelephoneExpediteur(parts[2]);
        mg.setNomExpediteur(parts[3]);
        mg.setContenuTexte(parts[4]);
        try { mg.setDateEnvoi(LocalDateTime.parse(parts[5])); } catch (Exception e) { mg.setDateEnvoi(null); }
        mg.setNomGroupe(parts[6].isEmpty() ? null : parts[6]);
        mg.setEstMoi(false); // Sera déterminé côté client
        return mg;
    }
}