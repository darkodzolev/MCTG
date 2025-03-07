import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import http.ContentType;
import model.User;
import model.Card;
import org.junit.jupiter.api.*;
import org.mockito.*;
import repository.UserRepository;
import service.UserService;
import service.CardService;
import utils.Database;
import server.Response;
import http.HttpStatus;
import server.Request;
import routing.ClientHandler;
import routing.RouteHandler;
import http.Method;
import server.HeaderMap;
import service.AuthService;
import utils.RequestParser;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;
import utils.Router;
import server.Service;
import server.Server;

import java.sql.*;
import java.util.*;

class UnitTest {

    private Router router;
    private Service mockService;
    private HeaderMap headerMap;

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private CardService mockCardService;

    @InjectMocks
    private CardService cardService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService();

        router = new Router();
        mockService = mock(Service.class);

        headerMap = new HeaderMap();
    }

    @Test
    void testProcessWelcome() {
        // Create an instance of the client handler
        ClientHandler clientHandler = new ClientHandler();

        // Create and mock the request object
        Request request = mock(Request.class);

        // Mock the request method and path
        when(request.getMethod()).thenReturn(Method.GET);
        when(request.getPathname()).thenReturn("/");

        // Mock the header map (to prevent NullPointerException)
        HeaderMap headerMap = mock(HeaderMap.class);
        when(request.getHeaderMap()).thenReturn(headerMap);  // Ensure headerMap is not null

        // Call the method to process the request
        Response response = clientHandler.handleRequest(request);

        // Assertions to verify the response status and body
        assertResponse(response, HttpStatus.OK, "Welcome to the MTCG Server!\r\n");
    }


    @Test
    void testRouteUserRegistration() {
        // Create an instance of the client handler
        ClientHandler clientHandler = new ClientHandler();

        String requestBody = "{ \"Username\": \"darko\", \"Password\": \"password123\" }";

        // Simulate a POST request to /users
        Request request = mock(Request.class);
        when(request.getMethod()).thenReturn(Method.POST);  // Adjusting for String return value of getMethod()
        when(request.getPathname()).thenReturn("/users");
        when(request.getBody()).thenReturn(requestBody);

        // Mock HeaderMap to prevent NullPointerException
        HeaderMap headerMap = mock(HeaderMap.class);
        when(request.getHeaderMap()).thenReturn(headerMap);  // Return mocked HeaderMap

        // Mock UserService behavior for registration
        UserService mockUserService = mock(UserService.class);
        when(mockUserService.registerUser(requestBody)).thenReturn(new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "User registered successfully\r\n"));

        // Call the method to handle the request
        Response response = clientHandler.handleRequest(request);

        // Assertions
        assertResponse(response, HttpStatus.CREATED, "User registered successfully\r\n");
    }

    @Test
    void testRouteWithValidMethodAndPath() {
        RouteHandler routeHandler = new RouteHandler();

        // Set up a mock handler for the path "/test"
        routeHandler.addRoute("GET", "/test", request -> new Response(HttpStatus.OK, ContentType.PLAIN_TEXT, "Test successful"));

        // Simulate a GET request to /test
        Request request = mock(Request.class);
        when(request.getMethod()).thenReturn(Method.GET);
        when(request.getPathname()).thenReturn("/test");

        // Call the route method
        Response response = routeHandler.route(request);

        // Assertions
        assertResponse(response, HttpStatus.OK, "Test successful");
    }

    @Test
    void testAuthenticateUserSuccess() {
        // Arrange
        Map<String, User> users = new HashMap<>();
        User user = new User("joel", "password123", 20);
        user.setToken("validToken"); // Assuming User has a setToken method
        users.put(user.getUsername(), user);

        AuthService authService = new AuthService(users);
        String validToken = "validToken";

        // Act
        Response response = authService.authenticateUser(validToken);

        // Assert
        assertResponse(response, HttpStatus.OK, "User authenticated. Welcome, joel!\r\n");
    }

    @Test
    void testAuthenticateUserWithNullToken() {
        // Arrange
        Map<String, User> users = new HashMap<>();
        AuthService authService = new AuthService(users);
        String nullToken = null;

        // Act
        Response response = authService.authenticateUser(nullToken);

        // Assert
        assertResponse(response, HttpStatus.UNAUTHORIZED, "Unauthorized: No token provided\r\n");
    }

    @Test
    void testAuthenticateUserWithValidTokenMultipleUsers() {
        // Arrange
        Map<String, User> users = new HashMap<>();
        User user1 = new User("joel", "password123", 20);
        user1.setToken("token1");
        users.put(user1.getUsername(), user1);

        User user2 = new User("darko", "password123", 25);
        user2.setToken("token2");
        users.put(user2.getUsername(), user2);

        AuthService authService = new AuthService(users);
        String validToken = "token2";

        // Act
        Response response = authService.authenticateUser(validToken);

        // Assert
        assertResponse(response, HttpStatus.OK, "User authenticated. Welcome, darko!\r\n");
    }

    @Test
    void testAuthenticateUserMultipleInvalidTokens() {
        // Arrange
        Map<String, User> users = new HashMap<>();
        User user = new User("joel", "password123", 20);
        user.setToken("validToken");
        users.put(user.getUsername(), user);

        AuthService authService = new AuthService(users);
        String[] invalidTokens = {"invalid1", "invalid2", "invalid3"};

        // Act & Assert
        for (String invalidToken : invalidTokens) {
            Response response = authService.authenticateUser(invalidToken);
            assertResponse(response, HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid token\r\n");
        }
    }

    @Test
    void testGetConnection() throws SQLException {
        try (Connection conn = Database.getConnection()) {
            assertNotNull(conn);
        }
    }

    @Test
    void testSetAndGetFirstLoginFlag() {
        Database.setFirstLoginFlag(true);
        assertTrue(Database.isFirstLogin());

        Database.setFirstLoginFlag(false);
        assertFalse(Database.isFirstLogin());
    }

    // --- Tests for UserService ---

    @Test
    void testRegisterUserSuccess() throws SQLException {
        String body = "{ \"Username\": \"joel\", \"Password\": \"password123\" }";
        User newUser = new User("john", "password123", 20); // Create a new user object with initial coins as 20

        // Mock the behavior of the repository methods
        when(mockUserRepository.findUserByUsername("joel")).thenReturn(null); // No user found
        doNothing().when(mockUserRepository).registerUser(any(User.class)); // Simulate successful registration

        // Call the service method to register the user
        Response response = userService.registerUser(body);

        // Assertions using assertResponse method (which checks status and body)
        assertResponse(response, HttpStatus.CREATED, "User registered successfully\r\n");
    }


    @Test
    void testRegisterUserAlreadyExists() {
        String body = "{ \"Username\": \"joel\", \"Password\": \"password123\" }";
        when(mockUserRepository.findUserByUsername("joel")).thenReturn(new User("joel", "password123", 20));

        Response response = userService.registerUser(body);
        assertResponse(response, HttpStatus.CONFLICT, "Conflict: User already exists\r\n");
    }

    @Test
    void testLoginUserSuccess() {
        String body = "{ \"Username\": \"joel\", \"Password\": \"password123\" }";
        User user = new User("joel", "password123", 20);
        when(mockUserRepository.findUserByUsername("joel")).thenReturn(user);

        Response response = userService.loginUser(body);
        assertResponse(response, HttpStatus.OK, "Login successful");
    }

    @Test
    void testLoginUserAfterFirstLoginDoesNotCreateCard() throws SQLException {
        String body = "{ \"Username\": \"darko\", \"Password\": \"password123\" }";
        User user = new User("darko", "password123", 20);
        when(mockUserRepository.findUserByUsername("darko")).thenReturn(user);

        Database.setFirstLoginFlag(false);  // Simulate second login
        Response response = userService.loginUser(body);

        assertResponse(response, HttpStatus.OK, "Login successful");
        verify(mockCardService, never()).createPackage(any(), any());
    }

    // --- Tests for UserRepository ---

    @Test
    void testFindUserByUsernameFound() {
        User user = new User("joel", "password123", 20);
        when(mockUserRepository.findUserByUsername("joel")).thenReturn(user);

        User result = mockUserRepository.findUserByUsername("joel");

        assertNotNull(result);
        assertEquals("joel", result.getUsername());
    }

    @Test
    void testUpdateUserCoins() {
        User user = new User("roy", "password123", 20);
        mockUserRepository.updateUserCoins(user, 25);

        verify(mockUserRepository).updateUserCoins(eq(user), eq(25));
    }

    @Test
    void testCardConstructor() {
        // Given
        String id = "123";
        String name = "Fireball";
        double damage = 50.0;

        // When
        Card card = new Card(id, name, damage);

        // Then
        assertEquals(id, card.getId());
        assertEquals(name, card.getName());
        assertEquals(damage, card.getDamage());
    }

    @Test
    void testCreatePackageSuccess() throws SQLException {
        List<Card> cardList = Arrays.asList(new Card("1", "Card A", 10), new Card("2", "Card B", 15));
        String adminToken = "adminToken";

        // Create an admin user object
        User adminUser = new User("admin", "password", 100);

        // Mock the behavior of the userRepository and cardService methods
        when(mockUserRepository.findUserByToken(adminToken)).thenReturn(adminUser); // Simulate admin user found by token
        when(mockCardService.createPackage(cardList, adminToken)).thenReturn(new Response(HttpStatus.CREATED, ContentType.PLAIN_TEXT, "Package(s) created successfully\r\n"));

        // Call the service method to create the package
        Response response = mockCardService.createPackage(cardList, adminToken);

        // Assertions to verify the response's HTTP status and message
        assertResponse(response, HttpStatus.CREATED, "Package(s) created successfully\r\n");
    }

    @Test
    void testGetResponse() {
        // Given
        HttpStatus status = HttpStatus.OK;
        ContentType contentType = ContentType.PLAIN_TEXT;
        String content = "Hello, World!";
        Response response = new Response(status, contentType, content);

        // When
        String actualResponse = response.get();

        // Then
        // Check if the response contains the correct status and message
        assertTrue(actualResponse.contains("HTTP/1.1 200 OK"));
        assertTrue(actualResponse.contains("Content-Type: text/plain"));
        assertTrue(actualResponse.contains("Content-Length: " + content.length()));

        // Check if the content is part of the response body
        assertTrue(actualResponse.contains(content));

        // Check if Date and Expires headers are correctly formatted
        assertTrue(actualResponse.contains("Date:"));
        assertTrue(actualResponse.contains("Expires:"));
    }

    @Test
    void testParseHttpRequest_GET() throws IOException {
        String httpRequest = "GET /path HTTP/1.1\nHost: localhost\nUser-Agent: test-agent\n\n";
        BufferedReader reader = new BufferedReader(new StringReader(httpRequest));

        Map<String, String> requestData = RequestParser.parseHttpRequest(reader);

        assertEquals("GET", requestData.get("method"));
        assertEquals("/path", requestData.get("path"));
        assertEquals("localhost", requestData.get("Host"));
        assertEquals("test-agent", requestData.get("User-Agent"));
    }

    @Test
    void testAddService() {
        String route = "POST /users";
        router.addService(route, mockService);

        // Check if the service was correctly added
        Service resolvedService = router.resolve("POST", "/users");
        assertEquals(mockService, resolvedService, "The service should match the mocked service.");
    }

    @Test
    void testGetHeader() {
        String headerLine = "Content-Length: 1234";
        headerMap.ingest(headerLine);

        // Test retrieval of the header
        String contentLength = headerMap.getHeader("Content-Length");
        assertEquals("1234", contentLength, "The Content-Length header should be retrievable.");
    }

    @Test
    void testServerCreation() {
        // Test the constructor and ensure proper instantiation
        Router mockRouter = mock(Router.class);
        Server server = new Server(10001, mockRouter);

        // Assert that the server instance is created with the correct port and router
        assertNotNull(server);
    }



    // --- Helper method for assertions ---
    private void assertResponse(Response response, HttpStatus expectedStatus, String expectedBody) {
        assertNotNull(response, "Response should not be null");

        // Get the response string
        String fullResponse = response.get();
        System.out.println("Actual Response: " + fullResponse); // Debugging

        // Ensure the response matches expected format
        assertTrue(fullResponse.contains("HTTP/1.1 " + expectedStatus.code + " " + expectedStatus.message),
                "Expected HTTP status not found in response");
        assertTrue(fullResponse.contains(expectedBody),
                "Expected response body not found in response");
    }
}