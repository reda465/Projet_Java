package Dao;

import model.Message;
import model.MessageFileAttente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dao_MessageFileAttenteImp implements DAO_Message {

    /** Ajoute un message en file d'attente pour un destinataire hors ligne */
    public int ajouterEnAttente(int idMessage, int idDestinataire) throws SQLException {
        String sql = "INSERT INTO messages_file_attente (id_message, id_destinataire) "
                + "VALUES (?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMessage);
            ps.setInt(2, idDestinataire);
            return ps.executeUpdate();
        }
    }

    /** Récupère tous les messages en attente pour un utilisateur
     *  — appelé juste après le LOGIN réussi */
    public List<MessageFileAttente> getMessagesEnAttente(int idDestinataire) throws SQLException {
        List<MessageFileAttente> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages_file_attente "
                + "WHERE id_destinataire=? AND est_delivre=0 "
                + "ORDER BY date_mise_file ASC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idDestinataire);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MessageFileAttente mfa = new MessageFileAttente();
                mfa.setIdFile(rs.getInt("id_file"));
                mfa.setIdMessage(rs.getInt("id_message"));
                mfa.setIdDestinataire(rs.getInt("id_destinataire"));
                mfa.setEstDelivre(rs.getBoolean("est_delivre"));
                Timestamp t = rs.getTimestamp("date_mise_file");
                if (t != null) mfa.setDateMiseFile(t.toLocalDateTime());
                liste.add(mfa);
            }
        }
        return liste;
    }

    /** Marque les messages comme délivrés après les avoir envoyés au client */
    public void marquerDelivres(int idDestinataire) throws SQLException {
        String sql = "UPDATE messages_file_attente SET est_delivre=1 "
                + "WHERE id_destinataire=? AND est_delivre=0";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idDestinataire);
            ps.executeUpdate();
        }
    }

    @Override
    public int Add(Message message) throws SQLException {
        return 0;
    }

    @Override
    public int Modify(Message message) throws SQLException {
        return 0;
    }

    @Override
    public int Delete(Message message) throws SQLException {
        return 0;
    }

    @Override
    public Message getByID(Integer integer) throws SQLException {
        return null;
    }

    @Override
    public List<Message> getAll() throws SQLException {
        return List.of();
    }
}