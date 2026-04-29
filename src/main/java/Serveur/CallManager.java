package Serveur;

import model.Appel;
import model.Conversation;
import model.Utilisateur;
import model.enums.StatutAppel;
import model.enums.TypeAppel;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {
    private static CallManager instance;
    private final ConcurrentHashMap<String, Long> appelsEnCours = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> idAppels = new ConcurrentHashMap<>();

    private final UserManager userManager = UserManager.getInstance();

    private CallManager() {}

    public static synchronized CallManager getInstance() {
        if (instance == null) instance = new CallManager();
        return instance;
    }

    // Appelant demande un appel
    public void demanderAppel(String telAppelant, String telDest, String typeAppel) {
        ClientHandler dest = userManager.getHandler(telDest);
        if (dest == null) {
            ClientHandler appelant = userManager.getHandler(telAppelant);
            if (appelant != null) appelant.sendMessage("CALL_END|" + telDest + "|HORS_LIGNE");
            return;
        }

        // Stocker l'appel
        appelsEnCours.put(telAppelant, System.currentTimeMillis());

        // Récupérer l'IP de l'appelant
        String ipAppelant = userManager.getIP(telAppelant);

        // Envoyer au destinataire avec l'IP
        dest.sendMessage("CALL_REQUEST|" + telAppelant + "|" + telDest + "|" + typeAppel + "|0|" + ipAppelant);

        System.out.println("[APPEL] " + telAppelant + " appelle " + telDest);
    }

    // Destinataire accepte
    public void accepterAppel(String telAccepteur, String telAppelant) {
        ClientHandler appelant = userManager.getHandler(telAppelant);
        if (appelant == null) return;

        // Récupérer l'IP de celui qui accepte
        String ipAccepteur = userManager.getIP(telAccepteur);

        // Envoyer à l'appelant avec l'IP
        appelant.sendMessage("CALL_ACCEPT|" + telAccepteur + "|" + ipAccepteur);

        System.out.println("[APPEL] Accepté par " + telAccepteur);
    }

    // Refuser
    public void refuserAppel(String telRefuseur, String telAppelant) {
        ClientHandler appelant = userManager.getHandler(telAppelant);
        if (appelant != null) appelant.sendMessage("CALL_REFUSE|" + telRefuseur);
        appelsEnCours.remove(telAppelant);
    }

    // Terminer
    public void terminerAppel(String telAppelant, String telDest) {
        ClientHandler dest = userManager.getHandler(telDest);
        if (dest != null) dest.sendMessage("CALL_END|" + telAppelant);
        appelsEnCours.remove(telAppelant);
    }

    // Annuler
    public void annulerAppel(String telAppelant, String telDest) {
        ClientHandler dest = userManager.getHandler(telDest);
        if (dest != null) dest.sendMessage("CALL_END|" + telAppelant + "|ANNULE");
        appelsEnCours.remove(telAppelant);
    }
}