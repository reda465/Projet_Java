package com.ensa;

import client.*;
import model.*;

import java.util.List;

public class Test implements EcouteurClient {

    private ClientHandlerAuth facade;

    public static void main(String[] args) {
        new Test().tester();
    }

    public void tester() {
        facade = ClientHandlerAuth.getInstance();

        // 1. Connexion
        System.out.println("=== CONNEXION ===");
        facade.connecterAuServeur("localhost", 8080, this);
        facade.seConnecter("0612345678", "pass123");
        attendre(1000);

        // 2. Demander conversations
        System.out.println("\n=== CONVERSATIONS ===");
        facade.demanderConversations();
        attendre(1000);
    }

    private void attendre(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }

    public void connexionReussie(Utilisateur moi) {
        System.out.println(">>> Connecté : " + moi.getNomComplet());
    }

    @Override
    public void inscriptionReussie(String msg) {

    }

    public void conversationsRecues(java.util.List<Conversation> conversations) {
        System.out.println(">>> " + conversations.size() + " conversations :");
        for (Conversation c : conversations) {
            System.out.println("  - " + c.getNomContact() + " (" + c.getNumeroContact() + ")");
            System.out.println("    Dernier msg : " + c.getApercu());
            System.out.println("    Non lus : " + c.getMessagesNonLus());
        }
    }

    @Override
    public void contactAjoute(Contact contact) {

    }

    @Override
    public void listeContactsRecue(List<Contact> contacts) {

    }

    public void messageRecu(Message message) {
        System.out.println(">>> Message de " + message.getTelephoneExpediteur());
    }

    public void appelEntrant(String numero, String type) {
        System.out.println(">>> Appel " + type + " de " + numero);
    }

    @Override
    public void appelAccepte(String numero) {

    }

    @Override
    public void appelRefuse(String numero) {

    }

    @Override
    public void appelTermine(String numero) {

    }

    public void erreur(String message) {
        System.out.println(">>> ERREUR : " + message);
    }

    @Override
    public void messageRecu(String numeroDest, String message) {

    }

    public void deconnexion() {
        System.out.println(">>> DÉCONNECTÉ");
    }
}