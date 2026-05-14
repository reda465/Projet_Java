package Serveur;

import Dao.*;
import model.Conversation;
import model.Message;
import model.MessageFileAttente;
import model.Utilisateur;
import util.SqlMessageTypeUtil;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class MessageRouter {

    private static MessageRouter instance;

    private final UserManager               userManager    = UserManager.getInstance();
    private final Dao_MessageImp            messageDAO     = new Dao_MessageImp();
    private final Dao_MessageFileAttenteImp fileDAO        = new Dao_MessageFileAttenteImp();
    private final DaoConversationImp        convDAO        = new DaoConversationImp();
    private final Dao_UtilisateurImp        utilisateurDAO = new Dao_UtilisateurImp();

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

    /**
     * Enregistre un message avec pièce jointe (conversation individuelle) et notifie le destinataire.
     *
     * @param fichierStocke nom du fichier dans le répertoire server_media (ex. uuid_nom.pdf)
     * @return id du message créé, ou -1 si échec (utilisateurs introuvables, etc.)
     */
    public int enregistrerMessageFichier(String telephoneExpediteur,
                                          String telephoneDest,
                                          String fichierStocke,
                                          String nomFichierOriginal,
                                          long tailleOctets,
                                          String typeMessage,
                                          String legende) throws SQLException {

        Utilisateur expediteur = utilisateurDAO.findByTelephone(telephoneExpediteur);
        Utilisateur destinataire = utilisateurDAO.findByTelephone(telephoneDest);
        if (expediteur == null || destinataire == null) {
            System.out.println("[FILE] Utilisateur introuvable : "
                    + telephoneExpediteur + " → " + telephoneDest);
            return -1;
        }
        Dao_ContactImp contactDAO = new Dao_ContactImp();
        if (contactDAO.estBloque(destinataire.getIdUtilisateur(),
                expediteur.getIdUtilisateur())) {
            System.out.println("[FILE] Bloqué — " + telephoneExpediteur
                    + " est bloqué par " + telephoneDest);
            ClientHandler expHandler = userManager.getHandler(telephoneExpediteur);
            if (expHandler != null)
                expHandler.sendMessage("MSG_FAIL|BLOQUE");
            return -1;
        }

        Conversation conv = convDAO.findIndividuelle(
                expediteur.getIdUtilisateur(),
                destinataire.getIdUtilisateur());

        if (conv == null) {
            conv = new Conversation();
            conv.setTypeConversation("individuelle");
            conv.setNomGroupe(null);
            conv.setIdCreateur(null);

            int idConv = convDAO.Add(conv);
            conv.setIdConversation(idConv);

            convDAO.ajouterParticipant(conv.getIdConversation(),
                    expediteur.getIdUtilisateur());
            convDAO.ajouterParticipant(conv.getIdConversation(),
                    destinataire.getIdUtilisateur());
        }

        Message msg = new Message() {
            @Override
            public String toNetworkString() {
                return "";
            }
        };
        msg.setIdConversation(conv.getIdConversation());
        msg.setIdExpediteur(expediteur.getIdUtilisateur());
        msg.setTypeMessage(SqlMessageTypeUtil.pourStockageIndividuel(
                typeMessage != null ? typeMessage : "fichier"));
        msg.setContenuTexte(legende != null ? legende : "");
        msg.setUrlFichier(fichierStocke);
        msg.setNomFichier(nomFichierOriginal);
        msg.setTailleFichier(tailleOctets);
        messageDAO.Add(msg);

        convDAO.updateDateDernierMessage(conv.getIdConversation());

        String nomWire = Base64.getEncoder().encodeToString(
                (nomFichierOriginal != null ? nomFichierOriginal : "fichier").getBytes(StandardCharsets.UTF_8));
        String capWire = Base64.getEncoder().encodeToString(
                (legende != null ? legende : "").getBytes(StandardCharsets.UTF_8));

        String typeNotify = SqlMessageTypeUtil.pourAffichage(msg.getTypeMessage(), nomFichierOriginal);
        String ligne = Protocol.MSG_FILE_NOTIFY.name() + "|"
                + expediteur.getNumeroTelephone() + "|"
                + msg.getIdMessage() + "|"
                + nomWire + "|"
                + typeNotify + "|"
                + tailleOctets + "|"
                + capWire;

        ClientHandler destHandler = userManager.getHandler(telephoneDest);
        if (destHandler != null) {
            destHandler.sendMessage(ligne);
            System.out.println("[FILE] Notifié à " + telephoneDest + " message " + msg.getIdMessage());
        } else {
            fileDAO.ajouterEnAttente(msg.getIdMessage(),
                    destinataire.getIdUtilisateur());
            System.out.println("[FILE] Mis en attente pour " + telephoneDest);
        }
        return msg.getIdMessage();
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

        // 4. Envoyer chaque message
        for (MessageFileAttente mfa : attente) {
            Message msg = messageDAO.getByID(mfa.getIdMessage());
            if (msg == null) continue;

            Utilisateur expediteur = utilisateurDAO.getByID(msg.getIdExpediteur());
            if (expediteur == null) continue;

            String ligne;
            if ("texte".equals(msg.getTypeMessage())) {
                ligne = Protocol.MSG_RECEIVE.name() + "|"
                        + expediteur.getNumeroTelephone() + "|"
                        + (msg.getContenuTexte() != null ? msg.getContenuTexte() : "");
            } else {
                String nomWire = Base64.getEncoder().encodeToString(
                        (msg.getNomFichier() != null ? msg.getNomFichier() : "fichier")
                                .getBytes(StandardCharsets.UTF_8));
                String capWire = Base64.getEncoder().encodeToString(
                        (msg.getContenuTexte() != null ? msg.getContenuTexte() : "")
                                .getBytes(StandardCharsets.UTF_8));
                String typeN = SqlMessageTypeUtil.pourAffichage(msg.getTypeMessage(), msg.getNomFichier());
                ligne = Protocol.MSG_FILE_NOTIFY.name() + "|"
                        + expediteur.getNumeroTelephone() + "|"
                        + msg.getIdMessage() + "|"
                        + nomWire + "|"
                        + typeN + "|"
                        + (msg.getTailleFichier() != null ? msg.getTailleFichier() : 0L) + "|"
                        + capWire;
            }

            handler.sendMessage(ligne);
        }

        // 5. Marquer tous comme délivrés en DB
        fileDAO.marquerDelivres(utilisateur.getIdUtilisateur());
        System.out.println("[ATTENTE] Messages délivrés à " + telephone);
    }
}