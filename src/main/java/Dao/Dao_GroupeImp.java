package Dao;

import model.Groupe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Dao_GroupeImp {

    public Dao_GroupeImp() {
        initialiserTables();
    }

    private void initialiserTables() {
        String sqlGroupes = "CREATE TABLE IF NOT EXISTS groupes (" +
                "id_groupe INT AUTO_INCREMENT PRIMARY KEY," +
                "nom_groupe VARCHAR(120) NOT NULL," +
                "numero_createur VARCHAR(30) NOT NULL," +
                "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        String sqlMembres = "CREATE TABLE IF NOT EXISTS groupes_membres (" +
                "id_groupe INT NOT NULL," +
                "numero_membre VARCHAR(30) NOT NULL," +
                "PRIMARY KEY (id_groupe, numero_membre)" +
                ")";
        try (Connection c = DataBase.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sqlGroupes);
            st.execute(sqlMembres);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Groupe creerGroupe(String nom, String numCreateur, List<String> membres) throws Exception {
        String sql = "INSERT INTO groupes (nom_groupe, numero_createur) VALUES (?, ?)";
        Groupe g = new Groupe();
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.setString(2, numCreateur);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) g.setIdGroupe(keys.getInt(1));
        }
        g.setNomGroupe(nom);
        g.setNumeroCreateur(numCreateur);
        g.setDateCreation(java.time.LocalDateTime.now().toString());

        List<String> nums = new ArrayList<>();
        nums.add(numCreateur);
        if (membres != null) {
            for (String m : membres) {
                if (m != null && !m.isBlank() && !nums.contains(m)) nums.add(m.trim());
            }
        }
        g.setNumerosMembres(nums);
        for (String n : nums) ajouterMembre(g.getIdGroupe(), n);
        return g;
    }

    public List<Groupe> getGroupesPourMembre(String numero) throws Exception {
        List<Groupe> list = new ArrayList<>();
        String sql = "SELECT g.id_groupe, g.nom_groupe, g.numero_createur, g.date_creation " +
                "FROM groupes g JOIN groupes_membres gm ON gm.id_groupe = g.id_groupe " +
                "WHERE gm.numero_membre = ? ORDER BY g.id_groupe DESC";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, numero);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Groupe g = new Groupe();
                g.setIdGroupe(rs.getInt("id_groupe"));
                g.setNomGroupe(rs.getString("nom_groupe"));
                g.setNumeroCreateur(rs.getString("numero_createur"));
                Timestamp t = rs.getTimestamp("date_creation");
                g.setDateCreation(t != null ? t.toLocalDateTime().toString() : "");
                g.setNumerosMembres(getMembres(g.getIdGroupe()));
                list.add(g);
            }
        }
        return list;
    }

    public List<String> getMembres(int idGroupe) throws Exception {
        List<String> membres = new ArrayList<>();
        String sql = "SELECT numero_membre FROM groupes_membres WHERE id_groupe=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) membres.add(rs.getString(1));
        }
        return membres;
    }

    public Groupe getById(int idGroupe) throws Exception {
        String sql = "SELECT * FROM groupes WHERE id_groupe=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Groupe g = new Groupe();
            g.setIdGroupe(idGroupe);
            g.setNomGroupe(rs.getString("nom_groupe"));
            g.setNumeroCreateur(rs.getString("numero_createur"));
            Timestamp t = rs.getTimestamp("date_creation");
            g.setDateCreation(t != null ? t.toLocalDateTime().toString() : "");
            g.setNumerosMembres(getMembres(idGroupe));
            return g;
        }
    }

    public boolean estMembre(int idGroupe, String numero) throws Exception {
        String sql = "SELECT 1 FROM groupes_membres WHERE id_groupe=? AND numero_membre=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ps.setString(2, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean estAdmin(int idGroupe, String numero) throws Exception {
        String sql = "SELECT 1 FROM groupes WHERE id_groupe=? AND numero_createur=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ps.setString(2, numero);
            return ps.executeQuery().next();
        }
    }

    public void ajouterMembre(int idGroupe, String numero) throws Exception {
        String sql = "INSERT IGNORE INTO groupes_membres (id_groupe, numero_membre) VALUES (?, ?)";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ps.setString(2, numero);
            ps.executeUpdate();
        }
    }

    public void retirerMembre(int idGroupe, String numero) throws Exception {
        String sql = "DELETE FROM groupes_membres WHERE id_groupe=? AND numero_membre=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGroupe);
            ps.setString(2, numero);
            ps.executeUpdate();
        }
    }

    public void supprimerGroupe(int idGroupe) throws Exception {
        try (Connection c = DataBase.getConnection()) {
            try (PreparedStatement ps1 = c.prepareStatement("DELETE FROM messages_groupes WHERE id_groupe=?")) {
                ps1.setInt(1, idGroupe);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = c.prepareStatement("DELETE FROM groupes_membres WHERE id_groupe=?")) {
                ps2.setInt(1, idGroupe);
                ps2.executeUpdate();
            }
            try (PreparedStatement ps3 = c.prepareStatement("DELETE FROM groupes WHERE id_groupe=?")) {
                ps3.setInt(1, idGroupe);
                ps3.executeUpdate();
            }
        }
    }

    public void renommerGroupe(int idGroupe, String nouveauNom) throws Exception {
        String sql = "UPDATE groupes SET nom_groupe=? WHERE id_groupe=?";
        try (Connection c = DataBase.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nouveauNom);
            ps.setInt(2, idGroupe);
            ps.executeUpdate();
        }
    }
}
