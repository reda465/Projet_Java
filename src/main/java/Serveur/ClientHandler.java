package Serveur;

import lombok.Getter;
import lombok.Setter;
import model.Conversation;
import model.Groupe;
import model.Message;
import model.MessageGroupe;
import model.Utilisateur;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import Dao.*;
@Getter
@Setter

public class ClientHandler extends Thread {
    private static final Map<Integer, Groupe> groupes = new ConcurrentHashMap<>();
    private static final Map<Integer, List<MessageGroupe>> messagesGroupes = new ConcurrentHashMap<>();
    private static int sequenceGroupe = 1;
    private static int sequenceMessageGroupe = 1;

    private final Socket socket;
    private PrintWriter pw;
    private String telephoneConnecte; // null = pas encore authentifié
    private final MessageRouter messageRouter = MessageRouter.getInstance();
    private final CallManager callManager = CallManager.getInstance();
    private final UserManager      userManager = UserManager.getInstance();
    private final  Dao_UtilisateurImp userDAO   = new Dao_UtilisateurImp();
    // Dans les attributs de ClientHandler — ajouter ces deux
    private final DaoConversationImp convDAO    = new DaoConversationImp();
    private final Dao_MessageImp     messageDAO = new Dao_MessageImp();
    private final Dao_MessageFileAttenteImp fileDAO = new Dao_MessageFileAttenteImp();
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

                // LOGIN_OK|nom_complet|numero_telephone
                pw.println(Protocol.LOGIN_OK + "|" + u.getNomComplet() + "|" + u.getNumeroTelephone());

                messageRouter.delivrerMessagesEnAttente(telephoneConnecte);

            } else {
                pw.println(Protocol.LOGIN_FAIL+"|ErreurLogin");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.LOGIN_FAIL);
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

    // ── GROUPES V2 ───────────────────────────────────────────────────────────
    private void handleCreateGroup(String[] parts) {
        try {
            if (parts.length < 3) {
                pw.println(Protocol.CREATE_GROUP_FAIL.name() + "|Données invalides");
                return;
            }
            Groupe groupe = new Groupe();
            synchronized (ClientHandler.class) {
                groupe.setIdGroupe(sequenceGroupe++);
            }
            groupe.setNomGroupe(parts[1]);
            groupe.setNumeroCreateur(parts[2]);
            groupe.setDateCreation(LocalDateTime.now().toString());
            List<String> membres = new ArrayList<>();
            membres.add(parts[2]);
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].isBlank() && !membres.contains(parts[i])) membres.add(parts[i]);
            }
            groupe.setNumerosMembres(membres);
            groupes.put(groupe.getIdGroupe(), groupe);
            messagesGroupes.put(groupe.getIdGroupe(), new ArrayList<>());
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
        String numero = parts.length >= 2 ? parts[1] : telephoneConnecte;
        StringBuilder sb = new StringBuilder();
        for (Groupe g : groupes.values()) {
            if (g.getNumerosMembres() == null || !g.getNumerosMembres().contains(numero)) continue;
            if (sb.length() > 0) sb.append("|");
            sb.append(g.getIdGroupe()).append(";")
                    .append(g.getNomGroupe()).append(";")
                    .append(g.getNumeroCreateur()).append(";")
                    .append(g.getNumerosMembres() != null ? g.getNumerosMembres().size() : 0);
        }
        pw.println(Protocol.GROUPS_LIST.name() + "|" + sb);
    }

    private void handleSendGroupMessage(String[] parts) {
        if (parts.length < 4) return;
        int idGroupe = Integer.parseInt(parts[1]);
        Groupe g = groupes.get(idGroupe);
        if (g == null) return;

        MessageGroupe msg = new MessageGroupe();
        synchronized (ClientHandler.class) {
            msg.setIdMessage(sequenceMessageGroupe++);
        }
        msg.setIdGroupe(idGroupe);
        msg.setTelephoneExpediteur(parts[2]);
        msg.setNomExpediteur(parts[2]);
        msg.setContenu(parts[3]);
        msg.setDateEnvoi(LocalDateTime.now());

        List<MessageGroupe> liste = messagesGroupes.computeIfAbsent(idGroupe, k -> new ArrayList<>());
        synchronized (liste) { liste.add(msg); }

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
    }

    private void handleGetGroupMessages(String[] parts) {
        if (parts.length < 2) return;
        int idGroupe = Integer.parseInt(parts[1]);
        List<MessageGroupe> liste = messagesGroupes.getOrDefault(idGroupe, new ArrayList<>());
        StringBuilder sb = new StringBuilder();
        synchronized (liste) {
            for (MessageGroupe msg : liste) {
                if (sb.length() > 0) sb.append("|");
                sb.append(msg.getIdMessage()).append(";")
                        .append(msg.getTelephoneExpediteur()).append(";")
                        .append(msg.getNomExpediteur()).append(";")
                        .append(msg.getContenu()).append(";")
                        .append(msg.getDateEnvoi() != null ? msg.getDateEnvoi().toString() : "");
            }
        }
        pw.println(Protocol.GROUP_MESSAGES_LIST.name() + "|" + sb);
    }

    private void handleAddGroupMember(String[] parts) {
        if (parts.length < 4) return;
        int idGroupe = Integer.parseInt(parts[1]);
        String numAdmin = parts[2];
        String numNew = parts[3];
        Groupe g = groupes.get(idGroupe);
        if (g == null || !numAdmin.equals(g.getNumeroCreateur())) {
            pw.println(Protocol.ADD_GROUP_MEMBER_FAIL.name() + "|Permission refusée");
            return;
        }
        synchronized (g) {
            if (!g.getNumerosMembres().contains(numNew)) g.getNumerosMembres().add(numNew);
        }
        pw.println(Protocol.ADD_GROUP_MEMBER_OK.name() + "|" + idGroupe + "|" + numNew);
    }

    private void handleRemoveGroupMember(String[] parts) {
        if (parts.length < 4) return;
        int idGroupe = Integer.parseInt(parts[1]);
        String numAdmin = parts[2];
        String numMembre = parts[3];
        Groupe g = groupes.get(idGroupe);
        if (g == null || !numAdmin.equals(g.getNumeroCreateur())) {
            pw.println(Protocol.REMOVE_GROUP_MEMBER_FAIL.name() + "|Permission refusée");
            return;
        }
        synchronized (g) { g.getNumerosMembres().remove(numMembre); }
        pw.println(Protocol.REMOVE_GROUP_MEMBER_OK.name() + "|" + idGroupe + "|" + numMembre);
    }

    private void handleQuitGroup(String[] parts) {
        if (parts.length < 3) return;
        int idGroupe = Integer.parseInt(parts[1]);
        String numero = parts[2];
        Groupe g = groupes.get(idGroupe);
        if (g == null) return;
        synchronized (g) { g.getNumerosMembres().remove(numero); }
        pw.println(Protocol.QUIT_GROUP_OK.name() + "|" + idGroupe);
    }

    private void handleDeleteGroup(String[] parts) {
        if (parts.length < 3) return;
        int idGroupe = Integer.parseInt(parts[1]);
        String admin = parts[2];
        Groupe g = groupes.get(idGroupe);
        if (g == null) return;
        if (!admin.equals(g.getNumeroCreateur())) return;
        groupes.remove(idGroupe);
        messagesGroupes.remove(idGroupe);
        pw.println(Protocol.DELETE_GROUP_OK.name() + "|" + idGroupe);
    }

    private void handleRenameGroup(String[] parts) {
        if (parts.length < 4) return;
        int idGroupe = Integer.parseInt(parts[1]);
        String admin = parts[2];
        String nouveauNom = parts[3];
        Groupe g = groupes.get(idGroupe);
        if (g == null) return;
        if (!admin.equals(g.getNumeroCreateur())) return;
        g.setNomGroupe(nouveauNom);
        pw.println(Protocol.RENAME_GROUP_OK.name() + "|" + idGroupe + "|" + nouveauNom);
    }

}
