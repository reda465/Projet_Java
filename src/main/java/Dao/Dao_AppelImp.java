package Dao;

import model.Appel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Dao_AppelImp implements DAO_Appel{
    @Override
    public int Add(Appel appel) throws SQLException {
        Connection con= DataBase.getConnection();
        String Sql="insert into appels Values(?,?,?,?,?,?,?)";
        PreparedStatement st =con.prepareStatement(Sql);
        st.setInt(1, appel.getIdAppel());
        st.setInt(2, appel.getIdAppelant());
        st.setInt(3, appel.getIdConversation());
        //st.setString(4, (appel.getType_appel()).name());

        return 0;
    }

    @Override
    public int Modify(Appel appel) throws SQLException {
        return 0;
    }

    @Override
    public int Delete(Appel appel) throws SQLException {
        return 0;
    }

    @Override
    public Appel getByID(Integer integer) throws SQLException {
        return null;
    }

    @Override
    public List<Appel> getAll() throws SQLException {
        return List.of();
    }
}
