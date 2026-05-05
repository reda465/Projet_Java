package com.ensa;

import Dao.DaoConversationImp;
import Dao.Dao_MessageImp;
import Dao.Dao_UtilisateurImp;
import Dao.DataBase;
import model.Conversation;
import model.Message;
import model.Utilisateur;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Initialise la base MySQL (projet_java) avec des données de démonstration
 * adaptées à un contexte marocain (noms + numéros 06/07).
 *
 * Exécution:
 * - Configurer `Dao/DataBase.java` (URL/user/pass)
 * - Lancer la méthode main()
 */
public class DbSeeder {

    private final Dao_UtilisateurImp userDAO = new Dao_UtilisateurImp();
    private final DaoConversationImp convDAO = new DaoConversationImp();
    private final Dao_MessageImp messageDAO = new Dao_MessageImp();

    public static void main(String[] args) {
        new DbSeeder().run();
    }

    public void run() {
        if (!testConnexion()) return;

        try {
            // Utilisateurs (numéros marocains)
            Utilisateur maryam = upsertUser("Maryam El Amrani", "0611221122", "pass123");
            Utilisateur ali    = upsertUser("Ali Benjelloun",   "0611111111", "pass123");
            Utilisateur sara   = upsertUser("Sara Ait Lahcen",  "0622222222", "pass123");
            Utilisateur moha   = upsertUser("Mohamed Lahlou",   "0633333333", "pass123");
            Utilisateur ikram  = upsertUser("Ikram Berrada",    "0644444444", "pass123");
            Utilisateur youssef= upsertUser("Youssef El Idrissi","0712345678", "pass123");

            // Conversations individuelles + messages
            seedIndividuelle(maryam, ali, List.of(
                    "Salam Ali, kif dayr ?",
                    "Labas Maryam, hamdollah. W nti ?",
                    "Kantwjad l'projet, nchallah ghadi ykoun mzyan."
            ));

            seedIndividuelle(maryam, sara, List.of(
                    "Sba7 lkhir Sara!",
                    "Sba7 nour, wach lmeeting b9a f 20h ?",
                    "Ah, f Zoom. Ghadi nsift lik lien."
            ));

            seedIndividuelle(maryam, moha, List.of(
                    "Mohamed, wach t9dert tchouf l'API dyal messages ?",
                    "Oui, kayna wahed l'fix f parsing.",
                    "Mzyan, merci بزاف!"
            ));

            seedIndividuelle(ali, ikram, List.of(
                    "Ikram, fin wslti f UI ?",
                    "Baqi chi tweaks f colors w styles.",
                    "Ok, khallina nراجعوه m3a ba3d."
            ));

            seedIndividuelle(youssef, maryam, List.of(
                    "Bonsoir Maryam, wach l'base جاهزة ?",
                    "Oui, daba seed mzyan باش نجربو.",
                    "Perfect, nchallah!"
            ));

            System.out.println("✅ Seed terminé.");
        } catch (SQLException e) {
            System.err.println("❌ Seed échoué: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean testConnexion() {
        try (Connection c = DataBase.getConnection()) {
            if (c == null) {
                System.err.println("❌ Connexion DB = null. Vérifie `Dao/DataBase.java`.");
                return false;
            }
            if (c.isClosed()) {
                System.err.println("❌ Connexion DB fermée. Vérifie MySQL.");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("❌ Impossible d'ouvrir une connexion DB: " + e.getMessage());
            return false;
        }
    }

    private Utilisateur upsertUser(String nomComplet, String numero, String password) throws SQLException {
        Utilisateur existant = userDAO.findByTelephone(numero);
        if (existant != null) return existant;

        Utilisateur u = new Utilisateur();
        u.setNomComplet(nomComplet);
        u.setNumeroTelephone(numero);
        u.setMotDePasse(password);
        userDAO.Add(u);

        Utilisateur created = userDAO.findByTelephone(numero);
        if (created == null) {
            throw new SQLException("Utilisateur non retrouvé après insertion: " + numero);
        }
        return created;
    }

    private void seedIndividuelle(Utilisateur a, Utilisateur b, List<String> messages) throws SQLException {
        if (a == null || b == null) return;

        Conversation conv = convDAO.findIndividuelle(a.getIdUtilisateur(), b.getIdUtilisateur());
        if (conv == null) {
            Conversation c = new Conversation();
            c.setTypeConversation("individuelle");
            c.setNomGroupe(null);
            c.setIdCreateur(null);
            int idConv = convDAO.Add(c);
            if (idConv <= 0) throw new SQLException("Impossible de créer une conversation individuelle");
            convDAO.ajouterParticipant(idConv, a.getIdUtilisateur());
            convDAO.ajouterParticipant(idConv, b.getIdUtilisateur());
            conv = convDAO.getByID(idConv);
        }

        // Insérer quelques messages alternés (a, b, a, b, ...)
        for (int i = 0; i < messages.size(); i++) {
            String contenu = messages.get(i);
            if (contenu == null || contenu.trim().isEmpty()) continue;

            int idExpediteur = (i % 2 == 0) ? a.getIdUtilisateur() : b.getIdUtilisateur();
            Message m = new Message();
            m.setIdConversation(conv.getIdConversation());
            m.setIdExpediteur(idExpediteur);
            m.setTypeMessage("texte");
            m.setContenuTexte(contenu);
            m.setUrlFichier(null);
            m.setNomFichier(null);
            m.setTailleFichier(null);

            messageDAO.Add(m);
        }

        convDAO.updateDateDernierMessage(conv.getIdConversation());
    }
}

