package Dao;

import model.Contact;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dao_ContactImp implements DAO_Contact {

    // ── ADD ──────────────────────────────────────────────────────────────────
    @Override
    public int Add(Contact c) throws SQLException {
        String sql = "INSERT INTO contacts (id_utilisateur, id_contact_utilisateur, nom_affiche) "
                + "VALUES (?, ?, ?)";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getIdUtilisateur());
            ps.setInt(2, c.getIdContactUtilisateur());
            ps.setString(3, c.getNomAffiche());
            return ps.executeUpdate();
        }
    }

    // ── MODIFY ───────────────────────────────────────────────────────────────
    @Override
    public int Modify(Contact c) throws SQLException {
        String sql = "UPDATE contacts SET nom_affiche=?, est_bloque=? "
                + "WHERE id_utilisateur=? AND id_contact_utilisateur=?";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNomAffiche());
            ps.setBoolean(2, c.isEstBloque());
            ps.setInt(3, c.getIdUtilisateur());
            ps.setInt(4, c.getIdContactUtilisateur());
            return ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    @Override
    public int Delete(Contact c) throws SQLException {
        String sql = "DELETE FROM contacts WHERE id_utilisateur=? "
                + "AND id_contact_utilisateur=?";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getIdUtilisateur());
            ps.setInt(2, c.getIdContactUtilisateur());
            return ps.executeUpdate();
        }
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @Override
    public Contact getByID(Integer id) throws SQLException {
        String sql = "SELECT * FROM contacts WHERE id_contact=?";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────
    @Override
    public List<Contact> getAll() throws SQLException {
        List<Contact> liste = new ArrayList<>();
        String sql = "SELECT * FROM contacts";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    // ── METHODES SPECIFIQUES ──────────────────────────────────────────────────

    /** Vérifie si le contact existe déjà */
    public boolean contactExiste(int idUtilisateur,
                                 int idContactUtilisateur) throws SQLException {
        String sql = "SELECT id_contact FROM contacts "
                + "WHERE id_utilisateur=? AND id_contact_utilisateur=?";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.setInt(2, idContactUtilisateur);
            return ps.executeQuery().next();
        }
    }

    /** Vérifie si l'utilisateur est bloqué */
    public boolean estBloque(int idUtilisateur,
                             int idContactUtilisateur) throws SQLException {
        String sql = "SELECT est_bloque FROM contacts "
                + "WHERE id_utilisateur=? AND id_contact_utilisateur=? "
                + "AND est_bloque=1";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.setInt(2, idContactUtilisateur);
            return ps.executeQuery().next();
        }
    }

    /** Bloque un contact */
    public void bloquerContact(int idUtilisateur,
                               int idContactUtilisateur) throws SQLException {
        // Si le contact existe → on le met à bloqué
        // Sinon → on le crée directement bloqué
        if (contactExiste(idUtilisateur, idContactUtilisateur)) {
            String sql = "UPDATE contacts SET est_bloque=1 "
                    + "WHERE id_utilisateur=? AND id_contact_utilisateur=?";
            try (Connection con = DataBase.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idUtilisateur);
                ps.setInt(2, idContactUtilisateur);
                ps.executeUpdate();
            }
        } else {
            Contact c = new Contact();
            c.setIdUtilisateur(idUtilisateur);
            c.setIdContactUtilisateur(idContactUtilisateur);
            c.setNomAffiche("Bloqué");
            c.setEstBloque(true);
            Add(c);
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Contact mapResultSet(ResultSet rs) throws SQLException {
        Contact c = new Contact();
        c.setIdContact(rs.getInt("id_contact"));
        c.setIdUtilisateur(rs.getInt("id_utilisateur"));
        c.setIdContactUtilisateur(rs.getInt("id_contact_utilisateur"));
        c.setNomAffiche(rs.getString("nom_affiche"));
        c.setEstBloque(rs.getBoolean("est_bloque"));
        Timestamp dateAjout = rs.getTimestamp("date_ajout");
        if (dateAjout != null)
            c.setDateAjout(dateAjout.toLocalDateTime());
        return c;
    }
    /** Stocke une demande de contact en attente pour un utilisateur hors ligne */
    public void ajouterDemandeEnAttente(int idDemandeur, int idDestinataire) throws SQLException {
        // Utiliser INSERT ... ON DUPLICATE KEY UPDATE pour éviter le silence
        String sql = "INSERT INTO contacts "
                + "(id_utilisateur, id_contact_utilisateur, nom_affiche, est_bloque) "
                + "VALUES (?, ?, 'PENDING', 0) "
                + "ON DUPLICATE KEY UPDATE "
                + "nom_affiche = IF(nom_affiche = 'PENDING', 'PENDING', VALUES(nom_affiche))";

        System.out.println("[DEBUG] Demande en attente : dest=" + idDestinataire
                + ", demandeur=" + idDemandeur);
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDestinataire);   // côté destinataire
            ps.setInt(2, idDemandeur);      // le contact = demandeur
            ps.executeUpdate();
        }
    }

    /** Récupère les demandes en attente pour un utilisateur à sa reconnexion */
    public List<Contact> getDemandesEnAttente(int idUtilisateur) throws SQLException {
        List<Contact> liste = new ArrayList<>();
        String sql = "SELECT * FROM contacts "
                + "WHERE id_utilisateur = ? AND nom_affiche = 'PENDING'";
        System.out.println("[DEBUG] Recherche demandes pour utilisateur=" + idUtilisateur);
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }
    public List<Contact> getContactsByUtilisateur(int idUtilisateur) throws SQLException {
        List<Contact> liste = new ArrayList<>();
        String sql = "SELECT * FROM contacts WHERE id_utilisateur = ?";
        try (Connection con = DataBase.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));

        }catch (SQLException e) {
            System.err.println("[ERROR] getContactsByUtilisateur: " + e.getMessage());
            throw e;
        }
        return liste;
    }
}