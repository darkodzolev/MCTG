package service;

import model.User;
import model.Stack;
import org.json.JSONObject;
import repository.UserRepository;
import server.Response;
import http.HttpStatus;
import http.ContentType;
import service.CardService;
import utils.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService
{
    private final UserRepository userRepository = new UserRepository(); // Database repository

    public Response registerUser(String body)
    {
        try
        {
            JSONObject json = new JSONObject(body);
            String username = json.getString("Username");
            String password = json.getString("Password");

            if (username == null || password == null)
            {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Missing username or password\r\n");
            }

            // Check if the user already exists in the database
            if (userRepository.findUserByUsername(username) != null)
            {
                return new Response(HttpStatus.CONFLICT, ContentType.PLAIN_TEXT, "Conflict: User already exists\r\n");
            }

            // Register the user in the database
            User user = new User(username, password);
            userRepository.registerUser(user);

            return new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "User registered successfully\r\n");
        } catch (Exception e)
        {
            e.printStackTrace();
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON\r\n");
        }
    }

    public Response loginUser(String body)
    {
        try
        {
            JSONObject json = new JSONObject(body);
            String username = json.getString("Username");
            String password = json.getString("Password");

            if (username == null || password == null)
            {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Missing username or password\r\n");
            }

            // Fetch the user from the database
            User user = userRepository.findUserByUsername(username);
            if (user == null)
            {
                return new Response(HttpStatus.NOT_FOUND, ContentType.PLAIN_TEXT, "Not Found: User does not exist\r\n");
            }

            if (!user.getPassword().equals(password))
            {
                return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid username or password\r\n");
            }

            // Generate and set a token for the user
            String token = username + "-mtcgToken";
            user.setToken(token);

            String updateTokenQuery = "UPDATE users SET token = ? WHERE username = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateTokenQuery)) {
                stmt.setString(1, token);
                stmt.setString(2, username);
                stmt.executeUpdate();
            }

            // You can also save/update the token in the database if needed
            CardService.addUserStack(token, new Stack());

            System.out.println("Login Successful. Token Generated: " + token);
            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Login successful. Token: " + token + "\r\n");
        } catch (Exception e)
        {
            e.printStackTrace();
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON\r\n");
        }
    }
}