package model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Groupe {
    private int idGroupe;
    private String nomGroupe;
    private int idCreateur;              // ID de l'utilisateur créateur
    private String numeroCreateur;       // Numéro du créateur (transient)
    private String nomCreateur;          // Nom du créateur (transient)
    private LocalDateTime dateCreation;
    private String photoGroupe;///optionnel

    //enrichis par le serveur
    private int nombreMembres;
    private boolean jeSuisAdmin;   // Suis-je admin ?
    private boolean jeSuisMembre;  // Suis-je membre ? (toujours true si je reçois)
    private List<Utilisateur> membres; // Liste chargée à la demande
    private String dernierMessage;     // Pour affichage liste
    private LocalDateTime dateDernierMessage;

    // Format: idGroupe;nomGroupe;idCreateur;numeroCreateur;nomCreateur;dateCreation;photo;nombreMembres;jeSuisAdmin;dernierMessage;dateDernierMessage
    public String toNetworkString() {
        return idGroupe + ";"
                + nomGroupe + ";"
                + idCreateur + ";"
                + (numeroCreateur != null ? numeroCreateur : "") + ";"
                + (nomCreateur != null ? nomCreateur : "") + ";"
                + (dateCreation != null ? dateCreation.toString() : "") + ";"
                + (photoGroupe != null ? photoGroupe : "") + ";"
                + nombreMembres + ";"
                + jeSuisAdmin + ";"
                + (dernierMessage != null ? dernierMessage : "") + ";"
                + (dateDernierMessage != null ? dateDernierMessage.toString() : "");
    }

    public static Groupe fromNetworkString(String data) {
        String[] parts = data.split(";", -1);
        Groupe g = new Groupe();
        g.setIdGroupe(Integer.parseInt(parts[0]));
        g.setNomGroupe(parts[1]);
        g.setIdCreateur(Integer.parseInt(parts[2]));
        g.setNumeroCreateur(parts[3].isEmpty() ? null : parts[3]);
        g.setNomCreateur(parts[4].isEmpty() ? null : parts[4]);
        try { g.setDateCreation(LocalDateTime.parse(parts[5])); } catch (Exception e) { g.setDateCreation(null); }
        g.setPhotoGroupe(parts[6].isEmpty() ? null : parts[6]);
        g.setNombreMembres(Integer.parseInt(parts[7]));
        g.setJeSuisAdmin(Boolean.parseBoolean(parts[8]));
        g.setDernierMessage(parts[9].isEmpty() ? null : parts[9]);
        try { g.setDateDernierMessage(LocalDateTime.parse(parts[10])); } catch (Exception e) { g.setDateDernierMessage(null); }
        return g;
    }
}