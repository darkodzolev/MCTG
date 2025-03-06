package service;

import model.Card;
import model.Deck;
import model.Stack;
import model.Package;
import model.User;
import org.json.JSONObject;
import server.Request;
import server.Response;
import http.HttpStatus;
import http.ContentType;
import org.json.JSONArray;
import repository.UserRepository;
import utils.Database;

import java.util.*;
import java.util.stream.Stream;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CardService
{
    private static final Map<String, Stack> userStacks = new HashMap<>();
    private static final Map<String, Deck> userDecks = new HashMap<>();
    private static final List<Package> packages = new ArrayList<>();
    private final UserRepository userRepository = new UserRepository();
    private Card card;
    private Package packageRepo;

    // Static block to preload sample packages (Fix for #4)
    /*static
    {
        refillPackages();
    }*/

    public Response createPackage(List<Card> cardList, String token) throws SQLException {
        // Validate admin token
        User adminUser = userRepository.findUserByToken(token);
        if (adminUser == null || !adminUser.getUsername().equals("admin")) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Admin access required\r\n");
        }

        UUID packageIdUUID = UUID.randomUUID();
        String packageId = packageIdUUID.toString();

        for (Card card : cardList) {
            card.setPackageId(packageId);
        }

        Package newPackage = new Package(cardList, packageId);


        String queryInsertPackage = "INSERT INTO packages (id) VALUES (?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryInsertPackage)) {
            stmt.setObject(1, packageId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON\r\n");
        }

        String queryInsertCards = "INSERT INTO cards (id, name, damage, packageId) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryInsertCards)) {
            stmt.setObject(1, cardList.getFirst().getId());
            stmt.setObject(2, cardList.getFirst().getName());
            stmt.setObject(3, cardList.getFirst().getDamage());
            stmt.setObject(4, cardList.getFirst().getPackageId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid JSON\r\n");
        }
        return new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "Package(s) created successfully\r\n");
    }

    public Response acquirePackages(String token)
    {
        System.out.println("AcquirePackages called with token: " + token);
        User user = userRepository.findUserByToken(token);
        if (user == null)
        {
            System.out.println("Token validation failed for token: " + token);
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid token\r\n");
        }
        System.out.println("User validated: " + user.getUsername());

        if (user.getCoins() < 5)
        {
            System.out.println("Not enough coins for user: " + user.getUsername() + ", coins: " + user.getCoins());
            return new Response(HttpStatus.FORBIDDEN, ContentType.PLAIN_TEXT, "Forbidden: Not enough coins\r\n");
        }

        List<Card> acquiredCards = new ArrayList<>();
        // Step 3: Fetch a package from the database
        try (Connection conn = Database.getConnection())
        {
            String fetchPackageQuery = "SELECT id, name, damage FROM packages LIMIT 1";
            String deletePackageQuery = "DELETE FROM packages WHERE id::text = ?";

            while (user.getCoins() >= 5)
            {
                try (PreparedStatement fetchStmt = conn.prepareStatement(fetchPackageQuery);
                     PreparedStatement deleteStmt = conn.prepareStatement(deletePackageQuery)) {
                    ResultSet rs = fetchStmt.executeQuery();

                    if (!rs.next()) {
                        System.out.println("No packages available.");
                        break;
                    }

                    Card card = new Card(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getDouble("damage"),
                            rs.getString("packageId")
                    );
                    acquiredCards.add(card);

                    Stack userStack = userStacks.get(token);
                    if (userStack == null)
                    {
                        userStack = new Stack();
                        userStacks.put(token, userStack);
                    }
                    userStack.addCard(card);

                    deleteStmt.setString(1, card.getId());
                    deleteStmt.executeUpdate();
                    System.out.println("Acquired card: " + card.getName());

                    int coinsToDeduct = 5;
                    System.out.println("AFTER: Current Coins for User: " + user.getCoins());
                    System.out.println("Coins deducted for user: " + user.getUsername() + " remaining coins: " + user.getCoins());
                    System.out.println("BEFORE: Current Coins for User: " + user.getCoins() + " Reduced Coins: " + coinsToDeduct);
                    user.setCoins(user.getCoins() - coinsToDeduct);
                    userRepository.updateUserCoins(user);
                }
            }

            if (acquiredCards.isEmpty()) {
                System.out.println("No packages available.");
                return new Response(HttpStatus.NOT_FOUND, ContentType.PLAIN_TEXT, "Not Found: No packages available\r\n");
            }

            JSONArray cardsJson = new JSONArray();
            for (Card card : acquiredCards)
            {
                cardsJson.put(card.toJson());
            }

            System.out.println("Cards acquired successfully: " + cardsJson.toString());
            return new Response(HttpStatus.CREATED, ContentType.JSON, cardsJson.toString() + "\r\n");
        } catch (Exception e)
        {
            e.printStackTrace();
            return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.PLAIN_TEXT, "Internal Server Error\r\n");
        }
    }

    // Show user cards (Fix for #8)
    public Response getUserCards(Request request)
    {
        String token = request.getHeaderMap().getHeader("Authorization");
        System.out.println("Raw Authorization Header: " + token);

        if (token != null && token.startsWith("Bearer "))
        {
            token = token.substring(7); // Strip "Bearer " prefix
            System.out.println("Extracted Token: " + token);
        }

        if (token == null || !isValidToken(token))
        {
            System.out.println("Token is invalid or missing");
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        // Get the user's stack of cards
        Stack userStack = userStacks.get(token);
        if (userStack == null)
        {
            System.out.println("No stack found for token: " + token);
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "User not found\r\n");
        }

        // Check if the stack is empty
        if (userStack.getCards().isEmpty())
        {
            System.out.println("Stack is empty for user with token: " + token);
            return new Response(HttpStatus.OK, ContentType.JSON, "[]");
        }

        // Convert cards to JSON using Gson
        String responseBody = new Gson().toJson(userStack.getCards());
        System.out.println("Returning user cards for token: " + token);

        return new Response(HttpStatus.OK, ContentType.JSON, responseBody);
    }

    // Show unconfigured deck (Fix for #10)
    public Response getDeck(Request request)
    {
        String token = request.getHeaderMap().getHeader("Authorization");
        if (token == null || token.isEmpty())
        {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        // Detect the query parameter "format"
        String format = request.getQueryParam("format"); // Assuming getQueryParam() parses query params
        Deck userDeck = userDecks.get(token);

        if (userDeck == null || userDeck.getCards().isEmpty())
        {
            // Return "empty" response based on the requested format
            if ("plain".equalsIgnoreCase(format))
            {
                return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Deck is empty\r\n");
            }
            return new Response(HttpStatus.OK, ContentType.JSON, "[]\r\n");
        }

        if ("plain".equalsIgnoreCase(format))
        {
            // Generate plain-text response if format is "plain"
            StringBuilder plainTextDeck = new StringBuilder();
            userDeck.getCards().forEach(card ->
            {
                plainTextDeck.append("Name: ").append(card.getName())
                        .append(", Damage: ").append(card.getDamage()).append("\r\n");
            });
            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, plainTextDeck.toString());
        }

        // Default case: Return JSON response
        JSONObject json = new JSONObject();
        userDeck.getCards().forEach(card ->
        {
            JSONObject cardJson = new JSONObject();
            cardJson.put("Id", card.getId());
            cardJson.put("Name", card.getName());
            cardJson.put("Damage", card.getDamage());
            json.append("deck", cardJson);
        });

        return new Response(HttpStatus.OK, ContentType.JSON, json.toString());
    }

    // Configure deck (Fix for #11)
    public Response configureDeck(Request request)
    {
        String token = request.getHeaderMap().getHeader("Authorization");
        System.out.println("Raw Authorization Header: " + token);

        if (token != null && token.startsWith("Bearer "))
        {
            token = token.substring(7); // Strip "Bearer " prefix
            System.out.println("Extracted Token: " + token);
        }

        if (token == null || !isValidToken(token))
        {
            System.out.println("Token is invalid or missing");
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        Stack userStack = userStacks.get(token);
        if (userStack == null)
        {
            System.out.println("User stack not found for token: " + token);
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "User not found\r\n");
        }

        try
        {
            // Parse request body for card IDs
            JSONArray cardIds = new JSONArray(request.getBody());
            System.out.println("Received card IDs: " + cardIds);

            // Ensure the deck contains exactly 4 cards
            if (cardIds.length() != 4)
            {
                System.out.println("Invalid deck size: " + cardIds.length());
                return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Deck must contain exactly 4 cards\r\n");
            }

            // Validate and build the deck
            Deck userDeck = new Deck();
            for (int i = 0; i < cardIds.length(); i++)
            {
                String cardId = cardIds.getString(i);
                Card card = userStack.getCardById(cardId); // Check if the card exists in the user's stack
                if (card == null)
                {
                    System.out.println("Card not found in user's stack: " + cardId);
                    return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Card not found: " + cardId + "\r\n");
                }
                userDeck.addCard(card);
            }

            // Save the configured deck
            userDecks.put(token, userDeck);
            System.out.println("Deck successfully configured for token: " + token);
            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Deck configured\r\n");
        } catch (Exception e)
        {
            System.out.println("Exception during deck configuration: " + e.getMessage());
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Invalid request body\r\n");
        }
    }

    // Get deck as plain-text representation (Fix for #13)
    public Response getDeckAsPlainText(Request request)
    {
        String token = request.getHeaderMap().getHeader("Authorization");
        if (token == null || !isValidToken(token))
        {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        Deck userDeck = userDecks.get(token);
        if (userDeck == null || userDeck.getCards().isEmpty())
        {
            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Deck is empty\r\n"); // Handle empty decks
        }

        StringBuilder plainTextDeck = new StringBuilder();
        userDeck.getCards().forEach(card ->
        {
            plainTextDeck.append("Name: ").append(card.getName())
                    .append(", Damage: ").append(card.getDamage()).append("\r\n");
        });

        return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, plainTextDeck.toString());
    }

    private boolean isValidToken(String token)
    {
        return userStacks.containsKey(token);
    }

    public static void addUserStack(String token, Stack stack)
    {
        System.out.println("Adding user stack for token: " + token);
        userStacks.put(token, stack);
    }

    public List<Card> getCardsFromPayload(String payload)
    {
        List<Card> cards = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(payload);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject cardObj = jsonArray.getJSONObject(i);

                String id = cardObj.getString("Id");
                String name = cardObj.getString("Name");
                double damage = cardObj.getDouble("Damage");

                cards.add(new Card(id, name, damage, packageRepo.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cards;
    }
}