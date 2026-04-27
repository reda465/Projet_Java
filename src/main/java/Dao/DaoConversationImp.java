package Dao;

import model.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DaoConversationImp implements DAO_Conversation {

    // ── ADD ──────────────────────────────────────────────────────────────────
    // Retourne l'id généré — indispensable pour ajouter les participants ensuite
    @Override
    public int Add(Conversation conv) throws SQLException {
        String sql = "INSERT INTO conversations (type_conversation, nom_groupe, id_createur) "
                + "VALUES (?, ?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, conv.getTypeConversation());
            ps.setString(2, conv.getNomGroupe());
            if (conv.getIdCreateur() != null)
                ps.setInt(3, conv.getIdCreateur());
            else
                ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                conv.setIdConversation(keys.getInt(1));
                return keys.getInt(1);
            }
        }
        return -1;
    }

    // ── MODIFY ───────────────────────────────────────────────────────────────
    @Override
    public int Modify(Conversation conv) throws SQLException {
        String sql = "UPDATE conversations SET nom_groupe=?, date_dernier_message=? "
                + "WHERE id_conversation=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conv.getNomGroupe());
            if (conv.getDateDernierMessage() != null)
                ps.setTimestamp(2, Timestamp.valueOf(conv.getDateDernierMessage()));
            else
                ps.setNull(2, Types.TIMESTAMP);
            ps.setInt(3, conv.getIdConversation());
            return ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    @Override
    public int Delete(Conversation conv) throws SQLException {
        String sql = "DELETE FROM conversations WHERE id_conversation=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, conv.getIdConversation());
            return ps.executeUpdate();
        }
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @Override
    public Conversation getByID(Integer id) throws SQLException {
        String sql = "SELECT * FROM conversations WHERE id_conversation=?";
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
    public List<Conversation> getAll() throws SQLException {
        List<Conversation> liste = new ArrayList<>();
        String sql = "SELECT * FROM conversations ORDER BY date_dernier_message DESC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    // ── METHODES SPECIFIQUES ──────────────────────────────────────────────────

    /** Toutes les conversations d'un utilisateur
     *  — chargées juste après le LOGIN pour afficher la liste des chats */
    public List<Conversation> getByUtilisateur(int idUtilisateur) throws SQLException {
        List<Conversation> liste = new ArrayList<>();
        String sql = "SELECT c.* FROM conversations c "
                + "JOIN participants_conversation pc "
                + "ON c.id_conversation = pc.id_conversation "
                + "WHERE pc.id_utilisateur = ? "
                + "ORDER BY c.date_dernier_message DESC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapResultSet(rs));
        }
        return liste;
    }

    /** Trouve la conversation individuelle entre deux utilisateurs.
     *  Retourne null si elle n'existe pas encore → MessageRouter la créera */
    public Conversation findIndividuelle(int idUser1, int idUser2) throws SQLException {
        String sql = "SELECT c.* FROM conversations c "
                + "JOIN participants_conversation p1 "
                + "ON c.id_conversation = p1.id_conversation "
                + "JOIN participants_conversation p2 "
                + "ON c.id_conversation = p2.id_conversation "
                + "WHERE c.type_conversation = 'individuelle' "
                + "AND p1.id_utilisateur = ? "
                + "AND p2.id_utilisateur = ?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUser1);
            ps.setInt(2, idUser2);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    /** Ajoute un participant à une conversation.
     *  INSERT IGNORE → pas d'erreur si déjà présent */
    public void ajouterParticipant(int idConversation, int idUtilisateur) throws SQLException {
        String sql = "INSERT IGNORE INTO participants_conversation "
                + "(id_conversation, id_utilisateur) VALUES (?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idConversation);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
        }
    }

    /** Met à jour date_dernier_message après chaque message envoyé */
    public void updateDateDernierMessage(int idConversation) throws SQLException {
        String sql = "UPDATE conversations SET date_dernier_message = NOW() "
                + "WHERE id_conversation = ?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idConversation);
            ps.executeUpdate();
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Conversation mapResultSet(ResultSet rs) throws SQLException {
        Conversation conv = new Conversation();
        conv.setIdConversation(rs.getInt("id_conversation"));
        conv.setTypeConversation(rs.getString("type_conversation"));
        conv.setNomGroupe(rs.getString("nom_groupe"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null)
            conv.setDateCreation(dateCreation.toLocalDateTime());

        Timestamp dateDernierMessage = rs.getTimestamp("date_dernier_message");
        if (dateDernierMessage != null)
            conv.setDateDernierMessage(dateDernierMessage.toLocalDateTime());

        int idCreateur = rs.getInt("id_createur");
        if (!rs.wasNull())
            conv.setIdCreateur(idCreateur);

        return conv;
    }
}