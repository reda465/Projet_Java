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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import Dao.*;
import util.GroupeFichierPayload;
import util.SqlMessageTypeUtil;
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
                    case Protocol.GET_CONTACTS -> handleGetContacts();
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
                    case FILE_UPLOAD_BEGIN -> handleFileUploadBegin(parts);
                    case FILE_UPLOAD_CHUNK -> handleFileUploadChunk(parts);
                    case FILE_UPLOAD_END -> handleFileUploadEnd(parts);
                    case GET_MESSAGE_FILE -> handleGetMessageFile(parts);
                    case GET_GROUP_FILE -> handleGetGroupFile(parts);
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
        if (parts.length < 3) { pw.println(Protocol.LOGIN_FAIL); return; }

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
            pw.println(Protocol.LOGIN_FAIL);
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
            pw.println("REGISTER_OK|Inscription_Avec_Succes");
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

            String telephoneDest = normaliserNumeroPourRecherche(parts[1]);
            StringBuilder contenu = new StringBuilder(parts[2] != null ? parts[2] : "");
            for (int i = 3; i < parts.length; i++) {
                contenu.append('|').append(parts[i] != null ? parts[i] : "");
            }

            try {
                messageRouter.envoyerMessage(telephoneConnecte, telephoneDest, contenu.toString());
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
                String numeroAutre = "";

                if ("individuelle".equals(c.getTypeConversation())) {
                    Utilisateur autre = convDAO.getAutreParticipant(
                            c.getIdConversation(), u.getIdUtilisateur());
                    nomAffichage = (autre != null) ? autre.getNomComplet() : "Inconnu";
                    if (autre != null && autre.getNumeroTelephone() != null) {
                        numeroAutre = autre.getNumeroTelephone().trim();
                    }
                } else {
                    nomAffichage = c.getNomGroupe() != null ? c.getNomGroupe() : "Groupe";
                }

                int nbNonLus = nonLusMap.getOrDefault(c.getIdConversation(), 0);

                String dernierMsgContenu = "";
                Message dernierMsg = dernierMsgMap.get(c.getIdConversation());
                if (dernierMsg != null) {
                    if ("texte".equals(dernierMsg.getTypeMessage())) {
                        dernierMsgContenu = dernierMsg.getContenuTexte() != null
                                ? dernierMsg.getContenuTexte() : "";
                    } else {
                        String nomF = dernierMsg.getNomFichier() != null ? dernierMsg.getNomFichier() : "fichier";
                        dernierMsgContenu = "\ud83d\udcce " + nomF;
                    }
                }

                // id;type;nom;numeroTel;date;nonLus;dernierMsg
                sb.append(c.getIdConversation()).append(";")
                        .append(c.getTypeConversation()).append(";")
                        .append(nomAffichage).append(";")
                        .append(numeroAutre).append(";")
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

    // ── GET_CONTACTS ─────────────────────────────────────────────────────────
    private void handleGetContacts() {
        try {
            if (telephoneConnecte == null || telephoneConnecte.isBlank()) {
                pw.println(Protocol.CONTACTS_LIST.name() + "|ERROR");
                return;
            }
            Utilisateur u = userDAO.findByTelephone(telephoneConnecte);
            if (u == null) {
                pw.println(Protocol.CONTACTS_LIST.name() + "|");
                return;
            }

            List<Contact> contacts = contactDAO. getContactsByUtilisateur(u.getIdUtilisateur());

            if (contacts.isEmpty()) {
                pw.println(Protocol.CONTACTS_LIST.name() + "|");
                return;
            }

            StringBuilder sb = new StringBuilder(Protocol.CONTACTS_LIST.name() + "|");
            for (Contact c : contacts) {
                sb.append(c.getIdContact()).append(";")
                        .append(c.getNumeroTelephone() != null ? c.getNumeroTelephone().trim() : "")
                        .append(";")
                        .append(c.getNomComplet() != null ? c.getNomComplet().trim() : "")
                        .append("|");
            }

            pw.println(sb.toString());
            System.out.println("[CONTACTS] Envoyé " + contacts.size()
                    + " contacts à " + telephoneConnecte);

        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.CONTACTS_LIST.name() + "|ERROR");
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

                String type = m.getTypeMessage() != null ? m.getTypeMessage() : "texte";
                String nomFich = m.getNomFichier() != null ? m.getNomFichier() : "";
                String tailleStr = m.getTailleFichier() != null ? String.valueOf(m.getTailleFichier()) : "";

                sb.append(m.getIdMessage()).append(";")
                        .append(telExp).append(";")
                        .append(nomExp).append(";")
                        .append(m.getContenuTexte() != null ? m.getContenuTexte() : "").append(";")
                        .append(m.getDateEnvoi() != null ? m.getDateEnvoi().toString() : "").append(";")
                        .append(type).append(";")
                        .append(nomFich).append(";")
                        .append(tailleStr).append("|");
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

    private static final int FILE_TRANSFER_CHUNK = 48 * 1024;

    private void handleFileUploadBegin(String[] parts) {
        if (telephoneConnecte == null || parts.length < 8) {
            if (pw != null && parts.length > 1)
                pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + parts[1] + "|PARAM");
            return;
        }
        String sessionId = parts[1];
        String modeS = parts[2];
        String captionPlain = "";
        try {
            captionPlain = new String(Base64.getDecoder().decode(parts[7]), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        try {
            if ("IND".equalsIgnoreCase(modeS)) {
                String dest = normaliserNumeroPourRecherche(parts[3]);
                String name = parts[4] != null ? parts[4] : "fichier";
                long size = Long.parseLong(parts[5].trim());
                String type = parts[6] != null && !parts[6].isBlank() ? parts[6] : "document";
                ChunkedUploadBuffer.begin(sessionId, ChunkedUploadBuffer.Mode.IND, dest, 0, size, name, type, captionPlain);
            } else if ("GRP".equalsIgnoreCase(modeS)) {
                int idGroupe = Integer.parseInt(parts[3].trim());
                if (!groupeDAO.estMembre(idGroupe, telephoneConnecte)) {
                    pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|NOT_MEMBER");
                    return;
                }
                String name = parts[4] != null ? parts[4] : "fichier";
                long size = Long.parseLong(parts[5].trim());
                String type = parts[6] != null && !parts[6].isBlank() ? parts[6] : "document";
                ChunkedUploadBuffer.begin(sessionId, ChunkedUploadBuffer.Mode.GRP, "", idGroupe, size, name, type, captionPlain);
            } else {
                pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|MODE");
            }
        } catch (Exception e) {
            ChunkedUploadBuffer.cancel(sessionId);
            if (pw != null)
                pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|" + e.getMessage());
        }
    }

    private void handleFileUploadChunk(String[] parts) {
        if (parts.length < 3) return;
        String sessionId = parts[1];
        StringBuilder b64 = new StringBuilder(parts[2] != null ? parts[2] : "");
        for (int i = 3; i < parts.length; i++) {
            b64.append('|').append(parts[i] != null ? parts[i] : "");
        }
        try {
            ChunkedUploadBuffer.appendChunk(sessionId, b64.toString());
        } catch (Exception e) {
            ChunkedUploadBuffer.cancel(sessionId);
            if (pw != null)
                pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|CHUNK");
        }
    }

    private void handleFileUploadEnd(String[] parts) {
        if (telephoneConnecte == null || parts.length < 2) return;
        String sessionId = parts[1];
        try {
            ChunkedUploadBuffer.ResolvedUpload r = ChunkedUploadBuffer.endSession(sessionId);
            Path mediaPath = MediaPaths.mediaRoot().resolve(r.storedFilename());
            long sz = Files.size(mediaPath);

            if (r.mode() == ChunkedUploadBuffer.Mode.IND) {
                int id = messageRouter.enregistrerMessageFichier(telephoneConnecte, r.destTel(),
                        r.storedFilename(), r.originalName(), sz, r.typeMessage(), r.captionPlain());
                if (id < 0) {
                    Files.deleteIfExists(mediaPath);
                    pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|SEND");
                    return;
                }
                pw.println(Protocol.FILE_UPLOAD_OK.name() + "|" + sessionId + "|IND|" + id);
            } else {
                Groupe g = groupeDAO.getById(r.idGroupe());
                if (g == null) {
                    Files.deleteIfExists(mediaPath);
                    pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|GROUP");
                    return;
                }
                Utilisateur exp = userDAO.findByTelephone(telephoneConnecte);
                MessageGroupe msg = new MessageGroupe();
                msg.setIdGroupe(r.idGroupe());
                msg.setTelephoneExpediteur(normaliserNumeroPourRecherche(telephoneConnecte));
                msg.setNomExpediteur(exp != null ? exp.getNomComplet() : telephoneConnecte);
                msg.setContenu(GroupeFichierPayload.creer(r.typeMessage(), r.originalName(), r.storedFilename(), sz, r.captionPlain()));
                msg.setDateEnvoi(LocalDateTime.now());
                int idMsg = messageGroupeDAO.ajouter(msg);

                String payload = Protocol.GROUP_MESSAGE_RECEIVE.name() + "|"
                        + r.idGroupe() + "|"
                        + msg.getTelephoneExpediteur() + "|"
                        + msg.getNomExpediteur() + "|"
                        + msg.getContenu() + "|"
                        + msg.getDateEnvoi();
                if (g.getNumerosMembres() != null) {
                    for (String membre : g.getNumerosMembres()) {
                        ClientHandler h = userManager.getHandler(membre);
                        if (h != null) h.sendMessage(payload);
                    }
                }
                pw.println(Protocol.FILE_UPLOAD_OK.name() + "|" + sessionId + "|GRP|" + idMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ChunkedUploadBuffer.cancel(sessionId);
            if (pw != null)
                pw.println(Protocol.FILE_UPLOAD_FAIL.name() + "|" + sessionId + "|" + e.getMessage());
        }
    }

    private void handleGetMessageFile(String[] parts) {
        if (telephoneConnecte == null || parts.length < 2) return;
        try {
            int idMessage = Integer.parseInt(parts[1].trim());
            Utilisateur u = userDAO.findByTelephone(telephoneConnecte);
            if (u == null) return;
            Message m = messageDAO.getByID(idMessage);
            if (m == null || m.getUrlFichier() == null || m.getUrlFichier().isBlank()) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|NO_FILE");
                return;
            }
            if (!convDAO.estParticipant(m.getIdConversation(), u.getIdUtilisateur())) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|FORBIDDEN");
                return;
            }
            Path f = MediaPaths.mediaRoot().resolve(m.getUrlFichier());
            if (!Files.isRegularFile(f)) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|MISSING");
                return;
            }
            byte[] data = Files.readAllBytes(f);
            String nom = m.getNomFichier() != null ? m.getNomFichier() : "fichier";
            envoyerTransfertFichier(idMessage, nom, m.getTypeMessage(), data);
        } catch (Exception e) {
            e.printStackTrace();
            if (pw != null) pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|0|ERROR");
        }
    }

    private void handleGetGroupFile(String[] parts) {
        if (telephoneConnecte == null || parts.length < 2) return;
        try {
            int idMessage = Integer.parseInt(parts[1].trim());
            MessageGroupe mg = messageGroupeDAO.getById(idMessage);
            if (mg == null || !GroupeFichierPayload.estFichier(mg.getContenu())) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|NO_FILE");
                return;
            }
            if (!groupeDAO.estMembre(mg.getIdGroupe(), telephoneConnecte)) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|FORBIDDEN");
                return;
            }
            GroupeFichierPayload.Meta meta = GroupeFichierPayload.parser(mg.getContenu());
            if (meta == null || meta.storageKey() == null || meta.storageKey().isBlank()) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|META");
                return;
            }
            Path f = MediaPaths.mediaRoot().resolve(meta.storageKey());
            if (!Files.isRegularFile(f)) {
                pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|" + idMessage + "|MISSING");
                return;
            }
            byte[] data = Files.readAllBytes(f);
            envoyerTransfertFichier(idMessage, meta.nomFichier(), meta.typeMessage(), data);
        } catch (Exception e) {
            e.printStackTrace();
            if (pw != null) pw.println(Protocol.FILE_TRANSFER_FAIL.name() + "|0|ERROR");
        }
    }

    private void envoyerTransfertFichier(int messageId, String nomAffiche, String typeMessage, byte[] data) {
        String nomB64 = Base64.getEncoder().encodeToString(nomAffiche.getBytes(StandardCharsets.UTF_8));
        int total = Math.max(1, (data.length + FILE_TRANSFER_CHUNK - 1) / FILE_TRANSFER_CHUNK);
        String affType = SqlMessageTypeUtil.pourAffichage(typeMessage, nomAffiche);
        pw.println(Protocol.FILE_TRANSFER_BEGIN.name() + "|" + messageId + "|" + nomB64 + "|"
                + data.length + "|" + total + "|" + affType);
        for (int i = 0; i < total; i++) {
            int from = i * FILE_TRANSFER_CHUNK;
            int to = Math.min(from + FILE_TRANSFER_CHUNK, data.length);
            String b64 = Base64.getEncoder().encodeToString(Arrays.copyOfRange(data, from, to));
            pw.println(Protocol.FILE_TRANSFER_PART.name() + "|" + messageId + "|" + i + "|" + b64);
        }
        pw.println(Protocol.FILE_TRANSFER_END.name() + "|" + messageId);
    }

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
    /** Même logique que le client : aligner saisie et clés UserManager. */
    private static String normaliserNumeroPourRecherche(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", "").replace("-", "");
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

        if (telephoneConnecte == null || telephoneConnecte.isBlank()) {
            pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|NON_AUTH");
            return;
        }

        String telephoneContact = normaliserNumeroPourRecherche(parts[1]);
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

            // Ligne PENDING côté Bob : id_utilisateur = Bob, id_contact_utilisateur = Alice
            try {
                if (!contactDAO.contactExiste(contact.getIdUtilisateur(), moi.getIdUtilisateur())) {
                    contactDAO.ajouterDemandeEnAttente(
                            moi.getIdUtilisateur(),
                            contact.getIdUtilisateur());
                }
            } catch (SQLException pendingEx) {
                pendingEx.printStackTrace();
                System.err.println("[CONTACT] PENDING SQL (non bloquant) : " + pendingEx.getMessage());
            }

            String telDestEnBase = contact.getNumeroTelephone();
            ClientHandler contactHandler = telDestEnBase != null
                    ? userManager.getHandler(telDestEnBase.trim())
                    : null;
            if (contactHandler != null) {
                String nomMoi = moi.getNomComplet() != null ? moi.getNomComplet() : "";
                contactHandler.sendMessage(
                        Protocol.CONTACT_REQUEST.name()
                                + "|" + moi.getNumeroTelephone()
                                + "|" + nomMoi
                );
                System.out.println("[CONTACT] CONTACT_REQUEST push → " + telDestEnBase
                        + " (demandeur " + moi.getNumeroTelephone() + ")");
            } else {
                System.out.println("[CONTACT] Destinataire hors ligne ou clé inconnue : " + telDestEnBase);
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
        String telephoneAlice = normaliserNumeroPourRecherche(parts[1].trim());

        try {
            Utilisateur moi   = userDAO.findByTelephone(telephoneConnecte); // Bob
            Utilisateur alice = userDAO.findByTelephone(telephoneAlice);
            if (moi == null || alice == null) return;

            if (!contactDAO.contactExiste(moi.getIdUtilisateur(), alice.getIdUtilisateur())) {
                Contact c = new Contact();
                c.setIdUtilisateur(moi.getIdUtilisateur());
                c.setIdContactUtilisateur(alice.getIdUtilisateur());
                c.setNomAffiche(alice.getNomComplet());
                c.setEstBloque(false);
                contactDAO.Add(c);
            } else {
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
            }

            // 2. Confirmer à Bob
            pw.println(Protocol.CONTACT_ACCEPTED.name() + "|OK");

            // 3. Notifier Alice que Bob a accepté
            String telAlice = alice.getNumeroTelephone();
            ClientHandler aliceHandler = telAlice != null ? userManager.getHandler(telAlice.trim()) : null;
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
        String telephoneABloquer = normaliserNumeroPourRecherche(parts[1]);

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
                if (!parts[i].isBlank()) membres.add(normaliserNumeroPourRecherche(parts[i]));
            }
            Groupe groupe = groupeDAO.creerGroupe(parts[1].trim(),
                    normaliserNumeroPourRecherche(parts[2]), membres);
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
            String numero = parts.length >= 2
                    ? normaliserNumeroPourRecherche(parts[1])
                    : normaliserNumeroPourRecherche(telephoneConnecte != null ? telephoneConnecte : "");
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

            String telExp = normaliserNumeroPourRecherche(parts[2]);
            Utilisateur exp = userDAO.findByTelephone(telExp);
            String nomExp = exp != null ? exp.getNomComplet() : telExp;

            StringBuilder contenu = new StringBuilder(parts[3] != null ? parts[3] : "");
            for (int i = 4; i < parts.length; i++) {
                contenu.append('|').append(parts[i] != null ? parts[i] : "");
            }

            MessageGroupe msg = new MessageGroupe();
            msg.setIdGroupe(idGroupe);
            msg.setTelephoneExpediteur(telExp);
            msg.setNomExpediteur(nomExp);
            msg.setContenu(contenu.toString());
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
            String numAdmin = normaliserNumeroPourRecherche(parts[2]);
            String numNew = normaliserNumeroPourRecherche(parts[3]);
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
            String numAdmin = normaliserNumeroPourRecherche(parts[2]);
            String numMembre = normaliserNumeroPourRecherche(parts[3]);
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
            String numero = normaliserNumeroPourRecherche(parts[2]);
            Groupe avant = groupeDAO.getById(idGroupe);
            List<String> notif = avant != null && avant.getNumerosMembres() != null
                    ? new ArrayList<>(avant.getNumerosMembres()) : new ArrayList<>();
            groupeDAO.retirerMembre(idGroupe, numero);
            String payload = Protocol.QUIT_GROUP_OK.name() + "|" + idGroupe;
            for (String tel : notif) {
                ClientHandler h = userManager.getHandler(tel);
                if (h != null) h.sendMessage(payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteGroup(String[] parts) {
        try {
            if (parts.length < 3) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String admin = normaliserNumeroPourRecherche(parts[2]);
            if (!groupeDAO.estAdmin(idGroupe, admin)) return;
            Groupe g = groupeDAO.getById(idGroupe);
            List<String> membres = g != null && g.getNumerosMembres() != null
                    ? new ArrayList<>(g.getNumerosMembres()) : new ArrayList<>();
            groupeDAO.supprimerGroupe(idGroupe);
            String payload = Protocol.DELETE_GROUP_OK.name() + "|" + idGroupe;
            for (String tel : membres) {
                ClientHandler h = userManager.getHandler(tel);
                if (h != null) h.sendMessage(payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRenameGroup(String[] parts) {
        try {
            if (parts.length < 4) return;
            int idGroupe = Integer.parseInt(parts[1]);
            String admin = normaliserNumeroPourRecherche(parts[2]);
            StringBuilder nouveauNom = new StringBuilder(parts[3] != null ? parts[3] : "");
            for (int i = 4; i < parts.length; i++) {
                nouveauNom.append('|').append(parts[i] != null ? parts[i] : "");
            }
            if (!groupeDAO.estAdmin(idGroupe, admin)) return;
            groupeDAO.renommerGroupe(idGroupe, nouveauNom.toString());
            Groupe g = groupeDAO.getById(idGroupe);
            String payload = Protocol.RENAME_GROUP_OK.name() + "|" + idGroupe + "|" + nouveauNom;
            if (g != null && g.getNumerosMembres() != null) {
                for (String tel : g.getNumerosMembres()) {
                    ClientHandler h = userManager.getHandler(tel);
                    if (h != null) h.sendMessage(payload);
                }
            } else {
                pw.println(payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
