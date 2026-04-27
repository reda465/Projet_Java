/*package com.ensa ;


import client.EcouteurClient;
import model.Utilisateur;
import service.CallAudiooService;

/**
 * Main de TEST pour l'appel audio.
 * Pas besoin de serveur : on simule un loopback local.
 *
 * Ce que ça fait :
 *  - Crée un faux ClientReseau (mode simulation, sans connexion réelle)
 *  - Lance le micro + haut-parleur via CallAudiooService
 *  - Ce que tu parles dans le micro est joué directement dans les speakers
 *  - Après 10 secondes, l'appel s'arrête automatiquement
 */
/*public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== TEST APPEL AUDIO (mode local, sans serveur) ===");

        // Test inscription
        facade.sInscrire("Dupont",  "0612345678", "pass123");
        // -------------------------------------------------------
        // 1. Créer un EcouteurClient minimal (implémentation vide)
        // -------------------------------------------------------
        EcouteurClient ecouteurFactice = new EcouteurClient() {

            @Override
            public void connexionReussie(Utilisateur moi) {
                System.out.println("[ECOUTEUR] Connexion réussie : " + moi.getNom());
            }

            @Override
            public void erreur(String message) {
                System.out.println("[ECOUTEUR] Erreur : " + message);
            }

            @Override
            public void messageRecu(String contenu) {
                System.out.println("[ECOUTEUR] Message reçu : " + contenu);
            }

            @Override
            public void deconnexion() {
                System.out.println("[ECOUTEUR] Déconnecté.");
            }

            @Override
            public void audioCallStart(String info) {
                System.out.println("[ECOUTEUR] Appel audio démarré : " + info);
            }

            @Override
            public void audioCallStop(String info) {
                System.out.println("[ECOUTEUR] Appel audio arrêté : " + info);
            }

            @Override
            public void audioRecu(byte[] audio) {
                // Dans un vrai test avec serveur, les données arrivent ici
                System.out.println("[ECOUTEUR] Audio reçu : " + audio.length + " octets");
            }

            @Override
            public void videoRecu(byte[] frameData) {

            }
        };

        // -------------------------------------------------------
        // 2. Créer un ClientReseau en mode SIMULATION (sans socket)
        //    On surcharge envoyer() pour jouer l'audio localement
        // -------------------------------------------------------
        CallAudiooService[] serviceRef = new CallAudiooService[1]; // tableau pour capture en lambda

        ClientReseauCALL clientSimule = new ClientReseauCALL(ecouteurFactice) {

            @Override
            public void envoyer(network.Packet packet) {
                // Au lieu d'envoyer sur le réseau, on joue directement en local
                if (packet.getCommande() == network.Commande.Data_AUDIO) {
                    byte[] audio = (byte[]) packet.getData();
                    if (serviceRef[0] != null) {
                        serviceRef[0].jouerAudio(audio);  // loopback : micro → speakers
                    }
                } else {
                    System.out.println("[CLIENT SIMULE] Packet ignoré : " + packet.getCommande());
                }
            }

            @Override
            public boolean isConnecte() {
                return true; // on dit qu'on est "connecté" pour ne pas bloquer
            }
        };

        // -------------------------------------------------------
        // 3. Créer le service audio et démarrer l'appel
        // -------------------------------------------------------
        CallAudiooService audioService = new CallAudiooService(clientSimule);
        serviceRef[0] = audioService;

        System.out.println("[MAIN] Démarrage de l'appel audio dans 1 seconde...");
        Thread.sleep(1000);

        audioService.startCall("alice", "bob");

        System.out.println("[MAIN] Appel en cours pendant 10 secondes... Parlez dans le micro !");
        Thread.sleep(10_000);

        // -------------------------------------------------------
        // 4. Arrêter l'appel
        // -------------------------------------------------------
        audioService.stopCall("alice", "bob");

        System.out.println("[MAIN] Test terminé.");
    }
}
 */
