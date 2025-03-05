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

        System.out.println("Incoming Request - Method: " + request.getMethod() + ", Path: " + request.getPathname());

        Map<String, Function<Request, Response>> methodRoutes = routes.get(request.getMethod().name());
        if (methodRoutes != null)
        {
            // Try to find an exact match
            Function<Request, Response> handler = methodRoutes.get(request.getPathname());

            if (handler == null && request.getParams() != null)
            {
                // Try matching routes ignoring query params
                String basePath = request.getPathname().split("\\?")[0]; // Get the path without query parameters
                handler = methodRoutes.get(basePath);
            }

            if (handler != null)
            {
                System.out.println("Matched Handler for Path: " + request.getPathname());
                return handler.apply(request); // Call the handler
            }
        }
        // No matching route found
        System.out.println("No Handler Found for Path: " + request.getPathname());
        return new Response(http.HttpStatus.NOT_FOUND, http.ContentType.PLAIN_TEXT, "Not Found\r\n");
    }
}