package Dao;

import model.Conversation;

import java.sql.SQLException;
import java.util.List;

public class DaoConversationImp implements DAO_Conversation{
    @Override
    public int Add(Conversation conversation) throws SQLException {
        return 0;
    }

    @Override
    public int Modify(Conversation conversation) throws SQLException {
        return 0;
    }

    @Override
    public int Delete(Conversation conversation) throws SQLException {
        return 0;
    }

    @Override
    public Conversation getByID(Integer integer) throws SQLException {
        return null;
    }

    @Override
    public List<Conversation> getAll() throws SQLException {
        return List.of();
    }
}
