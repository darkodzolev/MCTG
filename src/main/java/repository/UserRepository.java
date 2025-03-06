package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.User;
import utils.Database;

public class UserRepository
{
    public User findUserByUsername(String username)
    {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                // Map the result set to a User object
                return new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("token"),
                        rs.getInt("coins"),
                        rs.getInt("id")
                );
            }

        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        return null; // Return null if user not found
    }

    public User findUserByToken(String token)
    {
        String query = "SELECT * FROM users WHERE token = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                return new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("token"),
                        rs.getInt("coins"),
                        rs.getInt("id")
                );
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Integer findUserIdByUsername(String username) {
        String query = "SELECT id FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void updateUserCoins(User user, int userCoins)
    {
        int newCoins = userCoins - 5;
        String query = "UPDATE users SET coins = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            System.out.println("Attempting to update coins for user: " + user.getUsername() + " to: " + user.getCoins());
            stmt.setInt(1, newCoins);
            stmt.setString(2, user.getUsername());
            int rowsAffected = stmt.executeUpdate();

            System.out.println("Coins updated in DB for user: " + user.getUsername() + ". Rows affected: " + rowsAffected);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void registerUser(User user) throws SQLException
    {
        String query = "INSERT INTO users (username, password, coins) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setInt(3, user.getCoins());
            stmt.executeUpdate();
        }
    }
}