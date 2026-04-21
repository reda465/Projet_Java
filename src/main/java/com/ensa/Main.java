package com.ensa;

import network.Commande;
import network.Packet;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
// TestFacade.java
import client.*;

public class Main implements EcouteurClient {

    public static void main(String[] args) {
        new Main().tester();
    }

    public void tester() {
        ChatClientFacade facade = ChatClientFacade.getInstance();

        // Test connexion simulation
        boolean ok = facade.connecterAuServeur("simul", 8080, this);
        System.out.println("Connecté : " + ok);

        // Test inscription
        facade.sInscrire("Dupont", "Jean", "0612345678", "jeandupont", "pass123");

        // Attendre un peu
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    public void connexionReussie(model.Utilisateur moi) {
        System.out.println(" Réussi : " + moi.getNomComplet());
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
