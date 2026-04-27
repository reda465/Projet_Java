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
    private final Dao_UtilisateurImp userDAO   = new Dao_UtilisateurImp();

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
                try {
                    network.Packet packet = network.Packet.fromString(ligne);

                    switch (packet.getProtocol()) {
                        case LOGIN    -> handleLogin(packet);
                        case REGISTER -> handleRegister(packet);
                        case LOGOUT   -> { handleLogout(); return; }
                        case MSG_SEND -> handleMessage(packet);
                        case CALL_REQUEST -> handleCallRequest(packet);
                        case CALL_ACCEPT  -> handleCallAccept(packet);
                        case CALL_REFUSE  -> handleCallRefuse(packet);
                        case CALL_END     -> handleCallEnd(packet);
                        case CALL_CANCEL  -> handleCallCancel(packet);
                        case Call_AUDIO_DATA -> handleMediaData(ligne);
                        case Call_VIDEO_DATA -> handleMediaData(ligne);

                        default                -> sendMessage(new network.Packet(Protocol.LOGIN_FAIL, "UNKNOWN_COMMAND"));
                    }
                } catch (Exception e) {
                    System.out.println("Erreur parsing packet: " + ligne);
                }
            }

        } catch (IOException e) {
            System.out.println("Connexion perdue : " + telephoneConnecte);
        } finally {
            handleLogout();
        }
    }

    // ── LOGIN|numero_telephone|mot_de_passe ──────────────────────────────────
    private void handleLogin(network.Packet packet) {
        String data = (String) packet.getData();
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split("\\|");
        if (parts.length < 2) { sendMessage(new network.Packet(Protocol.LOGIN_FAIL, "")); return; }

        String tel      = parts[0];
        String password = parts[1];

        try {
            Utilisateur u = userDAO.findByTelAndPassword(tel, password);
            if (u != null) {
                telephoneConnecte = u.getNumeroTelephone();
                userManager.addUser(telephoneConnecte, this);
                userDAO.updateDerniereConnexion(u.getIdUtilisateur());

                // LOGIN_OK|nom_complet|numero_telephone
                sendMessage(new network.Packet(Protocol.LOGIN_OK, u.getNomComplet() + "|" + u.getNumeroTelephone()));

                broadcastUsersList();
                messageRouter.delivrerMessagesEnAttente(telephoneConnecte);

            } else {
                sendMessage(new network.Packet(Protocol.LOGIN_FAIL, "ErreurLogin"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(new network.Packet(Protocol.LOGIN_FAIL, ""));
        }
    }

    // ── REGISTER|nom_complet|numero_telephone|mot_de_passe ───────────────────
    private void handleRegister(network.Packet packet) {
        String data = (String) packet.getData();
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split("\\|");
        if (parts.length < 3) { sendMessage(new network.Packet(Protocol.REGISTER_FAIL, "Erreur_Inscription")); return; }

        Utilisateur u = new Utilisateur();
        u.setNomComplet(parts[0]);
        u.setNumeroTelephone(parts[1]);
        u.setMotDePasse(parts[2]);

        try {
            if (userDAO.telephoneExiste(u.getNumeroTelephone())) {
                sendMessage(new network.Packet(Protocol.REGISTER_FAIL, "TELEPHONE_EXISTE"));
                return;
            }
            userDAO.Add(u);
            sendMessage(new network.Packet(Protocol.REGISTER_OK, "Inscription_Avec_Succes"));
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(new network.Packet(Protocol.REGISTER_FAIL, "Erreur_Inscription"));
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
    private void handleMessage(network.Packet packet) {
        String data = (String) packet.getData();
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split("\\|");
        if (parts.length < 2) return;

        String telephoneDest = parts[0];
        String contenu = parts[1];

        try {
            messageRouter.envoyerMessage(telephoneConnecte, telephoneDest, contenu);
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(new network.Packet(Protocol.MSG_SEND, "Erreur_Envoi"));
        }
    }
    // ── Broadcast liste connectés ─────────────────────────────────────────────
    private void broadcastUsersList() {
        userManager.broadcast(new network.Packet(Protocol.USERS_LIST, userManager.getOnlineUsersList()));
    }

    // ── Envoyer un message à CE client ───────────────────────────────────────
    public void sendMessage(String message) {
        if (pw != null) pw.println(message);
    }
    public void sendMessage(network.Packet packet) {
        if (pw != null) pw.println(packet.toString());
    }
    // ── GESTION DES APPELS (Adaptée au format Packet) ─────────────────────────
    private void handleCallRequest(network.Packet packet) {
        String data = (String) packet.getData();
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split(";");
        String telephoneDest = (parts.length > 1) ? parts[1] : data;
        String typeAppel = "audio"; // Par défaut si non précisé
        try {
            callManager.demanderAppel(telephoneConnecte, telephoneDest, typeAppel);
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(new network.Packet(Protocol.CALL_END, "ERREUR"));
        }
    }

    private void handleCallAccept(network.Packet packet) {
        String telephoneAppelant = (String) packet.getData();
        try {
            callManager.accepterAppel(telephoneConnecte, telephoneAppelant);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleCallRefuse(network.Packet packet) {
        String telephoneAppelant = (String) packet.getData();
        try {
            callManager.refuserAppel(telephoneConnecte, telephoneAppelant);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleCallEnd(network.Packet packet) {
        String data = (String) packet.getData();
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split(";");
        String telephoneDest = (parts.length > 1) ? parts[1] : data;
        try {
            callManager.terminerAppel(telephoneConnecte, telephoneDest);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleCallCancel(network.Packet packet) {
        String telephoneDest = (String) packet.getData();
        try {
            callManager.annulerAppel(telephoneConnecte, telephoneDest);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Relayer l'audio/vidéo à l'autre participant
    private void handleMediaData(String ligne) {
        String otherParticipant = callManager.getOtherParticipant(telephoneConnecte);
        if (otherParticipant != null) {
            ClientHandler destHandler = userManager.getHandler(otherParticipant);
            if (destHandler != null) {
                destHandler.sendMessage(ligne); // Transfère le packet entier (avec données base64)
            }
        }
    }
}
