package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database
{
    private static final String URL = "jdbc:postgresql://localhost:5432/mctg";
    private static final String USER = "darkodjolev";
    private static final String PASSWORD = "darko123";

    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}