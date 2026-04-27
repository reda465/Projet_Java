package Serveur;

import Dao.Dao_AppelImp;
import Dao.DaoConversationImp;
import Dao.Dao_UtilisateurImp;
import model.Appel;
import model.Conversation;
import model.Utilisateur;
import model.enums.StatutAppel;
import model.enums.TypeAppel;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {

    private static CallManager instance;

    // telephone appelant → timestamp de début d'appel (pour calculer la durée)
    private final ConcurrentHashMap<String, Long> appelsEnCours
            = new ConcurrentHashMap<>();

    // telephone appelant → idAppel en DB
    private final ConcurrentHashMap<String, Integer> idAppels
            = new ConcurrentHashMap<>();

    // Map pour relier les deux participants (pour le transfert audio)
    private final ConcurrentHashMap<String, String> participantMap
            = new ConcurrentHashMap<>();

    private final UserManager       userManager    = UserManager.getInstance();
    private final Dao_AppelImp      appelDAO       = new Dao_AppelImp();
    private final DaoConversationImp convDAO       = new DaoConversationImp();
    private final Dao_UtilisateurImp utilisateurDAO = new Dao_UtilisateurImp();

    private CallManager() {}

    public static synchronized CallManager getInstance() {
        if (instance == null) instance = new CallManager();
        return instance;
    }

    // ── CALL_REQUEST|telephoneDest|typeAppel ──────────────────────────────────
    public void demanderAppel(String telephoneAppelant,
                              String telephoneDest,
                              String typeAppel) throws SQLException {

        // 1. Vérifier que le destinataire est en ligne
        ClientHandler destHandler = userManager.getHandler(telephoneDest);
        if (destHandler == null) {
            // Destinataire hors ligne → notifier l'appelant
            ClientHandler appelantHandler = userManager.getHandler(telephoneAppelant);
            if (appelantHandler != null)
                appelantHandler.sendMessage(new network.Packet(
                        Protocol.CALL_END, telephoneDest + "|HORS_LIGNE"));
            return;
        }

        // 2. Retrouver les utilisateurs
        Utilisateur appelant    = utilisateurDAO.findByTelephone(telephoneAppelant);
        Utilisateur destinataire = utilisateurDAO.findByTelephone(telephoneDest);
        if (appelant == null || destinataire == null) return;

        // 3. Trouver ou créer la conversation
        Conversation conv = convDAO.findIndividuelle(
                appelant.getIdUtilisateur(),
                destinataire.getIdUtilisateur());

        if (conv == null) {
            conv = new Conversation();
            conv.setTypeConversation("individuelle");
            conv.setNomGroupe(null);
            conv.setIdCreateur(null);
            int idConv = convDAO.Add(conv);
            conv.setIdConversation(idConv);
            convDAO.ajouterParticipant(idConv, appelant.getIdUtilisateur());
            convDAO.ajouterParticipant(idConv, destinataire.getIdUtilisateur());
        }

        // 4. Persister l'appel en DB avec statut "en_cours"
        Appel appel = new Appel();
        appel.setIdAppelant(appelant.getIdUtilisateur());
        appel.setIdConversation(conv.getIdConversation());
        appel.setTypeAppel(TypeAppel.valueOf(typeAppel));
        appel.setStatut(StatutAppel.en_cours);
        appelDAO.Add(appel);

        // 5. Stocker en mémoire pour calculer la durée plus tard
        appelsEnCours.put(telephoneAppelant, System.currentTimeMillis());
        idAppels.put(telephoneAppelant, appel.getIdAppel());

        // 6. Notifier le destinataire
        destHandler.sendMessage(new network.Packet(
                Protocol.CALL_REQUEST,
                appelant.getNumeroTelephone() + "|"
                        + appelant.getNomComplet()      + "|"
                        + typeAppel                     + "|"
                        + appel.getIdAppel()
        ));

        System.out.println("[APPEL] " + telephoneAppelant
                + " → " + telephoneDest + " (" + typeAppel + ")");
    }

    // ── CALL_ACCEPT|telephoneAppelant ─────────────────────────────────────────
    public void accepterAppel(String telephoneAccepteur,
                              String telephoneAppelant) throws SQLException {

        // 1. Mettre à jour le statut en DB
        Integer idAppel = idAppels.get(telephoneAppelant);
        if (idAppel != null)
            appelDAO.updateStatut(idAppel, StatutAppel.accepte);

        // 2. Notifier l'appelant que son appel est accepté
        ClientHandler appelantHandler = userManager.getHandler(telephoneAppelant);
        if (appelantHandler != null)
            appelantHandler.sendMessage(new network.Packet(
                    Protocol.CALL_ACCEPT, telephoneAccepteur));

        // 3. Lier les deux participants pour l'audio
        participantMap.put(telephoneAccepteur, telephoneAppelant);
        participantMap.put(telephoneAppelant, telephoneAccepteur);

        System.out.println("[APPEL] Accepté par " + telephoneAccepteur);
    }

    // ── CALL_REFUSE|telephoneAppelant ─────────────────────────────────────────
    public void refuserAppel(String telephoneRefuseur,
                             String telephoneAppelant) throws SQLException {

        // 1. Mettre à jour le statut en DB
        Integer idAppel = idAppels.get(telephoneAppelant);
        if (idAppel != null) {
            appelDAO.updateStatut(idAppel, StatutAppel.refuse);
            idAppels.remove(telephoneAppelant);
            appelsEnCours.remove(telephoneAppelant);
        }

        // 2. Notifier l'appelant du refus
        ClientHandler appelantHandler = userManager.getHandler(telephoneAppelant);
        if (appelantHandler != null)
            appelantHandler.sendMessage(new network.Packet(
                    Protocol.CALL_REFUSE, telephoneRefuseur));

        System.out.println("[APPEL] Refusé par " + telephoneRefuseur);
    }

    // ── CALL_END|telephoneDest ────────────────────────────────────────────────
    public void terminerAppel(String telephoneAppelant,
                              String telephoneDest) throws SQLException {

        // 1. Calculer la durée
        Long debut = appelsEnCours.remove(telephoneAppelant);
        Integer idAppel = idAppels.remove(telephoneAppelant);

        int dureeSecondes = 0;
        if (debut != null)
            dureeSecondes = (int) ((System.currentTimeMillis() - debut) / 1000);

        // 2. Mettre à jour en DB
        if (idAppel != null)
            appelDAO.terminerAppel(idAppel, StatutAppel.accepte, dureeSecondes);

        // 3. Notifier l'autre participant
        ClientHandler destHandler = userManager.getHandler(telephoneDest);
        if (destHandler != null)
            destHandler.sendMessage(new network.Packet(
                    Protocol.CALL_END, telephoneAppelant));

        // 4. Retirer du map de participants
        participantMap.remove(telephoneAppelant);
        participantMap.remove(telephoneDest);

        System.out.println("[APPEL] Terminé — durée : " + dureeSecondes + "s");
    }

    // ── Appel manqué (appelant annule avant réponse) ──────────────────────────
    public void annulerAppel(String telephoneAppelant,
                             String telephoneDest) throws SQLException {

        Integer idAppel = idAppels.remove(telephoneAppelant);
        appelsEnCours.remove(telephoneAppelant);

        if (idAppel != null)
            appelDAO.updateStatut(idAppel, StatutAppel.manque);

        ClientHandler destHandler = userManager.getHandler(telephoneDest);
        if (destHandler != null)
            destHandler.sendMessage(new network.Packet(
                    Protocol.CALL_END, telephoneAppelant + "|ANNULE"));

        participantMap.remove(telephoneAppelant);
        participantMap.remove(telephoneDest);

        System.out.println("[APPEL] Annulé par " + telephoneAppelant);
    }

    public String getOtherParticipant(String telephone) {
        return participantMap.get(telephone);
    }
}