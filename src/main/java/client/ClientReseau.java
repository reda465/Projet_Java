
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
            stylo.println(packet.toString());
            System.out.println(" Envoyé : " + packet.getProtocol());
        }
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
                    if (ecouteur != null && parts.length >= 2) {
                        Groupe g = Groupe.fromNetworkString(parts[0] + ";" + parts[1]); // adapter selon format serveur
                        // Ou mieux: le serveur envoie le groupe complet sérialisé
                        ecouteur.groupeCree(g);
                    }
                    break;

                case CREATE_GROUP_FAIL:
                    if (ecouteur != null) ecouteur.creationGroupeEchouee(data);
                    break;
                case GROUPS_LIST:
                    traiterGroupesRecus(data);
                    break;

                case GROUP_MEMBERS_LIST:
                    if (parts.length >= 2) {
                        int idGroupe = Integer.parseInt(parts[0]);
                        traiterMembresGroupe(idGroupe, parts[1]);
                    }
                    break;
                case GROUP_MSG_RECEIVE:
                    if (ecouteur != null) {
                        MessageGroupe mg = MessageGroupe.fromNetworkString(data);
                        // Déterminer si c'est mon message
                        if (moi != null) mg.setEstMoi(mg.getTelephoneExpediteur().equals(moi.getNumeroTelephone()));
                        ecouteur.messageGroupeRecu(mg);
                    }
                    break;
                case ADD_MEMBER_OK:
                    if (ecouteur != null && parts.length >= 2) {
                        ecouteur.membreAjoute(Integer.parseInt(parts[0]), parts[1]);
                    }
                    break;

                case REMOVE_MEMBER_OK:
                    if (ecouteur != null && parts.length >= 2) {
                        ecouteur.membreRetire(Integer.parseInt(parts[0]), parts[1]);
                    }
                    break;
                case LEAVE_GROUP_OK:
                    if (ecouteur != null) ecouteur.aQuitteGroupe(Integer.parseInt(data));
                    break;

                case DELETE_GROUP_OK:
                    if (ecouteur != null) ecouteur.groupeSupprime(Integer.parseInt(data));
                    break;

                case UPDATE_GROUP_NAME_OK:
                    if (ecouteur != null && parts.length >= 2) {
                        ecouteur.nomGroupeModifie(Integer.parseInt(parts[0]), parts[1]);
                    }
                    break;
                default:
                    System.out.println("Protocole inconnu : " + p.getProtocol());
                    break;

            }
        }

        private void traiterGroupesRecus(String data) {
            List<Groupe> groupes = new ArrayList<>();
            if (data == null || data.isEmpty()) {
                if (ecouteur != null) ecouteur.listeGroupesRecue(groupes);
                return;
            }

            String[] grps = data.split("\\|");
            for (String g : grps) {
                if (g.isEmpty()) continue;
                try {
                    groupes.add(Groupe.fromNetworkString(g));
                } catch (Exception e) {
                    System.out.println("❌ Erreur parsing groupe: " + e.getMessage());
                }
            }
            System.out.println("👥 Groupes reçus: " + groupes.size());
            if (ecouteur != null) ecouteur.listeGroupesRecue(groupes);
        }

        // ===== TRAITER MEMBRES GROUPE =====
        private void traiterMembresGroupe(int idGroupe, String data) {
            List<Utilisateur> membres = new ArrayList<>();
            if (data == null || data.isEmpty()) {
                if (ecouteur != null) ecouteur.membresGroupeRecus(idGroupe, membres);
                return;
            }

            String[] usrs = data.split("\\|");
            for (String u : usrs) {
                if (u.isEmpty()) continue;
                String[] parts = u.split(";", -1);
                if (parts.length < 3) continue;
                Utilisateur user = new Utilisateur();
                user.setIdUtilisateur(Integer.parseInt(parts[0]));
                user.setNomComplet(parts[1]);
                user.setNumeroTelephone(parts[2]);
                user.setPhotoProfil(parts.length >= 4 && !parts[3].isEmpty() ? parts[3] : null);
                //user.setEnLigne(parts.length >= 5 && Boolean.parseBoolean(parts[4]));
                membres.add(user);
            }
            if (ecouteur != null) ecouteur.membresGroupeRecus(idGroupe, membres);
        }

        private void traiterConversationsRecues(String data) {
            List<Conversation> conversations = new ArrayList<>();

            if (data.isEmpty()) {
                if (ecouteur != null) ecouteur.conversationsRecues(conversations);
                return;
            }
            // Format : id;type;nomExp;datedernierMsg;nonLus;contenuDerniermsg|id;nom;...
            String[] convs = data.split("\\|");
            for (String c : convs) {
                if (c.isEmpty()) continue;
                String[] parts = c.split(";", 6);
                if (parts.length < 5) continue;

                Conversation conv = new Conversation();
                conv.setIdConversation(Integer.parseInt(parts[0]));
                conv.setTypeConversation(parts[1]);
                conv.setNomContact(parts[2]);
                try {
                    conv.setDateDernierMessage(java.time.LocalDateTime.parse(parts[3]));
                } catch (Exception e) {
                    conv.setDateDernierMessage(null);
                }
                conv.setMessagesNonLus(Integer.parseInt(parts[4]));
                conv.setDernierMessage(parts.length >= 6 ? parts[5] : "");
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
    }

    //fichiers
    public void envoyerFichier(String telDest, String fileName, byte[] dataBase64) {
        String contenu = telDest + "|" + fileName + "|" + new String(dataBase64);
        Packet p = new Packet(Protocol.FILE_SEND, contenu);
        envoyer(p);
    }
}