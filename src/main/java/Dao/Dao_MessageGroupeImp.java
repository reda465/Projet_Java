package Dao;

import model.MessageGroupe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Dao_MessageGroupeImp {

    public Dao_MessageGroupeImp() {
        initialiserTable();
    }

    private void initialiserTable() {
        String sql = "CREATE TABLE IF NOT EXISTS messages_groupes (" +
                "id_message INT AUTO_INCREMENT PRIMARY KEY," +
                "id_groupe INT NOT NULL," +
                "telephone_expediteur VARCHAR(30) NOT NULL," +
                "nom_expediteur VARCHAR(120) NOT NULL," +
                "contenu TEXT," +
                "date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Connection c = DataBase.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int ajouter(MessageGroupe m) throws Exception {
        String sql = "INSERT INTO messages_groupes (id_groupe, telephone_expediteur, nom_expediteur, contenu) VALUES (?, ?, ?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getIdGroupe());
            ps.setString(2, m.getTelephoneExpediteur());
            ps.setString(3, m.getNomExpediteur());
            ps.setString(4, m.getContenu());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        return -1;
    }

    public MessageGroupe getById(int idMessage) throws Exception {
        String sql = "SELECT * FROM messages_groupes WHERE id_message=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMessage);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        }
    }

    private MessageGroupe mapRow(ResultSet rs) throws Exception {
        MessageGroupe m = new MessageGroupe();
        m.setIdMessage(rs.getInt("id_message"));
        m.setIdGroupe(rs.getInt("id_groupe"));
        m.setTelephoneExpediteur(rs.getString("telephone_expediteur"));
        m.setNomExpediteur(rs.getString("nom_expediteur"));
        m.setContenu(rs.getString("contenu"));
        Timestamp t = rs.getTimestamp("date_envoi");
        m.setDateEnvoi(t != null ? t.toLocalDateTime() : null);
        return m;
    }

    public List<MessageGroupe> getByGroupe(int idGroupe) throws Exception {
        List<MessageGroupe> list = new ArrayList<>();
        String sql = "SELECT * FROM messages_groupes WHERE id_groupe=? ORDER BY date_envoi ASC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }
}
