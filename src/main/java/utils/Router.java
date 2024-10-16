package utils;

import server.Service;

import java.util.HashMap;
import java.util.Map;

public class Router
{
    private Map<String, Service> serviceRegistry = new HashMap<>();

    // Route hinzufügen, z.B. "POST /users"
    public void addService(String route, Service service)
    {
        this.serviceRegistry.put(route, service);
    }

    // Route entfernen
    public void removeService(String route)
    {
        this.serviceRegistry.remove(route);
    }

    // Methode und Pfad kombinieren, um die richtige Route zu finden
    public Service resolve(String method, String path)
    {
        String route = method + " " + path;  // Kombiniere Methode und Pfad
        return this.serviceRegistry.get(route);
    }
}