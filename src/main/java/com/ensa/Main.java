package com.ensa;

import network.Commande;
import network.Packet;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // Test création de Packet
        Packet p = new Packet(Commande.CONNEXION, "0600000000|monpass");
        String ligne = p.toString();
        System.out.println("Envoyé : " + ligne);

        // Test reconstruction
        Packet recu = Packet.fromString(ligne);
        System.out.println("Commande : " + recu.getCommande());
        System.out.println("Données : " + recu.getData());

        // Test inscription
        Packet insc = new Packet(Commande.INSCRIPTION, "Dupont|Jean|0611223344|jeandupont|password123");
        System.out.println("\nInscription : " + insc.toString());
    }
}
