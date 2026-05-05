package Serveur;

import lombok.Getter;
import lombok.Setter;
import model.Contact;
import model.Conversation;
import model.Message;
import model.Utilisateur;
import lombok.*;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
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
    // Dans les attributs de ClientHandler — ajouter ces deux
    private final DaoConversationImp convDAO    = new DaoConversationImp();
    private final Dao_MessageImp     messageDAO = new Dao_MessageImp();
    private final Dao_MessageFileAttenteImp fileDAO = new Dao_MessageFileAttenteImp();
    private final Dao_ContactImp contactDAO = new Dao_ContactImp();
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
                    case LOGIN    -> handleLogin(parts);
                    case REGISTER -> handleRegister(parts);
                    case LOGOUT   -> { handleLogout(); return; }
                    case MSG_SEND -> handleMessage(parts);
                    case GET_CONVERSATIONS -> handleGetConversations();
                    case GET_MESSAGES -> handleGetMessages(parts);
                    case CALL_REQUEST -> handleCallRequest(parts);
                    case CALL_ACCEPT  -> handleCallAccept(parts);
                    case CALL_REFUSE  -> handleCallRefuse(parts);
                    case CALL_END     -> handleCallEnd(parts);
                    case ADD_CONTACT      -> handleAddContact(parts);
                    case BLOCK_CONTACT    -> handleBlockContact(parts);
                    case CONTACT_ACCEPTED -> handleContactAccepted(parts);
                    //case CALL_CANCEL  -> handleCallCancel(parts);

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
    // ── ADD_CONTACT|telephoneContact ─────────────────────────────────────────
// Flux :
// 1. Alice ajoute Bob → serveur crée le contact côté Alice
// 2. Si Bob est en ligne → notifie Bob avec CONTACT_REQUEST
// 3. Bob répond CONTACT_ACCEPTED ou BLOCK_CONTACT
    private void handleAddContact(String[] parts) {
        if (parts.length < 2) {
            pw.println(Protocol.ADD_CONTACT_FAIL.name()); return;
        }

        String telephoneContact = parts[1].trim();
        String nomAffiche = parts.length >= 3 ? parts[2].trim() : "";

        try {
            // 1. Retrouver les deux utilisateurs
            Utilisateur moi = userDAO.findByTelephone(telephoneConnecte);
            if (moi == null) { pw.println(Protocol.ADD_CONTACT_FAIL.name()); return; }

            Utilisateur contact = userDAO.findByTelephone(telephoneContact);
            if (contact == null) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|NUMERO_INTROUVABLE");
                return;
            }

            // 2. Pas s'ajouter soi-même
            if (moi.getIdUtilisateur() == contact.getIdUtilisateur()) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|AJOUT_SOI_MEME");
                return;
            }

            // 3. Vérifier si Bob nous a bloqués
            if (contactDAO.estBloque(contact.getIdUtilisateur(),
                    moi.getIdUtilisateur())) {
                pw.println(Protocol.ADD_CONTACT_FAIL.name() + "|BLOQUE");
                return;
            }

            // 4. Créer le contact côté Alice (si pas déjà existant)
            if (!contactDAO.contactExiste(moi.getIdUtilisateur(),
                    contact.getIdUtilisateur())) {
                Contact c = new Contact();
                c.setIdUtilisateur(moi.getIdUtilisateur());
                c.setIdContactUtilisateur(contact.getIdUtilisateur());
                c.setNomAffiche(nomAffiche.isEmpty()
                        ? contact.getNomComplet() : nomAffiche);
                c.setEstBloque(false);
                contactDAO.Add(c);
            }

            // 5. Répondre à Alice : ADD_CONTACT_OK|telephone|nomComplet
            pw.println(Protocol.ADD_CONTACT_OK.name()
                    + "|" + contact.getNumeroTelephone()
                    + "|" + contact.getNomComplet());

            // 6. Notifier Bob si en ligne
            // Format : CONTACT_REQUEST|telephoneAlice|nomAlice
            // 6. Notifier Bob si en ligne
            ClientHandler contactHandler = userManager.getHandler(telephoneContact);
            if (contactHandler != null) {
                // Bob en ligne → notification immédiate
                contactHandler.sendMessage(
                        Protocol.CONTACT_REQUEST.name()
                                + "|" + moi.getNumeroTelephone()
                                + "|" + moi.getNomComplet()
                );
            } else {
                // Bob hors ligne → stocker la notification en file d'attente
                // On réutilise messages_file_attente avec un type spécial
                // OU on envoie le CONTACT_REQUEST à sa prochaine connexion
                // → on le stocke dans une table dédiée ou on le livre au LOGIN
                System.out.println("[CONTACT] " + telephoneContact
                        + " hors ligne — CONTACT_REQUEST mis en attente");
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

            // Créer le contact côté Bob
            if (!contactDAO.contactExiste(moi.getIdUtilisateur(),
                    alice.getIdUtilisateur())) {
                Contact c = new Contact();
                c.setIdUtilisateur(moi.getIdUtilisateur());
                c.setIdContactUtilisateur(alice.getIdUtilisateur());
                c.setNomAffiche(alice.getNomComplet());
                c.setEstBloque(false);
                contactDAO.Add(c);
            }

            // Notifier Alice que Bob a accepté
            ClientHandler aliceHandler = userManager.getHandler(telephoneAlice);
            if (aliceHandler != null) {
                aliceHandler.sendMessage(
                        Protocol.CONTACT_ACCEPTED.name()
                                + "|" + moi.getNumeroTelephone()
                                + "|" + moi.getNomComplet()
                );
            }

            pw.println(Protocol.CONTACT_ACCEPTED.name() + "|OK");
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

}
