package service;

import model.User;
import server.Response;
import http.HttpStatus;
import http.ContentType;

import java.util.Map;

public class AuthService
{
    private final Map<String, User> users;

    public AuthService(Map<String, User> users)
    {
        this.users = users;
    }

    public Response authenticateUser(String token)
    {
        if (token == null || token.isEmpty())
        {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: No token provided\r\n");
        }

        for (User user : users.values())
        {
            if (token.equals(user.getToken()))
            {
                return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "User authenticated. Welcome, " + user.getUsername() + "!\r\n");
            }
        }

        return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid token\r\n");
    }
}