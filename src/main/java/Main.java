import server.Server;
import utils.Router;
import routing.ClientHandler;

public class Main
{
    public static void main(String[] args)
    {
        Router router = configureRouter();  // Router konfigurieren
        Server server = new Server(10001, router);  // Server auf Port 10001 starten

        try
        {
            server.start();  // Server starten
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static Router configureRouter()
    {
        Router router = new Router();
        ClientHandler clientHandler = new ClientHandler();

        router.addService("POST /users", clientHandler);
        router.addService("POST /sessions", clientHandler);
        router.addService("POST /packages", clientHandler);
        router.addService("POST /transactions/packages", clientHandler);
        router.addService("GET /cards", clientHandler);
        router.addService("GET /deck", clientHandler);
        router.addService("PUT /deck", clientHandler);
        router.addService("GET /deck?format=plain", clientHandler);

        return router;
    }
}