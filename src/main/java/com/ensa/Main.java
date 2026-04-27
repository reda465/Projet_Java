package com.ensa;

import client.ClientReseau;
import client.EcouteurClient;
import model.Utilisateur;
import network.Packet;
import Serveur.Protocol;
import service.AuthService;
import service.CallAudiooService;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== TEST CONSOLE RESEAU (CLIENT) ===");
        Scanner scanner = new Scanner(System.in);
        CallAudiooService[] audioServiceRef = new CallAudiooService[1];
        ClientReseau[] clientRef = new ClientReseau[1];

        // 1. Définition de l'écouteur
        EcouteurClient ecouteur = new EcouteurClient() {
            @Override
            public void connexionReussie(Utilisateur moi) {
                System.out.println("\n✅ Connecté ! Bienvenue " + moi.getNomComplet());
            }

            @Override
            public void inscriptionReussie(String msg) {
                System.out.println("\n✅ Inscription réussie : " + msg);
            }

            @Override
            public void erreur(String message) {
                System.out.println("\n❌ Erreur : " + message);
            }

            @Override
            public void messageRecu(String contenu) {
                System.out.println("\n✉️ Message : " + contenu);
            }

            @Override
            public void deconnexion() {
                System.out.println("\n🔌 Déconnecté du serveur.");
            }

            @Override
            public void audioCallStart(String info) {
                System.out.println("\n📞 Début d'appel : " + info);
            }

            @Override
            public void audioCallStop(String info) {
                System.out.println("\n🛑 Appel terminé : " + info);
            }

            @Override
            public void audioRecu(byte[] audio) {
                if (audioServiceRef[0] != null) {
                    audioServiceRef[0].jouerAudio(audio);
                }
            }

            @Override
            public void videoRecu(byte[] frameData) {
            }
        };

        // 2. Connexion au serveur
        ClientReseau client = new ClientReseau(ecouteur);
        clientRef[0] = client;
        client.connecterAuServeur("127.0.0.1", 5000);
        
        AuthService authService = new AuthService(client);
        CallAudiooService audioService = new CallAudiooService(client);
        audioServiceRef[0] = audioService;

        Thread.sleep(1000); // Laisser le temps à la connexion de se faire

        // 3. Menu basique
        System.out.println("\n--- MENU ---");
        System.out.println("1. S'inscrire");
        System.out.println("2. Se connecter");
        System.out.print("Choix : ");
        String choix = scanner.nextLine();

        String monNumero = "";

        if ("1".equals(choix)) {
            System.out.print("Nom complet : ");
            String nom = scanner.nextLine();
            System.out.print("Numéro (10 chiffres) : ");
            monNumero = scanner.nextLine();
            System.out.print("Mot de passe : ");
            String mdp = scanner.nextLine();
            authService.inscrire(nom, monNumero, mdp);
            Thread.sleep(1000);
            System.out.println("Connexion auto...");
            authService.connecter(monNumero, mdp);
        } else {
            System.out.print("Numéro : ");
            monNumero = scanner.nextLine();
            System.out.print("Mot de passe : ");
            String mdp = scanner.nextLine();
            authService.connecter(monNumero, mdp);
        }

        Thread.sleep(1500);

        // 4. Test Appel Audio
        System.out.println("\n--- TEST APPEL ---");
        System.out.print("Voulez-vous appeler quelqu'un ? (oui/non) : ");
        if (scanner.nextLine().equalsIgnoreCase("oui")) {
            System.out.print("Numéro à appeler : ");
            String dest = scanner.nextLine();
            
            audioService.startCall(monNumero, dest);
            System.out.println("📞 Appel lancé vers " + dest + " ! Parlez dans le micro.");
            System.out.println("Tapez 'stop' pour raccrocher.");
            
            while (true) {
                if (scanner.nextLine().equalsIgnoreCase("stop")) {
                    audioService.stopCall(monNumero, dest);
                    break;
                }
            }
        } else {
            System.out.println("En attente d'appel entrant...");
            System.out.println("💡 (Si on vous appelle depuis une autre console, l'audio passera tout seul !)");
            System.out.print("Tapez le numéro de l'appelant pour 'accepter' (simulation), ou 'quitter' pour fermer : ");
            
            while (true) {
                String cmd = scanner.nextLine();
                if (cmd.equalsIgnoreCase("quitter")) {
                    break;
                } else if (!cmd.isEmpty()) {
                    Packet p = new Packet(Protocol.CALL_ACCEPT, cmd);
                    client.envoyer(p);
                    System.out.println("📞 Appel accepté avec " + cmd + ". L'audio est activé.");
                }
            }
        }

        client.deconnecter();
        System.out.println("Fin du test.");
        System.exit(0);
    }
}
