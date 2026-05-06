/*package com.ensa;
import Serveur.Protocol;
import client.ClientReseau;
import client.EcouteurClient;
import model.Contact;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import network.Packet;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class ClientFileReceiverConsole {

    public static void main(String[] args) throws Exception {

        ClientReseau client = new ClientReseau(new EcouteurClient() {

            @Override
            public void connexionReussie(Utilisateur moi) {
                System.out.println("✅ CONNECTÉ : " + moi.getNumeroTelephone());
            }

            @Override public void inscriptionReussie(String msg) {}
            @Override public void erreur(String message) { System.out.println("❌ ERREUR: " + message); }
            @Override public void messageRecu(String numeroDest, String message) {}
            @Override public void conversationsRecues(List<Conversation> conversations) {}
            @Override public void messagesRecus(List<Message> messages) {}
            @Override public void contactAjoute(Contact contact) {}
            @Override public void listeContactsRecue(List<Contact> contacts) {}
            @Override public void deconnexion() {}

            @Override public void appelEntrant(String numero, String type, String ipAppelant, String ip) {}
            @Override public void appelAccepte(String numero, String ip) {}
            @Override public void appelRefuse() {}
            @Override public void appelTermine(String numero) {}

            @Override
            public void fichierRecu(String telephoneExp, String fileName, String base64) {
                try {
                    byte[] data = Base64.getDecoder().decode(base64);

                    File dossier = new File("downloads");
                    if (!dossier.exists()) dossier.mkdirs();

                    File outFile = new File(dossier, "RECU_" + fileName);

                    Files.write(outFile.toPath(), data);

                    System.out.println("📥 Fichier reçu de " + telephoneExp + " : " + outFile.getAbsolutePath());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        client.connecterAuServeur("127.0.0.1", 9091);

        Thread.sleep(1000);

        client.envoyer(new Packet(Protocol.LOGIN, "2222|pass"));

        System.out.println("📌 Client receiver prêt (2222). Attente fichiers...");
    }
}

 */