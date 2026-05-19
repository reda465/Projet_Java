package model;

import java.time.LocalDateTime;

public class MessageGroupe {
    private int idMessage;
    private int idGroupe;
    private String telephoneExpediteur;
    private String nomExpediteur;
    private String contenu;
    private LocalDateTime dateEnvoi;

    public int getIdMessage() { return idMessage; }
    public void setIdMessage(int idMessage) { this.idMessage = idMessage; }
    public int getIdGroupe() { return idGroupe; }
    public void setIdGroupe(int idGroupe) { this.idGroupe = idGroupe; }
    public String getTelephoneExpediteur() { return telephoneExpediteur; }
    public void setTelephoneExpediteur(String telephoneExpediteur) { this.telephoneExpediteur = telephoneExpediteur; }
    public String getNomExpediteur() { return nomExpediteur; }
    public void setNomExpediteur(String nomExpediteur) { this.nomExpediteur = nomExpediteur; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
}