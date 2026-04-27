package Serveur;

import model.Utilisateur;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import Dao.*;

public class ClientHandler extends Thread {

    private final Socket socket;
    private PrintWriter pw;
    private String telephoneConnecte; // null = pas encore authentifié
    private final MessageRouter messageRouter = MessageRouter.getInstance();
    private final CallManager callManager = CallManager.getInstance();
    private final UserManager      userManager = UserManager.getInstance();
    private final  Dao_UtilisateurImp userDAO   = new Dao_UtilisateurImp();

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
                    case CALL_REQUEST -> handleCallRequest(parts);
                    case CALL_ACCEPT  -> handleCallAccept(parts);
                    case CALL_REFUSE  -> handleCallRefuse(parts);
                    case CALL_END     -> handleCallEnd(parts);
                    case CALL_CANCEL  -> handleCallCancel(parts);

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

                broadcastUsersList();
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
            callManager.demanderAppel(telephoneConnecte, telephoneDest, typeAppel);
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
            callManager.accepterAppel(telephoneConnecte, telephoneAppelant);
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
}
