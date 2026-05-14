
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    private final ConcurrentHashMap<String, CompletableFuture<FileUploadResult>> uploadAttentes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, DownloadAssembler> telechargements = new ConcurrentHashMap<>();

    private static final class DownloadAssembler {
        int messageId;
        String nomFichier;
        long totalSize;
        int totalParts;
        String typeMessage;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    }
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

    public void enregistrerAttenteUpload(String sessionId, CompletableFuture<FileUploadResult> futur) {
        if (sessionId != null && futur != null) uploadAttentes.put(sessionId, futur);
    }

    public void demanderFichierMessage(int idMessage) {
        if (!connecte || stylo == null) return;
        envoyer(new Packet(Protocol.GET_MESSAGE_FILE, String.valueOf(idMessage)));
    }

    public void demanderFichierGroupe(int idMessage) {
        if (!connecte || stylo == null) return;
        envoyer(new Packet(Protocol.GET_GROUP_FILE, String.valueOf(idMessage)));
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
                    // Données serveur : numéro|nomComplet
                    if (parts.length >= 1 && ecouteur != null) {
                        Contact contactAjoute = new Contact();
                        contactAjoute.setNumeroTelephone(parts[0].trim());
                        contactAjoute.setNomComplet(parts.length >= 2 ? parts[1].trim() : parts[0].trim());
                        System.out.println("✅ Contact ajouté: " + contactAjoute.getNomComplet());
                        ecouteur.contactAjoute(contactAjoute);
                    }
                    break;
                case CONTACT_REQUEST:
                    if (parts.length >= 2 && ecouteur != null) {
                        ecouteur.demandeContactRecue(parts[0].trim(), parts[1].trim());
                    }
                    break;
                case CONTACT_ACCEPTED:
                    if (ecouteur == null) break;
                    if ("OK".equalsIgnoreCase(data != null ? data.trim() : "")) {
                        ecouteur.contactAcceptationConfirmee();
                    } else if (parts.length >= 2) {
                        Contact c = new Contact();
                        c.setNumeroTelephone(parts[0].trim());
                        c.setNomComplet(parts[1].trim());
                        ecouteur.contactAjoute(c);
                    }
                    break;
                case ADD_CONTACT_FAIL:
                    if (ecouteur != null) {
                        ecouteur.erreur("Échec ajout contact: " + p.getData());
                    }
                    break;
                    //fichier
                case FILE_RECEIVE:
                    if (parts.length >= 3) {
                        String telExp = parts[0];
                        String fileName = parts[1];
                        String base64 = parts[2];

                        if (ecouteur != null) {
                            ecouteur.fichierRecu(telExp, fileName, base64);
                        }
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
                case FILE_UPLOAD_OK:
                    if (parts.length >= 3) {
                        String sid = parts[0];
                        String mode = parts[1];
                        try {
                            int idMsg = Integer.parseInt(parts[2].trim());
                            CompletableFuture<FileUploadResult> f = uploadAttentes.remove(sid);
                            if (f != null) f.complete(FileUploadResult.ok(mode, idMsg));
                        } catch (Exception e) {
                            CompletableFuture<FileUploadResult> f = uploadAttentes.remove(parts[0]);
                            if (f != null) f.complete(FileUploadResult.fail("PARSE"));
                        }
                    }
                    break;
                case FILE_UPLOAD_FAIL:
                    if (parts.length >= 1) {
                        String sid = parts[0];
                        String err = parts.length >= 2 ? parts[1] : "FAIL";
                        CompletableFuture<FileUploadResult> f = uploadAttentes.remove(sid);
                        if (f != null) f.complete(FileUploadResult.fail(err));
                    }
                    break;
                case MSG_FILE_NOTIFY:
                    if (parts.length >= 6 && ecouteur != null) {
                        try {
                            String tel = parts[0];
                            int idMsg = Integer.parseInt(parts[1].trim());
                            String nom = new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8);
                            String type = parts[3];
                            long taille = Long.parseLong(parts[4].trim());
                            String legende = "";
                            if (parts.length >= 6) {
                                try {
                                    legende = new String(Base64.getDecoder().decode(parts[5]), StandardCharsets.UTF_8);
                                } catch (Exception ignored) {
                                }
                            }
                            ecouteur.messageFichierNotifie(tel, idMsg, nom, type, taille, legende);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case FILE_TRANSFER_BEGIN:
                    traiterTransferBegin(parts);
                    break;
                case FILE_TRANSFER_PART:
                    traiterTransferPart(parts);
                    break;
                case FILE_TRANSFER_END:
                    traiterTransferEnd(parts);
                    break;
                case FILE_TRANSFER_FAIL:
                    if (parts.length >= 1 && ecouteur != null) {
                        try {
                            int mid = Integer.parseInt(parts[0].trim());
                            String raison = parts.length >= 2 ? parts[1] : "";
                            telechargements.remove(mid);
                            ecouteur.fichierTelechargeErreur(mid, raison);
                        } catch (Exception ignored) {
                        }
                    }
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
                    if (champs.length >= 9) {
                        msg.setTypeMessage(champs[5] != null && !champs[5].isBlank() ? champs[5] : "texte");
                        msg.setNomFichier(champs[6] != null && !champs[6].isBlank() ? champs[6] : null);
                        try {
                            msg.setTailleFichier(champs[7] != null && !champs[7].isBlank()
                                    ? Long.parseLong(champs[7].trim()) : null);
                        } catch (NumberFormatException nfe) {
                            msg.setTailleFichier(null);
                        }
                        msg.setUrlFichier("texte".equals(msg.getTypeMessage()) ? null : "stored");
                    } else {
                        msg.setTypeMessage("texte");
                        msg.setNomFichier(null);
                        msg.setTailleFichier(null);
                        msg.setUrlFichier(null);
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
                int nb = 0;
                try { nb = Integer.parseInt(champs[3]); } catch (Exception ignored) {}
                if (champs.length > 4) {
                    for (int i = 4; i < champs.length; i++) {
                        if (!champs[i].isEmpty()) membres.add(champs[i]);
                    }
                } else {
                    for (int i = 0; i < nb; i++) membres.add("");
                }
                g.setNumerosMembres(membres);
                groupes.add(g);
            }
            if (ecouteur != null) ecouteur.listeGroupesRecue(groupes);
        }

        private void traiterMessagesGroupeRecus(String data) {
            if (data == null || data.isEmpty()) return;
            String[] lignes = data.split("\\|");
            for (String ligne : lignes) {
                String[] champs = ligne.split(";", -1);
                if (champs.length < 5) continue;
                MessageGroupe msg = new MessageGroupe();
                if (champs.length >= 6) {
                    try { msg.setIdMessage(Integer.parseInt(champs[0])); } catch (Exception ignored) {}
                    try { msg.setIdGroupe(Integer.parseInt(champs[1])); } catch (Exception ignored) {}
                    msg.setTelephoneExpediteur(champs[2]);
                    msg.setNomExpediteur(champs[3]);
                    msg.setContenu(champs[4]);
                    try { msg.setDateEnvoi(LocalDateTime.parse(champs[5])); } catch (Exception e) { msg.setDateEnvoi(null); }
                } else {
                    try { msg.setIdMessage(Integer.parseInt(champs[0])); } catch (Exception ignored) {}
                    msg.setIdGroupe(dernierIdGroupeDemande);
                    msg.setTelephoneExpediteur(champs[1]);
                    msg.setNomExpediteur(champs[2]);
                    msg.setContenu(champs[3]);
                    try { msg.setDateEnvoi(LocalDateTime.parse(champs[4])); } catch (Exception e) { msg.setDateEnvoi(null); }
                }
                if (ecouteur != null) ecouteur.messageGroupeRecu(msg);
            }
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

        private void traiterTransferBegin(String[] parts) {
            if (parts.length < 5) return;
            try {
                int messageId = Integer.parseInt(parts[0].trim());
                String nom = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                long size = Long.parseLong(parts[2].trim());
                int total = Integer.parseInt(parts[3].trim());
                String type = parts.length >= 5 ? parts[4] : "document";
                DownloadAssembler da = new DownloadAssembler();
                da.messageId = messageId;
                da.nomFichier = nom;
                da.totalSize = size;
                da.totalParts = total;
                da.typeMessage = type;
                telechargements.put(messageId, da);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void traiterTransferPart(String[] parts) {
            if (parts.length < 3) return;
            try {
                int messageId = Integer.parseInt(parts[0].trim());
                DownloadAssembler da = telechargements.get(messageId);
                if (da == null) return;
                StringBuilder b64 = new StringBuilder(parts[2] != null ? parts[2] : "");
                for (int i = 3; i < parts.length; i++) {
                    b64.append('|').append(parts[i] != null ? parts[i] : "");
                }
                byte[] chunk = Base64.getDecoder().decode(b64.toString());
                da.buffer.write(chunk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void traiterTransferEnd(String[] parts) {
            if (parts.length < 1 || ecouteur == null) return;
            try {
                int messageId = Integer.parseInt(parts[0].trim());
                DownloadAssembler da = telechargements.remove(messageId);
                if (da == null) return;
                da.buffer.flush();
                byte[] all = da.buffer.toByteArray();
                if (da.totalSize > 0 && all.length != da.totalSize) {
                    ecouteur.fichierTelechargeErreur(messageId, "SIZE");
                    return;
                }
                File outDir = new File("downloads");
                outDir.mkdirs();
                String safe = da.nomFichier.replaceAll("[\\\\/:*?\"<>|]", "_");
                File out = new File(outDir, "msg_" + messageId + "_" + safe);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(all);
                }
                ecouteur.fichierTelecharge(messageId, out, da.typeMessage);
            } catch (Exception e) {
                e.printStackTrace();
                if (ecouteur != null) ecouteur.fichierTelechargeErreur(Integer.parseInt(parts[0].trim()), e.getMessage());
            }
        }
    }

    //fichiers
    public void envoyerFichier(String telDest, String fileName, byte[] dataBase64) {
        String contenu = telDest + "|" + fileName + "|" + new String(dataBase64);
        Packet p = new Packet(Protocol.FILE_SEND, contenu);
        envoyer(p);
    }
}