package Serveur;

import lombok.Getter;
import lombok.Setter;
import model.Contact;
import model.Conversation;
import model.Groupe;
import model.Message;
import model.MessageGroupe;
import model.Utilisateur;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import Dao.*;
@Getter
@Setter

public class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter pw;
    private String telephoneConnecte; // null = pas encore authentifié
    private final MessageRouter messageRouter = MessageRouter.getInstance();
    private final CallManager callManager = CallManager.getInstance();
    private final UserManager      userManager = UserManager.getInstance();
    private final  Dao_UtilisateurImp userDAO   = new Dao_UtilisateurImp();
    private final Dao_ContactImp contactDAO = new Dao_ContactImp();
    // Dans les attributs de ClientHandler — ajouter ces deux
    private final DaoConversationImp convDAO    = new DaoConversationImp();
    private final Dao_MessageImp     messageDAO = new Dao_MessageImp();
    private final Dao_MessageFileAttenteImp fileDAO = new Dao_MessageFileAttenteImp();
    private final Dao_GroupeImp groupeDAO = new Dao_GroupeImp();
    private final Dao_MessageGroupeImp messageGroupeDAO = new Dao_MessageGroupeImp();
    public ClientHandler(Socket s) {
        this.socket = s;
    }

    // ── BOUCLE PRINCIPALE ────────────────────────────────────────────────────
    @Override
    public void run() {
        try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw  = new PrintWriter(socket.getOutputStream(), true);

            String ligne;

            while ((ligne = br.readLine()) != null) {
                System.out.println("[RECU] " + ligne);
                String[] parts    = ligne.split("\\|", -1);

                switch (Protocol.valueOf(parts[0])) {
                    case Protocol.LOGIN    -> handleLogin(parts);
                    case Protocol.REGISTER -> handleRegister(parts);
                    case Protocol.LOGOUT   -> { handleLogout(); return; }
                    case Protocol.MSG_SEND -> handleMessage(parts);
                    case Protocol.GET_CONVERSATIONS -> handleGetConversations();
                    case Protocol.GET_MESSAGES -> handleGetMessages(parts);
                    case CALL_REQUEST -> handleCallRequest(parts);
                    case CALL_ACCEPT  -> handleCallAccept(parts);
                    case CALL_REFUSE  -> handleCallRefuse(parts);
                    case CALL_END     -> handleCallEnd(parts);
                    case ADD_CONTACT      -> handleAddContact(parts);
                    case BLOCK_CONTACT    -> handleBlockContact(parts);
                    case CONTACT_ACCEPTED -> handleContactAccepted(parts);
                    //case CALL_CANCEL  -> handleCallCancel(parts);
                   //fichier
                    case FILE_SEND -> handleFileSend(parts);
                    case CREATE_GROUP -> handleCreateGroup(parts);
                    case GET_GROUPS -> handleGetGroups(parts);
                    case SEND_GROUP_MESSAGE -> handleSendGroupMessage(parts);
                    case GET_GROUP_MESSAGES -> handleGetGroupMessages(parts);
                    case ADD_GROUP_MEMBER -> handleAddGroupMember(parts);
                    case REMOVE_GROUP_MEMBER -> handleRemoveGroupMember(parts);
                    case QUIT_GROUP -> handleQuitGroup(parts);
                    case DELETE_GROUP -> handleDeleteGroup(parts);
                    case RENAME_GROUP -> handleRenameGroup(parts);

                    default                -> pw.println("UNKNOWN_COMMAND");
                }
            }

        } catch (IOException e) {
            System.out.println("Connexion perdue : " + telephoneConnecte);
        } finally {
            handleLogout();
        }
    }


    // ── LOGIN|numero_telephone|mot_de_passe ──────────────────────────────────
    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            pw.println(Protocol.LOGIN_FAIL + "|Format_invalide");
            return;
        }

        String tel      = parts[1];
        String password = parts[2];

        try {
            Utilisateur u = userDAO.findByTelAndPassword(tel, password);
            if (u != null) {
                telephoneConnecte = u.getNumeroTelephone();
                userManager.addUser(telephoneConnecte, this);
                userDAO.updateDerniereConnexion(u.getIdUtilisateur());

                pw.println(Protocol.LOGIN_OK + "|"
                        + u.getNomComplet() + "|"
                        + u.getNumeroTelephone());

                broadcastUsersList();
                messageRouter.delivrerMessagesEnAttente(telephoneConnecte);

                // ← NOUVEAU : délivrer les demandes de contact en attente
                delivrerDemandesContactEnAttente(u);

            } else {
                pw.println(Protocol.LOGIN_FAIL + "|ErreurLogin");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.LOGIN_FAIL + "|ErreurServeur");
        }
    }

    // ── Délivrer les CONTACT_REQUEST en attente ───────────────────────────────
    private void delivrerDemandesContactEnAttente(Utilisateur u) {
        try {
            List<Contact> demandes = contactDAO.getDemandesEnAttente(
                    u.getIdUtilisateur());

            for (Contact demande : demandes) {
                Utilisateur demandeur = userDAO.getByID(
                        demande.getIdContactUtilisateur());
                if (demandeur == null) continue;

                pw.println(Protocol.CONTACT_REQUEST.name()
                        + "|" + demandeur.getNumeroTelephone()
                        + "|" + demandeur.getNomComplet());

                System.out.println("[CONTACT] Demande en attente livrée à "
                        + u.getNumeroTelephone()
                        + " de " + demandeur.getNumeroTelephone());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── REGISTER|nom_complet|numero_telephone|mot_de_passe ───────────────────
    private void handleRegister(String[] parts) {
        if (parts.length < 4) { pw.println("REGISTER_FAIL|Erreur_Inscription"); return; }

        Utilisateur u = new Utilisateur();
        u.setNomComplet(parts[1]);
        u.setNumeroTelephone(parts[2]);
        u.setMotDePasse(parts[3]);

        try {
            if (userDAO.telephoneExiste(u.getNumeroTelephone())) {
                pw.println("REGISTER_FAIL|TELEPHONE_EXISTE");
                return;
            }
            userDAO.Add(u);
            
            // Auto-login après inscription
            Utilisateur newUser = userDAO.findByTelephone(u.getNumeroTelephone());
            if (newUser != null) {
                telephoneConnecte = newUser.getNumeroTelephone();
                userManager.addUser(telephoneConnecte, this);
                userDAO.updateDerniereConnexion(newUser.getIdUtilisateur());
                
                pw.println(Protocol.REGISTER_OK + "|" + newUser.getNomComplet() + "|" + newUser.getNumeroTelephone());
                broadcastUsersList();
            } else {
                pw.println(Protocol.REGISTER_OK + "|" + u.getNomComplet() + "|" + u.getNumeroTelephone());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            pw.println("REGISTER_FAIL|Erreur_Inscription");
        }
    }

    // ── LOGOUT ───────────────────────────────────────────────────────────────
    private void handleLogout() {
        if (telephoneConnecte != null) {
            userManager.removeUser(telephoneConnecte);
            System.out.println("Déconnexion : " + telephoneConnecte);
            broadcastUsersList();
            telephoneConnecte = null;
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ── MSG_SEND — sprint suivant ─────────────────────────────────────────────
        private void handleMessage(String[] parts) {
            if (parts.length < 3) return;

            String telephoneDest = parts[1];
            String contenu = parts[2];

            try {
                messageRouter.envoyerMessage(telephoneConnecte, telephoneDest, contenu);
            } catch (SQLException e) {
                e.printStackTrace();
                pw.println("MSG_FAIL|Erreur_Envoi");
            }
        }
    // ── Broadcast liste connectés ─────────────────────────────────────────────
    private void broadcastUsersList() {
        userManager.broadcast(Protocol.USERS_LIST + "|" + userManager.getOnlineUsersList());
    }

    // ── Envoyer un message à CE client ───────────────────────────────────────
    public void sendMessage(String message) {
        if (pw != null) pw.println(message);
    }
    private void handleCallRequest(String[] parts) {
        if (parts.length < 3) return;
        String telephoneDest = parts[1];
        String typeAppel     = parts[2]; // "audio" ou "video"
        try {
            String ipAppelant = socket != null && socket.getInetAddress() != null
                    ? socket.getInetAddress().getHostAddress()
                    : "";
            callManager.demanderAppel(telephoneConnecte, telephoneDest, typeAppel, ipAppelant);
        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.CALL_END.name() + "|ERREUR");
        }
    }

    // ── CALL_ACCEPT|telephoneAppelant ────────────────────────────────────────
    private void handleCallAccept(String[] parts) {
        if (parts.length < 2) return;
        String telephoneAppelant = parts[1];
        try {
            String ipAccepteur = socket != null && socket.getInetAddress() != null
                    ? socket.getInetAddress().getHostAddress()
                    : "";
            callManager.accepterAppel(telephoneConnecte, telephoneAppelant, ipAccepteur);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── CALL_REFUSE|telephoneAppelant ────────────────────────────────────────
    private void handleCallRefuse(String[] parts) {
        if (parts.length < 2) return;
        String telephoneAppelant = parts[1];
        try {
            callManager.refuserAppel(telephoneConnecte, telephoneAppelant);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── CALL_END|telephoneDest ────────────────────────────────────────────────
    private void handleCallEnd(String[] parts) {
        if (parts.length < 2) return;
        String telephoneDest = parts[1];
        try {
            callManager.terminerAppel(telephoneConnecte, telephoneDest);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── CALL_CANCEL|telephoneDest ─────────────────────────────────────────────
    private void handleCallCancel(String[] parts) {
        if (parts.length < 2) return;
        String telephoneDest = parts[1];
        try {
            callManager.annulerAppel(telephoneConnecte, telephoneDest);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // ── GET_CONVERSATIONS ─────────────────────────────────────────────────────
// Client envoie : GET_CONVERSATIONS
// Serveur répond : CONVERSATIONS_LIST|id;type;nom;dateDernierMsg|id;type;...
    private void handleGetConversations() {
        try {
            Utilisateur u = userDAO.findByTelephone(telephoneConnecte);
            if (u == null) return;

            List<Conversation> convs = convDAO.getByUtilisateur(u.getIdUtilisateur());

            // Pré-charger les données
            Map<Integer, Integer> nonLusMap = fileDAO.compterNonLusParConversation(u.getIdUtilisateur());
            Map<Integer, Message> dernierMsgMap = messageDAO.getDernierMessageParConversation();

            if (convs.isEmpty()) {
                pw.println(Protocol.CONVERSATIONS_LIST.name() + "|");
                return;
            }

            StringBuilder sb = new StringBuilder(Protocol.CONVERSATIONS_LIST.name() + "|");

            for (Conversation c : convs) {
                String nomAffichage;

                // Pour conversation individuelle → nom de l'autre participant
                if ("individuelle".equals(c.getTypeConversation())) {
                    Utilisateur autre = convDAO.getAutreParticipant(
                            c.getIdConversation(), u.getIdUtilisateur());
                    nomAffichage = (autre != null) ? autre.getNomComplet() : "Inconnu";
                } else {
                    nomAffichage = c.getNomGroupe() != null ? c.getNomGroupe() : "Groupe";
                }

                int nbNonLus = nonLusMap.getOrDefault(c.getIdConversation(), 0);

                String dernierMsgContenu = "";
                Message dernierMsg = dernierMsgMap.get(c.getIdConversation());
                if (dernierMsg != null) {
                    dernierMsgContenu = dernierMsg.getContenuTexte() != null
                            ? dernierMsg.getContenuTexte() : "";
                }

                sb.append(c.getIdConversation()).append(";")
                        .append(c.getTypeConversation()).append(";")
                        .append(nomAffichage).append(";")
                        .append((c.getDateDernierMessage() != null)
                                ? c.getDateDernierMessage().toString() : "").append(";")
                        .append(nbNonLus).append(";")
                        .append(dernierMsgContenu).append("|");
            }

            pw.println(sb.toString());
            System.out.println("[CONV] Envoyé " + convs.size()
                    + " conversations enrichies à " + telephoneConnecte);

        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.CONVERSATIONS_LIST.name() + "|ERROR");
        }
    }

    // ── GET_MESSAGES|idConversation ───────────────────────────────────────────
    // Après envoi des messages, marquer la conversation comme lue
    private void handleGetMessages(String[] parts) {
        if (parts.length < 2) return;

        try {
            int idConversation = Integer.parseInt(parts[1].trim());
            Utilisateur u = userDAO.findByTelephone(telephoneConnecte);
            if (u == null) return;

            List<Message> messages = messageDAO.getByConversation(idConversation);

            if (messages.isEmpty()) {
                pw.println(Protocol.MESSAGES_LIST.name() + "|");
                return;
            }

            StringBuilder sb = new StringBuilder(Protocol.MESSAGES_LIST.name() + "|");
            for (Message m : messages) {
                Utilisateur exp = userDAO.getByID(m.getIdExpediteur());
                String telExp = exp != null ? exp.getNumeroTelephone() : "?";
                String nomExp = exp != null ? exp.getNomComplet() : "?";

                sb.append(m.getIdMessage()).append(";")
                        .append(telExp).append(";")
                        .append(nomExp).append(";")
                        .append(m.getContenuTexte() != null ? m.getContenuTexte() : "").append(";")
                        .append(m.getDateEnvoi() != null ? m.getDateEnvoi().toString() : "").append("|");
            }

            pw.println(sb.toString());

            // ← MARQUER COMME LUS : mettre est_delivre=1 dans messages_file_attente
            fileDAO.marquerConversationCommeLue(idConversation, u.getIdUtilisateur());

            System.out.println("[MSG] Envoyé " + messages.size()
                    + " messages de conv " + idConversation
                    + " à " + telephoneConnecte);

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }
    //fichier

    private void handleFileSend(String[] parts) {
        if (parts.length < 4) return;

        String telDest = parts[1];
        String fileName = parts[2];
        String base64 = parts[3];

        ClientHandler destHandler = userManager.getHandler(telDest);

        if (destHandler == null) {
            pw.println(Protocol.FILE_FAIL.name() + "|DEST_OFFLINE");
            return;
        }

        destHandler.sendMessage(
                Protocol.FILE_RECEIVE.name() + "|" +
                        telephoneConnecte + "|" +
                        fileName + "|" +
                        base64
        );

        System.out.println("[FILE] fichier transféré de " + telephoneConnecte + " vers " + telDest);
    }
    // ── ADD_CONTACT|telephoneContact ─────────────────────────────────────────
// Flux :
// 1. Alice ajoute Bob → serveur crée le contact côté Alice
// 2. Si Bob est en ligne → notifie Bob avec CONTACT_REQUEST
// 3. Bob répond CONTACT_ACCEPTED ou BLOCK_CONTACT
    private void handleAddContact(String[] parts) {
        if (parts.length < 2) {
            pw.println(Protocol.ADD_CONTACT_FAIL.name());
            return;
        }

        String telephoneContact = parts[1].trim();
        String nomAffiche = parts.length >= 3 ? parts[2].trim() : "";

        try {
            Utilisateur moi = userDAO.findByTelephone(telephoneConnecte);
            if (moi == null) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name());
                return;
            }

            Utilisateur contact = userDAO.findByTelephone(telephoneContact);
            if (contact == null) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|NUMERO_INTROUVABLE");
                return;
            }

            if (moi.getIdUtilisateur() == contact.getIdUtilisateur()) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|AJOUT_SOI_MEME");
                return;
            }

            if (contactDAO.estBloque(contact.getIdUtilisateur(), moi.getIdUtilisateur())) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|BLOQUE");
                return;
            }

            // Créer le contact côté Alice (moi)
            if (!contactDAO.contactExiste(moi.getIdUtilisateur(), contact.getIdUtilisateur())) {
                Contact c = new Contact();
                c.setIdUtilisateur(moi.getIdUtilisateur());
                c.setIdContactUtilisateur(contact.getIdUtilisateur());
                c.setNomAffiche(nomAffiche.isEmpty() ? contact.getNomComplet() : nomAffiche);
                c.setEstBloque(false);
                contactDAO.Add(c);
            }

            pw.println(Protocol.ADD_CONTACT_OK.name()
                    + "|" + contact.getNumeroTelephone()
                    + "|" + contact.getNomComplet());

            // Ligne PENDING côté destinataire (Bob) : nécessaire pour CONTACT_ACCEPTED, en ligne comme hors ligne
            if (!contactDAO.contactExiste(contact.getIdUtilisateur(), moi.getIdUtilisateur())) {
                contactDAO.ajouterDemandeEnAttente(
                        contact.getIdUtilisateur(),
                        moi.getIdUtilisateur());
            }

            // Notifier Bob s'il est connecté
            ClientHandler contactHandler = userManager.getHandler(telephoneContact);
            if (contactHandler != null) {
                contactHandler.sendMessage(
                        Protocol.CONTACT_REQUEST.name()
                                + "|" + moi.getNumeroTelephone()
                                + "|" + moi.getNomComplet()
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|ERREUR_SERVEUR");
        }
    }

    // ── CONTACT_ACCEPTED|telephoneAlice ──────────────────────────────────────
// Bob accepte → on crée le contact côté Bob aussi
    private void handleContactAccepted(String[] parts) {
        if (parts.length < 2) return;
        String telephoneAlice = parts[1].trim();

        try {
            Utilisateur moi   = userDAO.findByTelephone(telephoneConnecte); // Bob
            Utilisateur alice = userDAO.findByTelephone(telephoneAlice);
            if (moi == null || alice == null) return;

            // 1. Mettre à jour PENDING → nom réel d'Alice côté Bob
            String sql = "UPDATE contacts SET nom_affiche=? "
                    + "WHERE id_utilisateur=? AND id_contact_utilisateur=? "
                    + "AND nom_affiche='PENDING'";
            try (Connection con = DataBase.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, alice.getNomComplet());
                ps.setInt(2, moi.getIdUtilisateur());
                ps.setInt(3, alice.getIdUtilisateur());
                ps.executeUpdate();
            }

            // 2. Confirmer à Bob
            pw.println(Protocol.CONTACT_ACCEPTED.name() + "|OK");

            // 3. ← NOUVEAU : notifier Alice que Bob a accepté
            ClientHandler aliceHandler = userManager.getHandler(telephoneAlice);
            if (aliceHandler != null) {
                aliceHandler.sendMessage(
                        Protocol.CONTACT_ACCEPTED.name()
                                + "|" + moi.getNumeroTelephone()
                                + "|" + moi.getNomComplet()
                );
            }

            System.out.println("[CONTACT] " + telephoneConnecte
                    + " a accepté " + telephoneAlice);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── BLOCK_CONTACT|telephoneContact ───────────────────────────────────────
// Bob bloque Alice → plus aucun message ne passera
    private void handleBlockContact(String[] parts) {
        if (parts.length < 2) return;
        String telephoneABloquer = parts[1].trim();

        try {
            Utilisateur moi      = userDAO.findByTelephone(telephoneConnecte);
            Utilisateur aBlocker = userDAO.findByTelephone(telephoneABloquer);
            if (moi == null || aBlocker == null) return;

            // Bloquer
            contactDAO.bloquerContact(moi.getIdUtilisateur(),
                    aBlocker.getIdUtilisateur());

            pw.println(Protocol.BLOCK_OK.name() + "|" + telephoneABloquer);

            System.out.println("[CONTACT] " + telephoneConnecte
                    + " a bloqué " + telephoneABloquer);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── GROUPES V2 ───────────────────────────────────────────────────────────
    private void handleCreateGroup(String[] parts) {
        try {
            if (parts.length < 3) {
                pw.println(Protocol.CREATE_GROUP_FAIL.name() + "|Données invalides");
                return;
            }
            List<String> membres = new ArrayList<>();
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].isBlank()) membres.add(parts[i].trim());
            }
            Groupe groupe = groupeDAO.creerGroupe(parts[1].trim(), parts[2].trim(), membres);
            pw.println(Protocol.CREATE_GROUP_OK.name() + "|"
                    + groupe.getIdGroupe() + "|"
                    + groupe.getNomGroupe() + "|"
                    + groupe.getNumeroCreateur() + "|"
                    + groupe.getDateCreation() + "|"
                    + String.join(";", groupe.getNumerosMembres()));
        } catch (Exception e) {
            e.printStackTrace();
            pw.println(Protocol.CREATE_GROUP_FAIL.name() + "|Erreur création groupe");
        }
    }

    private void handleGetGroups(String[] parts) {
        try {
            String numero = parts.length >= 2 ? parts[1] : telephoneConnecte;
            List<Groupe> groupes = groupeDAO.getGroupesPourMembre(numero);
            StringBuilder sb = new StringBuilder();
            for (Groupe g : groupes) {
                if (sb.length() > 0) sb.append("|");
                sb.append(g.getIdGroupe()).append(";")
                        .append(g.getNomGroupe()).append(";")
                        .append(g.getNumeroCreateur()).append(";")
                        .append(g.getNumerosMembres() != null ? g.getNumerosMembres().size() : 0);
                if (g.getNumerosMembres() != null) {
                    for (String m : g.getNumerosMembres()) {
                        if (m != null && !m.isBlank()) sb.append(";").append(m.trim());
                    }
                }
            }
            pw.println(Protocol.GROUPS_LIST.name() + "|" + sb);
        } catch (Exception e) {
            e.printStackTrace();
            pw.println(Protocol.GROUPS_LIST.name() + "|");
        }
    }

    private void handleSendGroupMessage(String[] parts) {
        try {
            if (parts.length < 4) return;
            int idGroupe = Integer.parseInt(parts[1]);
            Groupe g = groupeDAO.getById(idGroupe);
            if (g == null) return;

            Utilisateur exp = userDAO.findByTelephone(parts[2]);
            String nomExp = exp != null ? exp.getNomComplet() : parts[2];

            MessageGroupe msg = new MessageGroupe();
            msg.setIdGroupe(idGroupe);
            msg.setTelephoneExpediteur(parts[2]);
            msg.setNomExpediteur(nomExp);
            msg.setContenu(parts[3]);
            msg.setDateEnvoi(LocalDateTime.now());
            int idMsg = messageGroupeDAO.ajouter(msg);
            msg.setIdMessage(idMsg);

            String payload = Protocol.GROUP_MESSAGE_RECEIVE.name() + "|"
                    + idGroupe + "|"
                    + msg.getTelephoneExpediteur() + "|"
                    + msg.getNomExpediteur() + "|"
                    + msg.getContenu() + "|"
                    + msg.getDateEnvoi();
            for (String membre : g.getNumerosMembres()) {
                ClientHandler h = userManager.getHandler(membre);
                if (h != null) h.sendMessage(payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleGetGroupMessages(String[] parts) {
        try {
            if (parts.length < 2) return;
            int idGroupe = Integer.parseInt(parts[1]);
            List<MessageGroupe> liste = messageGroupeDAO.getByGroupe(idGroupe);
            StringBuilder sb = new StringBuilder();
            for (MessageGroupe msg : liste) {
                if (sb.length() > 0) sb.append("|");
                sb.append(msg.getIdMessage()).append(";")
                        .append(msg.getTelephoneExpediteur()).append(";")
                        .append(msg.getNomExpediteur()).append(";")
                        .append(msg.getContenu()).append(";")
                        .append(msg.getDateEnvoi() != null ? msg.getDateEnvoi().toString() : "");
            }
            pw.println(Protocol.GROUP_MESSAGES_LIST.name() + "|" + sb);
        } catch (Exception e) {
            e.printStackTrace();
            pw.println(Protocol.GROUP_MESSAGES_LIST.name() + "|");
        }
    }

    private void handleAddGroupMember(String[] parts) {
        try {
            if (parts.length < 4) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String numAdmin = parts[2];
            String numNew = parts[3];
            if (!groupeDAO.estAdmin(idGroupe, numAdmin)) {
                pw.println(Protocol.ADD_GROUP_MEMBER_FAIL.name() + "|Permission refusée");
                return;
            }
            groupeDAO.ajouterMembre(idGroupe, numNew);
            Groupe g = groupeDAO.getById(idGroupe);
            String payload = Protocol.ADD_GROUP_MEMBER_OK.name() + "|" + idGroupe + "|" + numNew;
            if (g != null && g.getNumerosMembres() != null) {
                for (String membre : g.getNumerosMembres()) {
                    ClientHandler h = userManager.getHandler(membre);
                    if (h != null) h.sendMessage(payload);
                }
            } else {
                pw.println(payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pw.println(Protocol.ADD_GROUP_MEMBER_FAIL.name() + "|Erreur serveur");
        }
    }

    private void handleRemoveGroupMember(String[] parts) {
        try {
            if (parts.length < 4) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String numAdmin = parts[2];
            String numMembre = parts[3];
            if (!groupeDAO.estAdmin(idGroupe, numAdmin)) {
                pw.println(Protocol.REMOVE_GROUP_MEMBER_FAIL.name() + "|Permission refusée");
                return;
            }
            Groupe avant = groupeDAO.getById(idGroupe);
            List<String> notif = avant != null && avant.getNumerosMembres() != null
                    ? new ArrayList<>(avant.getNumerosMembres()) : new ArrayList<>();
            groupeDAO.retirerMembre(idGroupe, numMembre);
            if (!notif.contains(numMembre)) notif.add(numMembre);
            String payload = Protocol.REMOVE_GROUP_MEMBER_OK.name() + "|" + idGroupe + "|" + numMembre;
            for (String tel : notif) {
                ClientHandler h = userManager.getHandler(tel);
                if (h != null) h.sendMessage(payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pw.println(Protocol.REMOVE_GROUP_MEMBER_FAIL.name() + "|Erreur serveur");
        }
    }

    private void handleQuitGroup(String[] parts) {
        try {
            if (parts.length < 3) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String numero = parts[2];
            groupeDAO.retirerMembre(idGroupe, numero);
            pw.println(Protocol.QUIT_GROUP_OK.name() + "|" + idGroupe);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteGroup(String[] parts) {
        try {
            if (parts.length < 3) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String admin = parts[2];
            if (!groupeDAO.estAdmin(idGroupe, admin)) return;
            groupeDAO.supprimerGroupe(idGroupe);
            pw.println(Protocol.DELETE_GROUP_OK.name() + "|" + idGroupe);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRenameGroup(String[] parts) {
        try {
            if (parts.length < 4) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String admin = parts[2];
            String nouveauNom = parts[3];
            if (!groupeDAO.estAdmin(idGroupe, admin)) return;
            groupeDAO.renommerGroupe(idGroupe, nouveauNom);
            pw.println(Protocol.RENAME_GROUP_OK.name() + "|" + idGroupe + "|" + nouveauNom);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
