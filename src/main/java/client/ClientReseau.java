
package client;

import Serveur.Protocol;
import lombok.Getter;
import lombok.Setter;
import network.*;
import model.*;
import service.CallService;
import service.MessageService;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
//import Serveur.*;
@Setter
@Getter
public class ClientReseau {
    private Socket tuyau;
    private PrintWriter stylo;
    private BufferedReader yeux;
    private boolean connecte = false;

    private EcouteurClient ecouteur;
    private Utilisateur moi;
    private MessageService messageService;
    private CallService callService;
    private int dernierIdGroupeDemande = -1;


    public ClientReseau(EcouteurClient ecouteur){//lier a l'interface graphique pour les signales
        this.ecouteur = ecouteur;
    }
    public MessageService getMessageService() {
        if (messageService == null) {
            messageService = new MessageService(this);
        }
        return messageService;
    }
    // ===== CONNEXION NORMALE =====
    public void connecterAuServeur(String ip, int port) {
        try {
            tuyau = new Socket(ip, port);
            stylo = new PrintWriter(tuyau.getOutputStream(), true);
            yeux = new BufferedReader(new InputStreamReader(tuyau.getInputStream()));
            System.out.println("Connexion au serveur " + ip + ":" + port);
            connecte = true;
            System.out.println("✅ Connecté au serveur");
            Thread ami = new Thread(new Ecouteur());
            ami.start();

        } catch (IOException e) {
            connecte = false;
            System.out.println(" Erreur connexion : " + e.getMessage());


            if (ecouteur != null) {//ecouteur est null signifie que y'a pas de liaison entre UI et Client
                ecouteur.erreur("Impossible de se connecter au serveur");
            }
        }
    }

    public void demanderMessages(int idConversation) {
        if (!connecte) {
            System.out.println("❌ Pas connecté au serveur");
            return;
        }
        Packet p = new Packet(Protocol.GET_MESSAGES, String.valueOf(idConversation));
        envoyer(p);
    }
    // ===== ENVOYER =====
    public void envoyer(Packet packet) {
        if (!connecte) {//erreur du connection
            return;
        }
        if (stylo != null) {
            if (packet.getProtocol() == Protocol.GET_GROUP_MESSAGES) {
                try { dernierIdGroupeDemande = Integer.parseInt(packet.getData()); } catch (Exception ignored) {}
            }
            stylo.println(packet.toString());
            System.out.println(" Envoyé : " + packet.getProtocol());
        }
    }

    public void demanderContacts() {
        if (!connecte || stylo == null) {
            System.out.println("❌ Pas connecté au serveur");
            return;
        }
        Packet p = new Packet(Protocol.GET_CONTACTS, "");
        envoyer(p);
    }

    public void demanderConversations() {
        if (!connecte || stylo == null) {
            System.out.println("❌ Pas connecté au serveur");
            return;
        }
        Packet p = new Packet(Protocol.GET_CONVERSATIONS, "");
        envoyer(p);
    }
    // ===== DÉCONNEXION =====
    public void deconnecter() {
        connecte = false;
        try{
        if (tuyau != null) { tuyau.close();}
        } catch (IOException e) {}
        if (ecouteur != null) {
            ecouteur.deconnexion();
        }
    }
    // ===== CLASSE INTERNE : ÉCOUTEUR RÉSEAU =====
    private class Ecouteur implements Runnable {

        public void run() {
            try {
                String ligne;
                while (connecte && (ligne = yeux.readLine()) != null) {
                    Packet recu = Packet.fromString(ligne);
                    traiterPacket(recu);
                }
            } catch (IOException e) {
                connecte = false;
                if (ecouteur != null) {
                    ecouteur.deconnexion();
                }
            }
        }

        private void traiterPacket(Packet p) {
            String data = p.getData();
            String[] parts = data.split("\\|", -1); // -1 pour garder les champs vides
            switch (p.getProtocol()) {
                case LOGIN_OK:
                    if (parts.length >= 2) {
                        moi = new Utilisateur();
                        moi.setNomComplet(parts[0]);
                        moi.setNumeroTelephone(parts[1]);
                    if (ecouteur != null) {
                        ecouteur.connexionReussie(moi);
                    }
                    System.out.println("Connecté : " + moi.getNomComplet());
                    }
                    //ClientHandlerAuth.demanderConversations();
                    break;

                case LOGIN_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;

                case REGISTER_OK:
                    if (parts.length >= 2) {
                        moi = new Utilisateur();
                        moi.setNomComplet(parts[0]);
                        moi.setNumeroTelephone(parts[1]);
                        System.out.println("Inscrit : " + moi.getNomComplet());
                        if (ecouteur != null) ecouteur.inscriptionReussie(moi.getNomComplet());
                    }
                    break;
                case REGISTER_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur((String) p.getData());
                    }
                    break;
                case MSG_RECEIVE:
                    if (parts.length >= 2) {
                        Message msg = new Message() {
                            @Override
                            public String toNetworkString() {
                                return "";
                            }
                        };
                        msg.setTelephoneExpediteur(parts[0]);
                        msg.setContenuTexte(parts[1]);
                        System.out.println("Message " + msg.getContenuTexte());
                        if (ecouteur != null) ecouteur.messageRecu(msg.getTelephoneExpediteur(), msg.getContenuTexte());
                    }
                    break;
                case CALL_REQUEST:
                    if (parts.length >= 5 && ecouteur != null) {
                        String numAppelant = parts[0];
                        String nomAppelant = parts[1];
                        String typeAppel = parts[2];
                        String ipAppelant  = parts[4];
                        ecouteur.appelEntrant(numAppelant, typeAppel, ipAppelant,nomAppelant);
                        System.out.println("Appel entrant de " + nomAppelant + " (" + numAppelant + ")  ip=" + ipAppelant);
                        if (callService != null) {
                            callService.recevoirAppel(numAppelant, nomAppelant, typeAppel, ipAppelant);
                        }
                    }
                    break;
                case CALL_ACCEPT:
                    // Format serveur : telephoneAccepteur|ipAccepteur
                    if (parts.length >= 2 && ecouteur != null) {

                        String numeroAccepteur = parts[0];
                        String ipAccepteur = parts[1];

                        if (callService != null) {
                            callService.onAccepte(ipAccepteur);
                        }

                        ecouteur.appelAccepte(numeroAccepteur, ipAccepteur);
                    }
                    break;
                case CALL_REFUSE:
                    if (ecouteur != null) ecouteur.appelRefuse();
                    if (callService != null) callService.onTermine();
                    break;
                case CALL_END:
                    if (parts.length >= 1 && ecouteur != null) {
                        String telephone = parts[0];
                        ecouteur.appelTermine(telephone);
                        if (callService != null) callService.onTermine();
                    }
                    break;
                case CONVERSATIONS_LIST:
                    traiterConversationsRecues(data);
                    break;
                case MESSAGES_LIST:
                    traiterMessagesRecus(data);
                    break;
                case ADD_CONTACT_OK:
                    if (parts.length >= 3 && ecouteur != null) {
                        Contact contactAjoute = new Contact();
                        contactAjoute.setNomComplet(parts[1]);
                        contactAjoute.setNumeroTelephone(parts[0]);
                        System.out.println("✅ Contact ajouté: " + contactAjoute.getNomComplet());
                        ecouteur.contactAjoute(contactAjoute);
                    }
                    break;
                case ADD_CONTACT_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur("Échec ajout contact: " + p.getData());
                    }
                    break;
                case CONTACT_REQUEST:
                    if (parts.length >= 2 && ecouteur != null) {
                        String numDemandeur = parts[0];
                        String nomDemandeur = parts.length >= 2 ? parts[1] : "";                        System.out.println("[CONTACT] Demande reçue de " + nomDemandeur + " (" + numDemandeur + ")");
                        System.out.println("[CONTACT] Demande reçue de " + nomDemandeur + " (" + numDemandeur + ")");
                        ecouteur.demandeContactRecue(numDemandeur, nomDemandeur);                    }
                    break;
                    //fichier
                case FILE_RECEIVE:
                    if (parts.length >= 3 && ecouteur != null) {
                        String telExp = parts[0];
                        String type;
                        String fileName;
                        String base64;
                        if (parts.length >= 4 && isFileType(parts[1])) {
                            type = parts[1];
                            fileName = parts[2];
                            base64 = parts[3];
                            for (int i = 4; i < parts.length; i++) base64 += "|" + parts[i];
                        } else {
                            type = "fichier";
                            fileName = parts[1];
                            base64 = parts[2];
                            for (int i = 3; i < parts.length; i++) base64 += "|" + parts[i];
                        }
                        ecouteur.fichierRecu(telExp, type, fileName, base64);
                    }
                    break;
                case FILE_GROUP_RECEIVE:
                    if (parts.length >= 6 && ecouteur != null) {
                        int idG = Integer.parseInt(parts[0]);
                        String telExp = parts[1];
                        String nomExp = parts[2];
                        String type = parts[3];
                        String fileName = parts[4];
                        String base64 = parts[5];
                        for (int i = 6; i < parts.length; i++) base64 += "|" + parts[i];
                        ecouteur.fichierGroupeRecu(idG, telExp, nomExp, type, fileName, base64);
                    }
                    break;
                case FILE_OK:
                    if (ecouteur != null) {
                        String name = parts.length > 0 ? parts[parts.length - 1] : "";
                        ecouteur.fichierEnvoiReussi(name);
                        ecouteur.fichierProgress(100);
                    }
                    break;
                case FILE_FAIL:
                    if (ecouteur != null) {
                        ecouteur.fichierEnvoiEchoue(data != null ? data : "Échec envoi");
                    }
                    break;
                case FILE_PROGRESS:
                    if (ecouteur != null && parts.length >= 1) {
                        try {
                            ecouteur.fichierProgress(Integer.parseInt(parts[0]));
                        } catch (NumberFormatException ignored) {}
                    }
                    break;
                case CREATE_GROUP_OK:
                    traiterCreateGroupOk(data);
                    break;
                case CREATE_GROUP_FAIL:
                    if (ecouteur != null) ecouteur.creationGroupeEchouee(data);
                    break;
                case GROUPS_LIST:
                    traiterGroupesRecus(data);
                    break;
                case GROUP_MESSAGES_LIST:
                    traiterMessagesGroupeRecus(data);
                    break;
                case GROUP_MESSAGE_RECEIVE:
                    traiterMessageGroupeRecu(parts);
                    break;
                case ADD_GROUP_MEMBER_OK:
                    if (parts.length >= 2 && ecouteur != null) {
                        ecouteur.membreAjoute(Integer.parseInt(parts[0]), parts[1]);
                    }
                    break;
                case ADD_GROUP_MEMBER_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur("Ajout membre au groupe : " + (data != null ? data : ""));
                    }
                    break;
                case REMOVE_GROUP_MEMBER_OK:
                    if (parts.length >= 2 && ecouteur != null) {
                        ecouteur.membreRetire(Integer.parseInt(parts[0]), parts[1]);
                    }
                    break;
                case REMOVE_GROUP_MEMBER_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur("Retrait membre du groupe : " + (data != null ? data : ""));
                    }
                    break;
                case QUIT_GROUP_OK:
                    if (parts.length >= 1 && ecouteur != null) ecouteur.aQuitteGroupe(Integer.parseInt(parts[0]));
                    break;
                case DELETE_GROUP_OK:
                    if (parts.length >= 1 && ecouteur != null) ecouteur.groupeSupprime(Integer.parseInt(parts[0]));
                    break;
                case RENAME_GROUP_OK:
                    if (parts.length >= 2 && ecouteur != null) ecouteur.nomGroupeModifie(Integer.parseInt(parts[0]), parts[1]);
                    break;
                case JOIN_GROUP_CALL:
                    if (parts.length >= 7 && ecouteur != null) {
                        int idGroupe = Integer.parseInt(parts[0]);
                        String numeroMembre = parts[1];
                        String nomMembre = parts[2];
                        String ip = parts[3];
                        String type = parts[4];
                        int port = Integer.parseInt(parts[5]);
                        int portAudio;
                        boolean isReply;
                        if (parts.length >= 8) {
                            portAudio = Integer.parseInt(parts[6]);
                            isReply = "1".equals(parts[7]);
                        } else {
                            portAudio = "VIDEO".equalsIgnoreCase(type) ? port + 1 : port;
                            isReply = "1".equals(parts[6]);
                        }
                        ecouteur.membreRejointAppelGroupe(idGroupe, numeroMembre, nomMembre, ip, type, port, portAudio, isReply);
                    }
                    break;
                case LEAVE_GROUP_CALL:
                    if (parts.length >= 2 && ecouteur != null) {
                        int idGroupe = Integer.parseInt(parts[0]);
                        String numeroMembre = parts[1];
                        ecouteur.membreQuitteAppelGroupe(idGroupe, numeroMembre);
                    }
                    break;
                case USERS_LIST:
                    // Optionnel : gérer la liste des utilisateurs connectés
                    break;
                default:
                    System.out.println("Protocole inconnu : " + p.getProtocol());
                    break;

            }
        }

        private void traiterConversationsRecues(String data) {
            List<Conversation> conversations = new ArrayList<>();

            if (data == null || data.isEmpty()) {
                if (ecouteur != null) ecouteur.conversationsRecues(conversations);
                return;
            }
            // Format v2 : id;type;nom;numeroTel;date;nonLus;dernierMsg|
            // Ancien : id;type;nom;date;nonLus;dernierMsg|
            String[] convs = data.split("\\|");
            for (String c : convs) {
                if (c.isEmpty()) continue;
                String[] parts = c.split(";", -1);
                if (parts.length < 6) continue;

                Conversation conv = new Conversation();
                conv.setIdConversation(Integer.parseInt(parts[0].trim()));
                conv.setTypeConversation(parts[1].trim());
                conv.setNomContact(parts[2].trim());

                if (parts.length >= 7) {
                    conv.setNumeroContact(parts[3].trim());
                    try {
                        conv.setDateDernierMessage(java.time.LocalDateTime.parse(parts[4]));
                    } catch (Exception e) {
                        conv.setDateDernierMessage(null);
                    }
                    conv.setMessagesNonLus(Integer.parseInt(parts[5].trim()));
                    if (parts.length > 7) {
                        StringBuilder dm = new StringBuilder(parts[6] != null ? parts[6] : "");
                        for (int pi = 7; pi < parts.length; pi++) {
                            dm.append(';').append(parts[pi] != null ? parts[pi] : "");
                        }
                        conv.setDernierMessage(dm.toString());
                    } else {
                        conv.setDernierMessage(parts[6] != null ? parts[6] : "");
                    }
                } else {
                    try {
                        conv.setDateDernierMessage(java.time.LocalDateTime.parse(parts[3]));
                    } catch (Exception e) {
                        conv.setDateDernierMessage(null);
                    }
                    conv.setMessagesNonLus(Integer.parseInt(parts[4].trim()));
                    conv.setDernierMessage(parts.length >= 6 ? parts[5] : "");
                }
                conversations.add(conv);
            }
            System.out.println("Conversations reçues : " + conversations.size());
            if (ecouteur != null) {
                ecouteur.conversationsRecues(conversations);
            }
        }

        private void traiterMessagesRecus(String data) {
            List<Message> messages = new ArrayList<>();

            if (data == null || data.isEmpty()) {
                System.out.println("📭 Aucun message dans cette conversation");
                if (ecouteur != null) ecouteur.messagesRecus(messages);
                return;
            }

            // Format : idMessage;telExp;nomExp;contenu;date|idMessage;telExp;nomExp;contenu;date|...
            String[] msgs = data.split("\\|");
            for (String m : msgs) {
                if (m == null || m.isEmpty()) continue;
                String[] champs = m.split(";", -1);
                if (champs.length < 5) continue;

                try {
                    Message msg = new Message() {
                        @Override
                        public String toNetworkString() {
                            return "";
                        }
                    };
                    msg.setIdMessage(Integer.parseInt(champs[0].trim()));
                    msg.setTelephoneExpediteur(champs[1]);
                    msg.setNomExpediteur(champs[2]);
                    msg.setContenuTexte(champs[3]);
                    try {
                        msg.setDateEnvoi(LocalDateTime.parse(champs[4]));
                    } catch (Exception e) {
                        msg.setDateEnvoi(null);
                    }
                    if (champs.length >= 6) {
                        msg.setTypeMessage(champs[5]);
                        msg.setNomFichier(champs[6]);
                    }
                    if (champs.length >= 8 && champs[7] != null && !champs[7].isEmpty()) {
                        msg.setUrlFichier(champs[7]);
                    }
                    // Déterminer si c'est un message envoyé ou reçu
                    msg.setEstMoi(moi != null && champs[1].equals(moi.getNumeroTelephone()));
                    messages.add(msg);
                } catch (NumberFormatException e) {
                    System.out.println(" Erreur parsing message : " + e.getMessage());
                }
            }
            System.out.println(" Messages reçus : " + messages.size());
            if (ecouteur != null) {
                ecouteur.messagesRecus(messages);
            }
        }

        private void traiterCreateGroupOk(String data) {
            try {
                String[] parts = data.split("\\|", -1);
                Groupe groupe = new Groupe();
                if (parts.length > 0) groupe.setIdGroupe(Integer.parseInt(parts[0]));
                if (parts.length > 1) groupe.setNomGroupe(parts[1]);
                if (parts.length > 2) groupe.setNumeroCreateur(parts[2]);
                if (parts.length > 3) groupe.setDateCreation(parts[3]);
                List<String> membres = new ArrayList<>();
                if (parts.length > 4) {
                    String[] nums = parts[4].split(";", -1);
                    for (String num : nums) if (!num.isEmpty()) membres.add(num);
                }
                groupe.setNumerosMembres(membres);
                if (ecouteur != null) ecouteur.groupeCree(groupe);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void traiterGroupesRecus(String data) {
            List<Groupe> groupes = new ArrayList<>();
            if (data == null || data.isEmpty()) {
                if (ecouteur != null) ecouteur.listeGroupesRecue(groupes);
                return;
            }
            String[] lignes = data.split("\\|");
            for (String ligne : lignes) {
                String[] champs = ligne.split(";", -1);
                if (champs.length < 4) continue;
                Groupe g = new Groupe();
                g.setIdGroupe(Integer.parseInt(champs[0]));
                g.setNomGroupe(champs[1]);
                g.setNumeroCreateur(champs[2]);
                List<String> membres = new ArrayList<>();
                for (int i = 4; i < champs.length; i++) {
                    if (!champs[i].isEmpty()) membres.add(champs[i].trim());
                }
                g.setNumerosMembres(membres);
                groupes.add(g);
            }
            if (ecouteur != null) ecouteur.listeGroupesRecue(groupes);
        }

        private void traiterMessagesGroupeRecus(String data) {
            int idGroupe = dernierIdGroupeDemande;
            if (ecouteur != null) ecouteur.debutHistoriqueGroupe(idGroupe);
            if (data == null || data.isEmpty()) {
                if (ecouteur != null) ecouteur.finHistoriqueGroupe(idGroupe);
                return;
            }
            // Format serveur : id;tel;nom;contenu;date[;base64Fichier]
            String[] lignes = data.split("\\|");
            for (String ligne : lignes) {
                if (ligne == null || ligne.isEmpty()) continue;
                String[] champs = ligne.split(";", -1);
                if (champs.length < 5) continue;

                MessageGroupe msg = new MessageGroupe();
                try { msg.setIdMessage(Integer.parseInt(champs[0].trim())); } catch (Exception ignored) {}
                msg.setIdGroupe(idGroupe);
                msg.setTelephoneExpediteur(champs[1]);
                msg.setNomExpediteur(champs[2]);
                msg.setContenu(champs[3]);
                try {
                    msg.setDateEnvoi(LocalDateTime.parse(champs[4]));
                } catch (Exception e) {
                    msg.setDateEnvoi(null);
                }
                String b64File = champs.length >= 6 ? champs[5] : null;

                if (ecouteur == null) continue;
                if (b64File != null && !b64File.isEmpty()
                        && util.FileMediaUtil.isGroupFileContent(msg.getContenu())) {
                    String[] meta = util.FileMediaUtil.parseGroupFileContent(msg.getContenu());
                    if (meta != null) {
                        ecouteur.fichierGroupeRecu(idGroupe, msg.getTelephoneExpediteur(),
                                msg.getNomExpediteur(), meta[0], meta[1], b64File, champs[4]);
                        continue;
                    }
                }
                ecouteur.messageGroupeRecu(msg);
            }
            if (ecouteur != null) ecouteur.finHistoriqueGroupe(idGroupe);
        }

        private void traiterMessageGroupeRecu(String[] parts) {
            if (parts.length < 5) return;
            MessageGroupe msg = new MessageGroupe();
            msg.setIdGroupe(Integer.parseInt(parts[0]));
            msg.setTelephoneExpediteur(parts[1]);
            msg.setNomExpediteur(parts[2]);
            if (parts.length == 5) {
                msg.setContenu(parts[3]);
                try { msg.setDateEnvoi(LocalDateTime.parse(parts[4])); } catch (Exception e) { msg.setDateEnvoi(null); }
            } else {
                StringBuilder c = new StringBuilder(parts[3] != null ? parts[3] : "");
                for (int i = 4; i < parts.length - 1; i++) {
                    c.append('|').append(parts[i] != null ? parts[i] : "");
                }
                msg.setContenu(c.toString());
                try { msg.setDateEnvoi(LocalDateTime.parse(parts[parts.length - 1])); } catch (Exception e) { msg.setDateEnvoi(null); }
            }
            if (ecouteur != null) ecouteur.messageGroupeRecu(msg);
        }
    }
    private static boolean isFileType(String s) {
        return "image".equals(s) || "video".equals(s) || "audio".equals(s) || "fichier".equals(s);
    }

    public void notifierEchecFichier(String message) {
        if (ecouteur != null) ecouteur.fichierEnvoiEchoue(message);
    }
}