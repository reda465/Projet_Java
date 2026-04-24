package Dao;

import model.Message;

import java.sql.SQLException;
import java.util.List;

public class Dao_MessageImp implements DAO_Message{
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
