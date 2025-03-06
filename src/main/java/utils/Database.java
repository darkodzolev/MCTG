package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database
{
    private static final String URL = "jdbc:postgresql://localhost:5432/mctg";
    private static final String DATABASE_NAME = "mctg";
    private static final String USER = "darkodjolev";
    private static final String PASSWORD = "darko123";

    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void createDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/", USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Create the database if it doesn't exist
            String createDatabaseSQL = "CREATE DATABASE " + DATABASE_NAME;
            stmt.executeUpdate(createDatabaseSQL);
            System.out.println("Database created successfully: " + DATABASE_NAME);
        } catch (SQLException e) {
            // If the database already exists, catch the exception and continue
            if (e.getMessage().contains("already exists")) {
                System.out.println("Database already exists: " + DATABASE_NAME);
            } else {
                System.out.println("Error during database creation: " + e.getMessage());
            }
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String dropTablesSQL = "DROP TABLE IF EXISTS users, packages, cards, decks CASCADE";
            stmt.executeUpdate(dropTablesSQL);
            System.out.println("Dropped existing tables if they existed.");

            String createUsersTableSQL = "CREATE TABLE users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "token VARCHAR(255), " +
                    "coins INT)";
            stmt.executeUpdate(createUsersTableSQL);
            System.out.println("Created users table.");

            String createPackagesTableSQL = "CREATE TABLE packages (" +
                    "id UUID PRIMARY KEY, " +
                    "user_id INT REFERENCES users(id)" +
                    ");";
            stmt.executeUpdate(createPackagesTableSQL);
            System.out.println("Created packages table.");

            String createCardsTableSQL = "CREATE TABLE cards (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "name VARCHAR(50) NOT NULL, " +
                    "damage INT NOT NULL, " +
                    "package_id VARCHAR(300) NOT NULL " + ");";
            stmt.executeUpdate(createCardsTableSQL);
            System.out.println("Created cards table.");

        } catch (SQLException e) {
            System.out.println("Error during database initialization: " + e.getMessage());
        }
    }
}

