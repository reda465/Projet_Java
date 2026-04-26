package com.ensa;

import client.ClientReseauCALL;
import client.EcouteurClient;
import model.Utilisateur;
import network.Commande;
import network.Packet;
import service.CallAudiooService;

import java.util.Scanner;

public class MainTestAudio {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== TEST APPEL AUDIO (loopback local, sans serveur) ===\n");

        // Référence partagée au service audio
        CallAudiooService[] serviceRef = new CallAudiooService[1];

        // -------------------------------------------------------
        // EcouteurClient — implémentation minimale
        // -------------------------------------------------------
        EcouteurClient ecouteur = new EcouteurClient(){

            @Override public void connexionReussie(Utilisateur moi) {}
            @Override public void erreur(String message) {
                System.out.println("[ERREUR] " + message);
            }
            @Override public void messageRecu(String contenu) {}
            @Override public void deconnexion() {}
            @Override public void audioCallStart(String info) {
                System.out.println("[AUDIO] Appel entrant : " + info);
            }
            @Override public void audioCallStop(String info) {
                System.out.println("[AUDIO] Appel arrêté : " + info);
            }
            @Override public void audioRecu(byte[] audio) {
                // Loopback : on joue directement ce qu'on reçoit
                if (serviceRef[0] != null) {
                    serviceRef[0].jouerAudio(audio);

                }
            }
            @Override public void videoRecu(byte[] frameData) {}
        };

        // -------------------------------------------------------
        // ClientReseau simulé — envoyer() fait le loopback
        // au lieu d'envoyer sur le réseau
        // -------------------------------------------------------
        ClientReseauCALL clientSimule = new ClientReseauCALL(ecouteur) {

            @Override
            public void envoyer(Packet packet) {
                if (packet.getCommande() == Commande.Data_AUDIO) {
                    // Loopback : micro → jouerAudio directement
                    byte[] audio = (byte[]) packet.getData();
                    ecouteur.audioRecu(audio);
                }
                // Les autres packets (Debuter, Arreter) sont ignorés en local
            }

            @Override
            public boolean isConnecte() {
                return true;
            }
        };

        // -------------------------------------------------------
        // Création du service audio avec le client simulé
        // -------------------------------------------------------
        CallAudiooService audioService = new CallAudiooService(clientSimule);
        serviceRef[0] = audioService;

        // -------------------------------------------------------
        // Timer : affiche le temps écoulé en temps réel
        // -------------------------------------------------------
        Thread timerThread = new Thread(() -> {
            int secondes = 0;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    secondes++;
                    int min = secondes / 60;
                    int sec = secondes % 60;
                    System.out.printf("\r⏱️  Durée : %02d:%02d   ", min, sec);
                }
            } catch (InterruptedException e) {
                // arrêt normal
            }
        });

        // -------------------------------------------------------
        // Démarrage de l'appel
        // -------------------------------------------------------
        audioService.startCall("alice", "bob");

        timerThread.setDaemon(true);
        timerThread.start();

        System.out.println("🎙️  Parlez dans le micro... Appuyez sur ENTRÉE pour terminer.\n");

        // Attend l'appui sur Entrée
        new Scanner(System.in).nextLine();

        // -------------------------------------------------------
        // Arrêt propre
        // -------------------------------------------------------
        timerThread.interrupt();
        audioService.stopCall("alice", "bob");

        System.out.println("\n✅ Appel terminé.");
    }
}