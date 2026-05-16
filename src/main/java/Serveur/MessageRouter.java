package Serveur;

import Dao.*;
import model.Conversation;
import model.Groupe;
import model.Message;
import model.MessageFileAttente;
import model.MessageGroupe;
import model.Utilisateur;
import util.FileMediaUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MessageRouter {

    private static MessageRouter instance;

    private final UserManager               userManager    = UserManager.getInstance();
    private final Dao_MessageImp            messageDAO     = new Dao_MessageImp();
    private final Dao_MessageFileAttenteImp fileDAO        = new Dao_MessageFileAttenteImp();
    private final DaoConversationImp        convDAO        = new DaoConversationImp();
    private final Dao_UtilisateurImp        utilisateurDAO = new Dao_UtilisateurImp();
    private final Dao_GroupeImp             groupeDAO      = new Dao_GroupeImp();
    private final Dao_MessageGroupeImp      messageGroupeDAO = new Dao_MessageGroupeImp();

    private MessageRouter() {}

    public static synchronized MessageRouter getInstance() {
        if (instance == null) instance = new MessageRouter();
        return instance;
    }

    // ── ENVOYER UN MESSAGE ────────────────────────────────────────────────────
    // Format reçu du ClientHandler : MSG_SEND|telephoneDest|contenu
    public void envoyerMessage(String telephoneExpediteur,
                               String telephoneDest,
                               String contenu) throws SQLException {

        // 1. Retrouver les deux utilisateurs en DB
        Utilisateur expediteur   = utilisateurDAO.findByTelephone(telephoneExpediteur);
        Utilisateur destinataire = utilisateurDAO.findByTelephone(telephoneDest);
        if (expediteur == null || destinataire == null) {
            System.out.println("[MSG] Utilisateur introuvable : "
                    + telephoneExpediteur + " → " + telephoneDest);
            return;
        }
        Dao_ContactImp contactDAO = new Dao_ContactImp();
        if (contactDAO.estBloque(destinataire.getIdUtilisateur(),
                expediteur.getIdUtilisateur())) {
            System.out.println("[MSG] Bloqué — " + telephoneExpediteur
                    + " est bloqué par " + telephoneDest);
            ClientHandler expHandler = userManager.getHandler(telephoneExpediteur);
            if (expHandler != null)
                expHandler.sendMessage("MSG_FAIL|BLOQUE");
            return;
        }

        // 2. Trouver ou créer la conversation individuelle
        Conversation conv = convDAO.findIndividuelle(
                expediteur.getIdUtilisateur(),
                destinataire.getIdUtilisateur());

        if (conv == null) {
            // Première fois qu'ils se parlent → créer la conversation
            conv = new Conversation();
            conv.setTypeConversation("individuelle");
            conv.setNomGroupe(null);
            conv.setIdCreateur(null);

            int idConv = convDAO.Add(conv);
            conv.setIdConversation(idConv);

            // Ajouter les deux participants
            convDAO.ajouterParticipant(conv.getIdConversation(),
                    expediteur.getIdUtilisateur());
            convDAO.ajouterParticipant(conv.getIdConversation(),
                    destinataire.getIdUtilisateur());
        }

        // 3. Persister le message en DB
        Message msg = new Message() {
            @Override
            public String toNetworkString() {
                return "";
            }
        };
        msg.setIdConversation(conv.getIdConversation());
        msg.setIdExpediteur(expediteur.getIdUtilisateur());
        msg.setTypeMessage("texte");
        msg.setContenuTexte(contenu);
        msg.setUrlFichier(null);
        msg.setNomFichier(null);
        msg.setTailleFichier(null);
        messageDAO.Add(msg);

        // 4. Mettre à jour date_dernier_message
        convDAO.updateDateDernierMessage(conv.getIdConversation());

        // 5. Construire la ligne protocole pour le destinataire
        // Format : MSG_RECEIVE|telephoneExp|nomExp|contenu|idMessage
        String ligne = Protocol.MSG_RECEIVE.name()        + "|"
                + expediteur.getNumeroTelephone()    + "|"
                + contenu                            ;

        // 6. Destinataire en ligne → direct | hors ligne → file d'attente
        ClientHandler destHandler = userManager.getHandler(telephoneDest);
        if (destHandler != null) {
            destHandler.sendMessage(ligne);
            System.out.println("[MSG] Livré directement à " + telephoneDest);
        } else {
            fileDAO.ajouterEnAttente(msg.getIdMessage(),
                    destinataire.getIdUtilisateur());
            System.out.println("[MSG] Mis en attente pour " + telephoneDest);
        }
    }

    // ── DÉLIVRER LES MESSAGES EN ATTENTE ─────────────────────────────────────
    // Appelé dans ClientHandler.handleLogin() après un login réussi
    public void delivrerMessagesEnAttente(String telephone) throws SQLException {

        // 1. Retrouver l'utilisateur
        Utilisateur utilisateur = utilisateurDAO.findByTelephone(telephone);
        if (utilisateur == null) return;

        // 2. Récupérer ses messages en attente
        List<MessageFileAttente> attente = fileDAO.getMessagesEnAttente(
                utilisateur.getIdUtilisateur());

        if (attente.isEmpty()) return;

        // 3. Récupérer son handler pour lui envoyer
        ClientHandler handler = userManager.getHandler(telephone);
        if (handler == null) return;

        System.out.println("[ATTENTE] " + attente.size()
                + " message(s) en attente pour " + telephone);

        // 4. Envoyer chaque message (texte ou fichier)
        for (MessageFileAttente mfa : attente) {
            Message msg = messageDAO.getByID(mfa.getIdMessage());
            if (msg == null) continue;

            Utilisateur expediteur = utilisateurDAO.getByID(msg.getIdExpediteur());
            if (expediteur == null) continue;

            String type = msg.getTypeMessage() != null ? msg.getTypeMessage() : "texte";
            if ("texte".equals(type)) {
                String ligne = Protocol.MSG_RECEIVE.name() + "|"
                        + expediteur.getNumeroTelephone() + "|"
                        + (msg.getContenuTexte() != null ? msg.getContenuTexte() : "");
                handler.sendMessage(ligne);
            } else {
                try {
                    byte[] bytes = FileStorage.read(msg.getUrlFichier());
                    String b64 = FileStorage.toBase64(bytes);
                    String nom = msg.getNomFichier() != null ? msg.getNomFichier() : "fichier";
                    String ligne = Protocol.FILE_RECEIVE.name() + "|"
                            + expediteur.getNumeroTelephone() + "|"
                            + type + "|"
                            + nom + "|"
                            + b64;
                    handler.sendMessage(ligne);
                } catch (IOException e) {
                    System.out.println("[ATTENTE] Fichier introuvable id_message=" + msg.getIdMessage());
                }
            }
        }

        // 5. Marquer tous comme délivrés en DB
        fileDAO.marquerDelivres(utilisateur.getIdUtilisateur());
        System.out.println("[ATTENTE] Messages délivrés à " + telephone);
    }

    // ── ENVOYER UN FICHIER (1:1) — même flux que texte + persistance fichier ─
    public void envoyerFichier(String telephoneExpediteur, String telephoneDest,
                               String typeMessage, String nomFichier, byte[] fileBytes,
                               ClientHandler expHandler) throws SQLException, IOException {

        Utilisateur expediteur = utilisateurDAO.findByTelephone(telephoneExpediteur);
        Utilisateur destinataire = utilisateurDAO.findByTelephone(telephoneDest);
        if (expediteur == null || destinataire == null) {
            if (expHandler != null) expHandler.sendMessage(Protocol.FILE_FAIL.name() + "|USER_NOT_FOUND");
            return;
        }

        Dao_ContactImp contactDAO = new Dao_ContactImp();
        if (contactDAO.estBloque(destinataire.getIdUtilisateur(), expediteur.getIdUtilisateur())) {
            if (expHandler != null) expHandler.sendMessage(Protocol.FILE_FAIL.name() + "|BLOQUE");
            return;
        }

        Conversation conv = convDAO.findIndividuelle(
                expediteur.getIdUtilisateur(), destinataire.getIdUtilisateur());
        if (conv == null) {
            conv = new Conversation();
            conv.setTypeConversation("individuelle");
            int idConv = convDAO.Add(conv);
            conv.setIdConversation(idConv);
            convDAO.ajouterParticipant(conv.getIdConversation(), expediteur.getIdUtilisateur());
            convDAO.ajouterParticipant(conv.getIdConversation(), destinataire.getIdUtilisateur());
        }

        String url = FileStorage.saveForConversation(conv.getIdConversation(), nomFichier, fileBytes);

        Message msg = new Message() {
            @Override
            public String toNetworkString() { return ""; }
        };
        msg.setIdConversation(conv.getIdConversation());
        msg.setIdExpediteur(expediteur.getIdUtilisateur());
        msg.setTypeMessage(typeMessage != null ? typeMessage : "fichier");
        msg.setContenuTexte(FileMediaUtil.labelForType(typeMessage) + " : " + nomFichier);
        msg.setUrlFichier(url);
        msg.setNomFichier(nomFichier);
        msg.setTailleFichier((long) fileBytes.length);
        messageDAO.Add(msg);

        convDAO.updateDateDernierMessage(conv.getIdConversation());

        String b64 = FileStorage.toBase64(fileBytes);
        String ligne = Protocol.FILE_RECEIVE.name() + "|"
                + expediteur.getNumeroTelephone() + "|"
                + msg.getTypeMessage() + "|"
                + nomFichier + "|"
                + b64;

        ClientHandler destHandler = userManager.getHandler(telephoneDest);
        if (destHandler != null) {
            destHandler.sendMessage(ligne);
            if (expHandler != null) expHandler.sendMessage(Protocol.FILE_OK.name() + "|" + nomFichier);
            System.out.println("[FILE] Livré directement à " + telephoneDest);
        } else {
            fileDAO.ajouterEnAttente(msg.getIdMessage(), destinataire.getIdUtilisateur());
            if (expHandler != null) expHandler.sendMessage(Protocol.FILE_OK.name() + "|QUEUED|" + nomFichier);
            System.out.println("[FILE] Mis en attente pour " + telephoneDest);
        }
    }

    // ── ENVOYER UN FICHIER (groupe) ───────────────────────────────────────────
    public void envoyerFichierGroupe(String telephoneExpediteur, int idGroupe,
                                     String typeMessage, String nomFichier, byte[] fileBytes,
                                     ClientHandler expHandler) throws Exception {

        Groupe g = groupeDAO.getById(idGroupe);
        if (g == null) {
            if (expHandler != null) expHandler.sendMessage(Protocol.FILE_FAIL.name() + "|GROUP_NOT_FOUND");
            return;
        }

        Utilisateur exp = utilisateurDAO.findByTelephone(telephoneExpediteur);
        String nomExp = exp != null ? exp.getNomComplet() : telephoneExpediteur;

        String url = FileStorage.saveForGroup(idGroupe, nomFichier, fileBytes);
        String contenu = FileMediaUtil.buildGroupFileContent(
                typeMessage != null ? typeMessage : "fichier", nomFichier, url);

        MessageGroupe msg = new MessageGroupe();
        msg.setIdGroupe(idGroupe);
        msg.setTelephoneExpediteur(telephoneExpediteur);
        msg.setNomExpediteur(nomExp);
        msg.setContenu(contenu);
        msg.setDateEnvoi(LocalDateTime.now());
        int idMsg = messageGroupeDAO.ajouter(msg);
        msg.setIdMessage(idMsg);

        String b64 = FileStorage.toBase64(fileBytes);
        String payload = Protocol.FILE_GROUP_RECEIVE.name() + "|"
                + idGroupe + "|"
                + telephoneExpediteur + "|"
                + nomExp + "|"
                + (typeMessage != null ? typeMessage : "fichier") + "|"
                + nomFichier + "|"
                + b64;

        String telExpNorm = normaliserTel(telephoneExpediteur);
        if (g.getNumerosMembres() != null) {
            for (String membre : g.getNumerosMembres()) {
                if (membre == null || membre.isBlank()) continue;
                if (normaliserTel(membre).equals(telExpNorm)) continue;
                ClientHandler h = userManager.getHandler(membre.trim());
                if (h != null) h.sendMessage(payload);
            }
        }
        if (expHandler != null) expHandler.sendMessage(Protocol.FILE_OK.name() + "|" + nomFichier);
    }

    /** Lit un fichier groupé depuis son contenu DB et renvoie le base64 (historique). */
    private static String normaliserTel(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", "").replace("-", "");
    }

    /** Fichiers groupe envoyés pendant l'absence — livrés à la reconnexion. */
    public void delivrerFichiersGroupeEnAttente(String telephone, java.time.LocalDateTime depuis)
            throws Exception {
        Utilisateur u = utilisateurDAO.findByTelephone(telephone);
        if (u == null) return;
        ClientHandler handler = userManager.getHandler(telephone);
        if (handler == null) return;

        String telNorm = normaliserTel(telephone);
        List<Groupe> groupes = groupeDAO.getGroupesPourMembre(telephone);
        for (Groupe g : groupes) {
            List<MessageGroupe> msgs = messageGroupeDAO.getByGroupe(g.getIdGroupe());
            for (MessageGroupe msg : msgs) {
                if (!FileMediaUtil.isGroupFileContent(msg.getContenu())) continue;
                if (normaliserTel(msg.getTelephoneExpediteur()).equals(telNorm)) continue;
                if (depuis != null && msg.getDateEnvoi() != null && !msg.getDateEnvoi().isAfter(depuis)) {
                    continue;
                }
                String[] meta = FileMediaUtil.parseGroupFileContent(msg.getContenu());
                if (meta == null) continue;
                try {
                    byte[] bytes = FileStorage.read(meta[2]);
                    String b64 = FileStorage.toBase64(bytes);
                    String payload = Protocol.FILE_GROUP_RECEIVE.name() + "|"
                            + g.getIdGroupe() + "|"
                            + msg.getTelephoneExpediteur() + "|"
                            + msg.getNomExpediteur() + "|"
                            + meta[0] + "|"
                            + meta[1] + "|"
                            + b64;
                    handler.sendMessage(payload);
                } catch (IOException e) {
                    System.out.println("[FILE GROUPE] Fichier introuvable pour message " + msg.getIdMessage());
                }
            }
        }
    }

    public static String lireBase64DepuisContenuGroupe(String contenu) {
        String[] meta = FileMediaUtil.parseGroupFileContent(contenu);
        if (meta == null) return null;
        try {
            return FileStorage.toBase64(FileStorage.read(meta[2]));
        } catch (IOException e) {
            return null;
        }
    }
}