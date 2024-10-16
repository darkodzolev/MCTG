package routing;

import model.User;
import org.json.JSONObject;
import server.Request;
import server.Response;
import server.Service;
import http.HttpStatus;
import http.ContentType;

import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Service
{
    private static final Map<String, User> users = new HashMap<>();  // In-memory user storage

    public ClientHandler() {}

    @Override
    public Response handleRequest(Request request)
    {
        String method = request.getMethod().name();  // Verwende .name(), um den String der Methode zu bekommen
        String path = request.getPathname();  // Verwende die vorhandene getPath()-Methode
        System.out.println("Request received: " + request.getMethod().name() + " " + request.getPathname());

        // Routing basierend auf Methode und Pfad
        if ("GET".equals(method) && "/".equals(path))
        {
            return processWelcome();
        }
        else if ("POST".equals(method) && "/users".equals(path))
        {
            return processRegister(request.getBody());
        }
        else if ("POST".equals(method) && "/sessions".equals(path))
        {
            return processLogin(request.getBody());
        }
        else if ("GET".equals(method) && "/user".equals(path))
        {
            String token = request.getHeaderMap().getHeader("Authorization");
            return processUser(token);
        }
        else
        {
            return new Response(HttpStatus.NOT_FOUND, ContentType.PLAIN_TEXT, "Not Found"+"\r\n");
        }
    }

    private Response processWelcome()
    {
        return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Welcome to the MTCG Server!"+"\r\n");
    }

    public Response processRegister(String body)
    {
        try
        {
            JSONObject json = new JSONObject(body);
            String username = json.getString("Username");
            String password = json.getString("Password");

            if (username == null || password == null)
            {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Missing username or password"+"\r\n");
            }

            if (users.containsKey(username))
            {
                return new Response(HttpStatus.CONFLICT, ContentType.PLAIN_TEXT, "Conflict: User already exists"+"\r\n");
            }

            User user = new User(username, password);
            users.put(username, user);
            return new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "User registered successfully"+"\r\n");
        }
        catch (Exception e)
        {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON"+"\r\n");
        }
    }

    private Response processLogin(String body)
    {
        try
        {
            JSONObject json = new JSONObject(body);
            String username = json.getString("Username");
            String password = json.getString("Password");

            if (username == null || password == null)
            {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Missing username or password"+"\r\n");
            }

            // Check if the user exists
            User user = users.get(username);
            if (user == null)
            {
                return new Response(HttpStatus.NOT_FOUND, ContentType.PLAIN_TEXT, "Not Found: User does not exist"+"\r\n");
            }

            // Verify password
            if (!user.getPassword().equals(password))
            {
                return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid username or password"+"\r\n");
            }

            // Generate a token and store it in the user object
            String token = username+"-mtcgToken";
            user.setToken(token);

            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "OK: Login successful. Token: " + token+"\r\n");
        }
        catch (Exception e)
        {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON"+"\r\n");
        }
    }

    private Response processUser(String token)
    {
        if (token == null || token.isEmpty())
        {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: No token provided"+"\r\n");
        }

        for (User user : users.values())
        {
            if (token.equals(user.getToken()))
            {
                return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "User data can be accessed"+"\r\n");
            }
        }

        return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid token"+"\r\n");
    }
}