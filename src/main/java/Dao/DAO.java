package Dao;

import java.sql.SQLException;
import java.util.List;

public interface DAO<T,Id> {
    public int Add(T t) throws SQLException;
    public int Modify(T t) throws SQLException;
    public int Delete(T t) throws SQLException;
    public T getByID(Id id) throws SQLException;
    public List<T> getAll() throws SQLException;
}