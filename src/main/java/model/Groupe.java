package model;

import java.util.ArrayList;
import java.util.List;

public class Groupe {
    private int idGroupe;
    private String nomGroupe;
    private String numeroCreateur;
    private List<String> numerosMembres;
    private String dateCreation;

    public Groupe() {
        this.numerosMembres = new ArrayList<>();
    }

    // ── Sérialisation réseau ───────────────────────────────────────────────
    // Format : id;nom;numCreateur;date;num1;num2;...
    public String toNetworkString() {
        StringBuilder sb = new StringBuilder();
        sb.append(idGroupe).append(";")
                .append(nomGroupe != null ? nomGroupe : "").append(";")
                .append(numeroCreateur != null ? numeroCreateur : "").append(";")
                .append(dateCreation != null ? dateCreation : "");
        if (numerosMembres != null) {
            for (String numero : numerosMembres) {
                sb.append(";").append(numero != null ? numero : "");
            }
        }
        return sb.toString();
    }

    public static Groupe fromNetworkString(String data) {
        String[] parts = data.split(";", -1);
        Groupe g = new Groupe();
        if (parts.length > 0 && !parts[0].isEmpty()) g.setIdGroupe(Integer.parseInt(parts[0]));
        if (parts.length > 1) g.setNomGroupe(parts[1]);
        if (parts.length > 2) g.setNumeroCreateur(parts[2]);
        if (parts.length > 3) g.setDateCreation(parts[3]);
        List<String> membres = new ArrayList<>();
        for (int i = 4; i < parts.length; i++) {
            if (!parts[i].isEmpty()) membres.add(parts[i]);
        }
        g.setNumerosMembres(membres);
        return g;
    }

    public int getIdGroupe() { return idGroupe; }
    public void setIdGroupe(int idGroupe) { this.idGroupe = idGroupe; }
    public String getNomGroupe() { return nomGroupe; }
    public void setNomGroupe(String nomGroupe) { this.nomGroupe = nomGroupe; }
    public String getNumeroCreateur() { return numeroCreateur; }
    public void setNumeroCreateur(String numeroCreateur) { this.numeroCreateur = numeroCreateur; }
    public List<String> getNumerosMembres() { return numerosMembres; }
    public void setNumerosMembres(List<String> numerosMembres) { this.numerosMembres = numerosMembres; }
    public String getDateCreation() { return dateCreation; }
    public void setDateCreation(String dateCreation) { this.dateCreation = dateCreation; }
}