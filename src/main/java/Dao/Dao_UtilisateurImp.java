package Dao;

import model.Utilisateur;

import java.sql.SQLException;
import java.util.List;

public class Dao_UtilisateurImp implements Dao_Utilisateur{
    @Override
    public int Add(Utilisateur utilisateur) throws SQLException {
        return 0;
    }

    @Override
    public int Modify(Utilisateur utilisateur) throws SQLException {
        return 0;
    }

    @Override
    public int Delete(Utilisateur utilisateur) throws SQLException {
        return 0;
    }

    @Override
    public Utilisateur getByID(Integer integer) throws SQLException {
        return null;
    }

    @Override
    public List<Utilisateur> getAll() throws SQLException {
        return List.of();
    }
}
