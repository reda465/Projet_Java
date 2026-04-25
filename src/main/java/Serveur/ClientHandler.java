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
                String   commande = parts[0];

                switch (commande) {
                    case Protocol.LOGIN    -> handleLogin(parts);
                    case Protocol.REGISTER -> handleRegister(parts);
                    case Protocol.LOGOUT   -> { handleLogout(); return; }
                    case Protocol.MSG_SEND -> handleMessage(parts);
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
            } else {
                pw.println(Protocol.LOGIN_FAIL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            pw.println(Protocol.LOGIN_FAIL);
        }
    }

    // ── REGISTER|nom_complet|numero_telephone|mot_de_passe ───────────────────
    private void handleRegister(String[] parts) {
        if (parts.length < 4) { pw.println("REGISTER_FAIL"); return; }

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
            pw.println("REGISTER_OK");
        } catch (SQLException e) {
            e.printStackTrace();
            pw.println("REGISTER_FAIL");
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
        // Implémenté au sprint messagerie
    }

    // ── Broadcast liste connectés ─────────────────────────────────────────────
    private void broadcastUsersList() {
        userManager.broadcast(Protocol.USERS_LIST + "|" + userManager.getOnlineUsersList());
    }

    // ── Envoyer un message à CE client ───────────────────────────────────────
    public void sendMessage(String message) {
        if (pw != null) pw.println(message);
    }
}
