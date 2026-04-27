package Dao;

import model.Appel;
import model.enums.StatutAppel;
import model.enums.TypeAppel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dao_AppelImp implements DAO_Appel {

    // ── ADD ──────────────────────────────────────────────────────────────────
    // Retourne l'id généré — nécessaire pour mettre à jour l'appel après
    @Override
    public int Add(Appel a) throws SQLException {
        String sql = "INSERT INTO appels (id_appelant, id_conversation, type_appel, statut) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getIdAppelant());
            ps.setInt(2, a.getIdConversation());
            ps.setString(3, a.getTypeAppel().name());
            ps.setString(4, a.getStatut().name());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                a.setIdAppel(keys.getInt(1));
                return keys.getInt(1);
            }
        }
        return -1;
    }

    // ── MODIFY ───────────────────────────────────────────────────────────────
    @Override
    public int Modify(Appel a) throws SQLException {
        String sql = "UPDATE appels SET statut=?, duree_secondes=? WHERE id_appel=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getStatut().name());
            ps.setInt(2, a.getDureeSecondes());
            ps.setInt(3, a.getIdAppel());
            return ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    @Override
    public int Delete(Appel a) throws SQLException {
        String sql = "DELETE FROM appels WHERE id_appel=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a.getIdAppel());
            return ps.executeUpdate();
        }
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @Override
    public Appel getByID(Integer id) throws SQLException {
        String sql = "SELECT * FROM appels WHERE id_appel=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────
    @Override
    public List<Appel> getAll() throws SQLException {
        List<Appel> liste = new ArrayList<>();
        String sql = "SELECT * FROM appels ORDER BY date_appel DESC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    // ── METHODES SPECIFIQUES ──────────────────────────────────────────────────

    /** Appelé quand l'appelé accepte ou refuse */
    public void updateStatut(int idAppel, StatutAppel statut) throws SQLException {
        String sql = "UPDATE appels SET statut=? WHERE id_appel=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, idAppel);
            ps.executeUpdate();
        }
    }

    /** Appelé quand l'appel se termine — met à jour statut + durée */
    public void terminerAppel(int idAppel, StatutAppel statut,
                              int dureeSecondes) throws SQLException {
        String sql = "UPDATE appels SET statut=?, duree_secondes=? WHERE id_appel=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, dureeSecondes);
            ps.setInt(3, idAppel);
            ps.executeUpdate();
        }
    }

    /** Historique des appels d'une conversation */
    public List<Appel> getByConversation(int idConversation) throws SQLException {
        List<Appel> liste = new ArrayList<>();
        String sql = "SELECT * FROM appels WHERE id_conversation=? "
                + "ORDER BY date_appel DESC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idConversation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Appel mapResultSet(ResultSet rs) throws SQLException {
        Appel a = new Appel();
        a.setIdAppel(rs.getInt("id_appel"));
        a.setIdAppelant(rs.getInt("id_appelant"));
        a.setIdConversation(rs.getInt("id_conversation"));
        a.setTypeAppel(TypeAppel.valueOf(rs.getString("type_appel")));
        a.setStatut(StatutAppel.valueOf(rs.getString("statut")));
        a.setDureeSecondes(rs.getInt("duree_secondes"));

        Timestamp dateAppel = rs.getTimestamp("date_appel");
        if (dateAppel != null)
            a.setDateAppel(dateAppel.toLocalDateTime());

        return a;
    }
}