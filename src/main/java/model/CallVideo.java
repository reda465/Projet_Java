package model;

import java.time.LocalDateTime;

public class CallVideo {

    public enum Etat {
        EN_ATTENTE,
        EN_COURS,
        TERMINE,
        REFUSE
    }

    private Utilisateur caller;
    private Utilisateur receiver;
    private Etat etat;
    private LocalDateTime dateDebut;

    public CallVideo(Utilisateur caller, Utilisateur receiver) {
        this.caller = caller;
        this.receiver = receiver;
        this.etat = Etat.EN_ATTENTE;
        this.dateDebut = LocalDateTime.now();
    }

    public Utilisateur getCaller() {
        return caller;
    }

    public Utilisateur getReceiver() {
        return receiver;
    }

    public Etat getEtat() {
        return etat;
    }

    public void setEtat(Etat etat) {
        this.etat = etat;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    // Actions simples (logique)
    public void demarrer() {
        this.etat = Etat.EN_COURS;
    }

    public void terminer() {
        this.etat = Etat.TERMINE;
    }

    public void refuser() {
        this.etat = Etat.REFUSE;
    }
/*
    @Override
    public String toString() {
        return "Appel vidéo : " + caller.getNom() + " -> " + receiver.getNom()
                + " | Etat : " + etat;
    }*/
}

