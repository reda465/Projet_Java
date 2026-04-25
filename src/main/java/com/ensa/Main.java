package com.ensa;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
// TestFacade.java
import client.*;

public class Main implements EcouteurClient {

    public static void main(String[] args) {
        new Main().tester();
    }

    public void tester() {
        ClientHandlerAuth facade = ClientHandlerAuth.getInstance();

        // Test connexion simulation
        boolean ok = facade.connecterAuServeur("192.168.137.89", 5000, this);
        System.out.println("Connecté : " + ok);

        // Test inscription
        facade.seConnecter( "0612349678", "pass123");

        // Attendre un peu
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    public void connexionReussie(model.Utilisateur moi) {
        System.out.println(" Réussi : " + moi.getNomComplet());
    }

    @Override
    public void inscriptionReussie(String msg) {

    }

    public void erreur(String message) {
        System.out.println(" Erreur : " + message);
    }

    public void messageRecu(String contenu) {
        System.out.println(" Message : " + contenu);
    }

    public void deconnexion() {
        System.out.println(" Déconnecté");
    }
}
