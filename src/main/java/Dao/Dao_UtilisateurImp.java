package Dao;

import model.Utilisateur;
//import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dao_UtilisateurImp implements Dao_Utilisateur {

    // ── ADD ─────────────────────────────────────────────────────────────────
    @Override
    public int Add(Utilisateur u) throws SQLException {
        String sql = "INSERT INTO utilisateurs (nom_complet, numero_telephone, mot_de_passe) "
                + "VALUES (?, ?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNomComplet());
            ps.setString(2, u.getNumeroTelephone());

            return ps.executeUpdate();
        }
    }

    // ── MODIFY ──────────────────────────────────────────────────────────────
    @Override
    public int Modify(Utilisateur u) throws SQLException {
        String sql = "UPDATE utilisateurs SET nom_complet=?, photo_profil=? "
                + "WHERE id_utilisateur=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNomComplet());
            ps.setString(2, u.getPhotoProfil());
            ps.setInt(3, u.getIdUtilisateur());
            return ps.executeUpdate();
        }
    }

    // ── DELETE ──────────────────────────────────────────────────────────────
    @Override
    public int Delete(Utilisateur u) throws SQLException {
        String sql = "DELETE FROM utilisateurs WHERE id_utilisateur=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, u.getIdUtilisateur());
            return ps.executeUpdate();
        }
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────
    @Override
    public Utilisateur getByID(Integer id) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE id_utilisateur=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    // ── GET ALL ──────────────────────────────────────────────────────────────
    @Override
    public List<Utilisateur> getAll() throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }
    /** Retrouve un utilisateur par son numéro — sans vérifier le mot de passe */
    public Utilisateur findByTelephone(String tel) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE numero_telephone=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tel);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }
    // ── METHODES SPECIFIQUES ─────────────────────────────────────────────────

    /** Authentification : vérifie le téléphone + BCrypt. Retourne null si échec. */
    public Utilisateur findByTelAndPassword(String tel, String password) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE numero_telephone=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tel);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("mot_de_passe");

                return mapResultSet(rs);
            }
        }
        return null;
    }

    /** Vérifie si un numéro existe déjà (pour l'inscription). */
    public boolean telephoneExiste(String tel) throws SQLException {
        String sql = "SELECT id_utilisateur FROM utilisateurs WHERE numero_telephone=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tel);
            return ps.executeQuery().next();
        }
    }

    /** Met à jour derniere_connexion à l'instant présent. */
    public void updateDerniereConnexion(int idUtilisateur) throws SQLException {
        String sql = "UPDATE utilisateurs SET derniere_connexion=NOW() "
                + "WHERE id_utilisateur=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
        }
    }

    // ── MAPPING ──────────────────────────────────────────────────────────────
    private Utilisateur mapResultSet(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setIdUtilisateur(rs.getInt("id_utilisateur"));
        u.setNomComplet(rs.getString("nom_complet"));
        u.setNumeroTelephone(rs.getString("numero_telephone"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setPhotoProfil(rs.getString("photo_profil"));
        //u.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
        //u.setDerniereConnexion(rs.getTimestamp("derniere_connexion").toLocalDateTime());

        Timestamp dateInscription = rs.getTimestamp("date_inscription");
        if (dateInscription != null)
            u.setDateInscription(dateInscription.toLocalDateTime());

        Timestamp derniereConnexion = rs.getTimestamp("derniere_connexion");
        if (derniereConnexion != null)
            u.setDerniereConnexion(derniereConnexion.toLocalDateTime());

        return u;
    }
}