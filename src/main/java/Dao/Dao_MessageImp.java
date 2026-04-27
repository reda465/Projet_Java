package Dao;

import model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dao_MessageImp implements DAO_Message {

    // ── ADD ──────────────────────────────────────────────────────────────────
    @Override
    public int Add(Message m) throws SQLException {
        String sql = "INSERT INTO messages (id_conversation, id_expediteur, type_message, contenu_texte, "
                + "url_fichier, nom_fichier, taille_fichier) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getIdConversation());
            ps.setInt(2, m.getIdExpediteur());
            ps.setString(3, m.getTypeMessage());
            ps.setString(4, m.getContenuTexte());
            ps.setString(5, m.getUrlFichier());
            ps.setString(6, m.getNomFichier());
            if (m.getTailleFichier() != null)
                ps.setLong(7, m.getTailleFichier());
            else
                ps.setNull(7, Types.BIGINT);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                m.setIdMessage(keys.getInt(1));
                return keys.getInt(1);
            }
        }
        return -1;
    }

    // ── MODIFY ───────────────────────────────────────────────────────────────
    @Override
    public int Modify(Message m) throws SQLException {
        String sql = "UPDATE messages SET contenu_texte=? WHERE id_message=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getContenuTexte());
            ps.setInt(2, m.getIdMessage());
            return ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    @Override
    public int Delete(Message m) throws SQLException {
        String sql = "DELETE FROM messages WHERE id_message=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, m.getIdMessage());
            return ps.executeUpdate();
        }
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @Override
    public Message getByID(Integer id) throws SQLException {
        String sql = "SELECT * FROM messages WHERE id_message=?";
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
    public List<Message> getAll() throws SQLException {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages ORDER BY date_envoi ASC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    // ── METHODES SPECIFIQUES ──────────────────────────────────────────────────

    /** Tous les messages d'une conversation — chargés au moment où on ouvre le chat */
    public List<Message> getByConversation(int idConversation) throws SQLException {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE id_conversation=? ORDER BY date_envoi ASC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idConversation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    /** Les N derniers messages d'une conversation */
    public List<Message> getDerniersMessages(int idConversation, int limite) throws SQLException {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE id_conversation=? "
                + "ORDER BY date_envoi DESC LIMIT ?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idConversation);
            ps.setInt(2, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Message mapResultSet(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setIdMessage(rs.getInt("id_message"));
        m.setIdConversation(rs.getInt("id_conversation"));
        m.setIdExpediteur(rs.getInt("id_expediteur"));
        m.setTypeMessage(rs.getString("type_message"));
        m.setContenuTexte(rs.getString("contenu_texte"));
        m.setUrlFichier(rs.getString("url_fichier"));
        m.setNomFichier(rs.getString("nom_fichier"));
        m.setTailleFichier(rs.getLong("taille_fichier"));

        Timestamp dateEnvoi = rs.getTimestamp("date_envoi");
        if (dateEnvoi != null)
            m.setDateEnvoi(dateEnvoi.toLocalDateTime());

        return m;
    }
}