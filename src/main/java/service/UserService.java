package service;

import model.User;
import org.json.JSONObject;
import server.Response;
import http.HttpStatus;
import http.ContentType;

import java.util.HashMap;
import java.util.Map;

public class UserService
{
    private static final Map<String, User> users = new HashMap<>(); // In-memory user storage

    public static Map<String, User> getUsers()
    {
        return users;
    }

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

            if (users.containsKey(username))
            {
                return new Response(HttpStatus.CONFLICT, ContentType.PLAIN_TEXT, "Conflict: User already exists\r\n");
            }

            User user = new User(username, password);
            users.put(username, user);

            return new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "User registered successfully\r\n");
        } catch (Exception e)
        {
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

            User user = users.get(username);
            if (user == null)
            {
                return new Response(HttpStatus.NOT_FOUND, ContentType.PLAIN_TEXT, "Not Found: User does not exist\r\n");
            }

            if (!user.getPassword().equals(password))
            {
                return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid username or password\r\n");
            }

            String token = username + "-mtcgToken";
            user.setToken(token);

            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Login successful. Token: " + token + "\r\n");
        } catch (Exception e)
        {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON\r\n");
        }
    }
}