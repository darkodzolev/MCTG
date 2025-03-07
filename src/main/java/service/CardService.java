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
import org.json.JSONException;


public class CardService
{
    private static final Map<String, Stack> userStacks = new HashMap<>();
    private static final Map<String, Deck> userDecks = new HashMap<>();
    private static final List<Package> packages = new ArrayList<>();
    private final UserRepository userRepository = new UserRepository();
    private Card card;
    private Package packageRepo;

    public Response createPackage(List<Card> cardList, String token) throws SQLException {
        // Validate admin token
        User adminUser = userRepository.findUserByToken(token);
        if (adminUser == null || !adminUser.getUsername().equals("admin")) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Admin access required\r\n");
        }

        UUID packageIdUUID = UUID.randomUUID();


        for (Card card : cardList) {
            card.setPackageId(packageIdUUID);
        }

        Package newPackage = new Package(cardList, packageIdUUID);


        String queryInsertPackage = "INSERT INTO packages (id) VALUES (?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryInsertPackage)) {
            stmt.setObject(1, packageIdUUID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid SQL\r\n");
        }

        String queryInsertCards = "INSERT INTO cards (id, name, damage, package_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryInsertCards)) {

            for (Card card : cardList) {
                stmt.setObject(1, card.getId());
                stmt.setObject(2, card.getName());
                stmt.setObject(3, card.getDamage());
                stmt.setObject(4, card.getPackageId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Bad Request: Invalid SQL\r\n");
        }
        return new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "Package(s) created successfully\r\n");
    }

    public Response acquirePackages(String token) throws SQLException {
        System.out.println("AcquirePackages called with token: " + token);
        User user = userRepository.findUserByToken(token);

        if (user == null) {
            System.out.println("Token validation failed for token: " + token);
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized: Invalid token\r\n");
        }
        System.out.println("User validated: " + user.getUsername());

        int userCoins = 0;

        String getUserCoinsByUsername = "SELECT coins FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getUserCoinsByUsername)) {
            stmt.setString(1, user.getUsername());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    userCoins = rs.getInt("coins");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (userCoins < 5) {
            System.out.println("Not enough coins for user: " + user.getUsername() + ", coins: " + user.getCoins());
            return new Response(HttpStatus.FORBIDDEN, ContentType.PLAIN_TEXT, "Forbidden: Not enough coins\r\n");
        } else {
            List<UUID> availablePackageIds = new ArrayList<>();

            try (Connection conn = Database.getConnection()) {
                String getAvailablePackagesQuery = "SELECT id FROM packages WHERE user_id IS NULL";

                try (PreparedStatement fetchStmt = conn.prepareStatement(getAvailablePackagesQuery);
                     ResultSet rs = fetchStmt.executeQuery()) {

                    while (rs.next()) {
                        UUID packageId = UUID.fromString(rs.getString("id"));
                        availablePackageIds.add(packageId);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            if (!availablePackageIds.isEmpty()) {
                int userId = userRepository.findUserIdByUsername(user.getUsername());
                UUID packageId = availablePackageIds.getFirst();

                String queryInsertPackage = "UPDATE packages SET user_id = (?) WHERE id = (?)";
                try (Connection conn = Database.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(queryInsertPackage)) {
                    stmt.setObject(1, userId);
                    stmt.setObject(2, packageId);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                userRepository.updateUserCoins(user, userCoins);

            } else {
                return new Response(HttpStatus.NOT_FOUND, ContentType.PLAIN_TEXT, "No card package available for buying\r\n");
            }


            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Package(s) acquired\r\n");
        }
    }

    public Response getUserCards(Request request) {
        String token = request.getHeaderMap().getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !isValidToken(token)) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        User user = userRepository.findUserByToken(token);
        if (user == null) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "User not found\r\n");
        }

        // Ensure the user ID is not null
        Integer userId = userRepository.findUserIdByUsername(user.getUsername());
        if (userId == null) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "User ID not found\r\n");
        }

        List<Card> userCards = new ArrayList<>();
        String query = "SELECT id, name, damage FROM cards WHERE package_id::text IN (SELECT id::text FROM packages WHERE user_id = ?)";

        try (Connection conn = Database.getConnection()) {
            if (conn == null) {
                return new Response(HttpStatus.INTERNAL_SERVER_ERROR, ContentType.PLAIN_TEXT, "Database connection failed\r\n");
            }

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        String name = rs.getString("name");
                        double damage = rs.getDouble("damage");
                        userCards.add(new Card(id, name, damage));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Database error: " + e.getMessage() + "\r\n");
        }

        if (userCards.isEmpty()) {
            return new Response(HttpStatus.OK, ContentType.JSON, "[]\r\n");
        }

        return new Response(HttpStatus.OK, ContentType.JSON, new Gson().toJson(userCards) + "\r\n");
    }

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

    public Response configureDeck(Request request) {
        String token = request.getHeaderMap().getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        token = token.substring(7);  // Strip "Bearer " prefix

        if (!isValidToken(token)) {
            return new Response(HttpStatus.UNAUTHORIZED, ContentType.PLAIN_TEXT, "Unauthorized\r\n");
        }

        Stack userStack = userStacks.get(token);
        if (userStack == null) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "User not found\r\n");
        }

        try {
            JSONArray cardIds = new JSONArray(request.getBody());

            if (cardIds.length() != 4) {
                return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Deck must contain exactly 4 cards\r\n");
            }

            Deck userDeck = new Deck();
            for (int i = 0; i < cardIds.length(); i++) {
                String cardId = cardIds.getString(i);
                Card card = userStack.getCardById(cardId);
                if (card == null) {
                    return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Card not found: " + cardId + "\r\n");
                }
                userDeck.addCard(card);
            }

            userDecks.put(token, userDeck);
            return new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Deck configured\r\n");

        } catch (JSONException e) {
            return new Response(HttpStatus.BAD_REQUEST, ContentType.PLAIN_TEXT, "Invalid request body - JSON parsing error\r\n");
        } catch (Exception e) {
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

                cards.add(new Card(id, name, damage));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cards;
    }
}