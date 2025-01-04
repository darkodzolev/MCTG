package routing;

import server.Request;
import server.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RouteHandler
{
    private final Map<String, Map<String, Function<Request, Response>>> routes = new HashMap<>();

    public void addRoute(String method, String path, Function<Request, Response> handler)
    {
        routes.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public Response route(Request request)
    {
        Map<String, Function<Request, Response>> methodRoutes = routes.get(request.getMethod().name());
        if (methodRoutes != null)
        {
            Function<Request, Response> handler = methodRoutes.get(request.getPathname());
            if (handler != null)
            {
                return handler.apply(request); // Call the handler
            }
        }
        // No matching route
        return new Response(http.HttpStatus.NOT_FOUND, http.ContentType.PLAIN_TEXT, "Not Found\r\n");
    }
}