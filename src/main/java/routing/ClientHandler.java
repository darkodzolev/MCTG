package routing;

import model.Card;
import server.Request;
import server.Response;
import server.Service;
import service.UserService;
import service.AuthService;
import service.CardService;

import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Service
{
    private final RouteHandler router;
    public CardService cardService;

    public ClientHandler()
    {
        this.router = new RouteHandler();
        this.cardService = new CardService();
        setupRoutes();
    }

    private void setupRoutes()
    {
        // Existing routes
        router.addRoute("GET", "/", request -> processWelcome());
        router.addRoute("POST", "/users", request -> new UserService().registerUser(request.getBody()));
        router.addRoute("POST", "/sessions", request -> new UserService().loginUser(request.getBody()));
        router.addRoute("POST", "/packages", request -> {
            String token = request.getHeaderMap().getHeader("Authorization").replace("Bearer ", "");
            List<Card> cardList = cardService.getCardsFromPayload(request.getBody());
            try {
                return cardService.createPackage(cardList, token);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        });
        router.addRoute("POST", "/transactions/packages", request -> {
            String token = request.getHeaderMap().getHeader("Authorization").replace("Bearer ", "");
            System.out.println("Token extracted in ClientHandler: " + token);
            try {
                return new CardService().acquirePackages(token);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        router.addRoute("GET", "/cards", request -> new CardService().getUserCards(request));
        router.addRoute("GET", "/deck", request -> new CardService().getDeck(request));
        router.addRoute("PUT", "/deck", request -> new CardService().configureDeck(request));
        router.addRoute("GET", "/deck?format=plain", request -> new CardService().getDeckAsPlainText(request));
    }

    @Override
    public Response handleRequest(Request request) {
        System.out.println("Incoming Request:");
        System.out.println("Method: " + request.getMethod());
        System.out.println("Path: " + request.getPathname());
        System.out.println("Query Params: " + request.getParams());
        System.out.println("Headers: ");
        request.getHeaderMap().print();
        System.out.println("Body: " + request.getBody());

        return router.route(request); // Proceed with routing
    }

    private Response processWelcome()
    {
        return new Response(http.HttpStatus.OK, http.ContentType.PLAIN_TEXT, "Welcome to the MTCG Server!\r\n");
    }
}