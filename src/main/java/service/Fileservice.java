package service;

import Serveur.Protocol;
import client.ClientReseau;
import network.Packet;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

    public class Fileservice {

        private ClientReseau clientReseau;

        public Fileservice(ClientReseau clientReseau) {
            this.clientReseau = clientReseau;
        }
        public void envoyerFichier(String telDest, File file) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);

                String data = telDest + "|" + file.getName() + "|" + base64;
                clientReseau.envoyer(new Packet(Protocol.FILE_SEND, data));

                System.out.println("[FILE] fichier envoyé au serveur : " + file.getName());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

