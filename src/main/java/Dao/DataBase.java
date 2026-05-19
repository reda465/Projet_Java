package Dao;

import java.sql.*;

public class DataBase {
    private static String Url="jdbc:mysql://localhost:3306/projet_java";
    private static String Uname="root";
    private static String pass="";
    public static Connection getConnection(){
        try{
            Connection Con=DriverManager.getConnection(Url,Uname,pass);
            return Con;
        }
        catch(Exception e){
            e.getMessage();
        }
        return null;
    }
}