/*
package com.ensa;

import client.ClientReseauCALL;
import client.EcouteurClient;
import model.CallVideo;
import model.Utilisateur;
import network.Commande;
import network.Packet;
import service.CallVideoService;

import java.util.Scanner;

    public class MainTestVideo {

        public static void main(String[] args) throws InterruptedException {

            System.out.println("=== TEST APPEL VIDEO (loopback local, sans serveur) ===\n");

            // Référence partagée au service vidéo
            CallVideoService[] serviceRef = new CallVideoService[1];

            // -------------------------------------------------------
            // EcouteurClient — reçoit les frames et les passe au service
            // -------------------------------------------------------
            EcouteurClient ecouteur = new EcouteurClient() {

                @Override public void connexionReussie(Utilisateur moi) {}
                @Override public void erreur(String message) {
                    System.out.println("[ERREUR] " + message);
                }
                @Override public void messageRecu(String contenu) {}
                @Override public void deconnexion() {}
                @Override public void audioCallStart(String info) {}
                @Override public void audioCallStop(String info) {}
                @Override public void audioRecu(byte[] audio) {}

                // Reçoit une frame et l'affiche via le service
                public void videoRecu(byte[] frameData) {
                    if (serviceRef[0] != null) {
                        serviceRef[0].afficherFrame(frameData);
                    }
                }
            };

            // -------------------------------------------------------
            // ClientReseau simulé — loopback vidéo sans serveur
            // -------------------------------------------------------
            ClientReseauCALL clientSimule = new ClientReseauCALL(ecouteur) {

                @Override
                public void envoyer(Packet packet) {
                    if (packet.getCommande() == Commande.Data_Appel_Video) {
                        // Loopback : frame envoyée → affichée directement
                        byte[] frame = (byte[]) packet.getData();
                        ecouteur.videoRecu(frame);
                    }
                    // Debuter et Arreter ignorés en local
                }

                @Override
                public boolean isConnecte() {
                    return true;
                }
            };

            // -------------------------------------------------------
            // Créer les utilisateurs et le CallVideo
            // -------------------------------------------------------
            Utilisateur alice = new Utilisateur();
            alice.setId(1);
            alice.setNom("Alice");
            alice.setPrenom("Alice");

            Utilisateur bob = new Utilisateur();
            bob.setId(2);
            bob.setNom("Bob");
            bob.setPrenom("Bob");

            CallVideo call = new CallVideo(alice, bob);

            // -------------------------------------------------------
            // Créer le service et démarrer l'appel
            // -------------------------------------------------------
            CallVideoService videoService = new CallVideoService(clientSimule);
            serviceRef[0] = videoService;

            videoService.startCall(call);

            // -------------------------------------------------------
            // Timer dans la console
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

            timerThread.setDaemon(true);
            timerThread.start();

            System.out.println("📹 Appel en cours... Appuyez sur ENTRÉE pour terminer.\n");

            // Attend l'appui sur Entrée
            new Scanner(System.in).nextLine();

            // -------------------------------------------------------
            // Arrêt propre
            // -------------------------------------------------------
            timerThread.interrupt();
            videoService.stopCall(call);

            System.out.println("\n✅ Appel vidéo terminé.");
        }


    }*/