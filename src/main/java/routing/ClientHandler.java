package routing;

import server.Request;
import server.Response;
import server.Service;
import service.UserService;
import service.AuthService;

public class ClientHandler implements Service
{
    private final RouteHandler router;

    public ClientHandler()
    {
        this.router = new RouteHandler();
        setupRoutes();
    }

    private void setupRoutes()
    {
        router.addRoute("GET", "/", request -> processWelcome());
        router.addRoute("POST", "/users", request -> new UserService().registerUser(request.getBody()));
        router.addRoute("POST", "/sessions", request -> new UserService().loginUser(request.getBody()));
        router.addRoute("GET", "/user", request -> {
            String token = request.getHeaderMap().getHeader("Authorization");
            return new AuthService(UserService.getUsers()).authenticateUser(token);
        });
    }

    @Override
    public Response handleRequest(Request request)
    {
        return router.route(request);
    }

    private Response processWelcome()
    {
        return new Response(http.HttpStatus.OK, http.ContentType.PLAIN_TEXT, "Welcome to the MTCG Server!\r\n");
    }
}