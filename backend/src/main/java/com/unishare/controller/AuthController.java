package com.unishare.controller;

import com.unishare.model.User;
import com.unishare.service.AuthService;
import com.unishare.service.AuthService.AuthenticationException;
import com.unishare.util.CORSFilter;
import com.unishare.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles authentication related HTTP endpoints.
 */
public class AuthController implements HttpHandler {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "POST":
                    handlePost(exchange, path);
                    break;
                case "GET":
                    handleGet(exchange, path);
                    break;
                case "OPTIONS":
                    CORSFilter.handlePreflightRequest(exchange);
                    break;
                default:
                    sendJson(exchange, 405, Map.of("error", "Method Not Allowed"));
                    break;
            }
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "Internal Server Error"));
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if ("/api/auth/login".equals(path)) {
            handleLogin(exchange);
        } else if ("/api/auth/logout".equals(path)) {
            handleLogout(exchange);
        } else if ("/api/auth/register".equals(path)) {
            handleRegister(exchange);
        } else {
            sendJson(exchange, 404, Map.of("error", "Not Found"));
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if ("/api/auth/me".equals(path)) {
            handleCurrentUser(exchange);
        } else {
            sendJson(exchange, 404, Map.of("error", "Not Found"));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, String> payload = readJsonBody(exchange);
        String email = payload.get("email");
        String password = payload.get("password");
        String displayName = payload.get("displayName");

        try {
            User user = authService.register(email, password, displayName);
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId().toString());
            response.put("email", user.getEmail());
            response.put("displayName", user.getDisplayName());
            sendJson(exchange, 201, response);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            throw new IOException("Failed to register user", e);
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> payload = readJsonBody(exchange);
        String email = payload.get("email");
        String password = payload.get("password");

        String remoteIp = exchange.getRemoteAddress() != null
                ? exchange.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        try {
            AuthService.LoginResult loginResult = authService.login(email, password, remoteIp);

            exchange.getResponseHeaders().add("Set-Cookie",
                    authService.getSessionCookieName() + "=" + loginResult.token()
                            + "; HttpOnly; Path=/; SameSite=Strict");

            Map<String, Object> response = new HashMap<>();
            response.put("id", loginResult.user().getId().toString());
            response.put("token", loginResult.token());
            response.put("email", loginResult.user().getEmail());
            response.put("displayName", loginResult.user().getDisplayName());

            sendJson(exchange, 200, response);
        } catch (AuthenticationException e) {
            sendJson(exchange, 401, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            throw new IOException("Failed to login user", e);
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = extractToken(exchange);
        if (token != null) {
            authService.logout(token);
        }

        exchange.getResponseHeaders().add("Set-Cookie",
                authService.getSessionCookieName() + "=deleted; Max-Age=0; Path=/; HttpOnly; SameSite=Strict");

        sendJson(exchange, 200, Map.of("success", true));
    }

    private void handleCurrentUser(HttpExchange exchange) throws IOException {
        String token = extractToken(exchange);
        Optional<User> user = authService.findBySessionToken(token);

        if (user.isEmpty()) {
            sendJson(exchange, 401, Map.of("error", "Not authenticated"));
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.get().getId().toString());
        response.put("email", user.get().getEmail());
        response.put("displayName", user.get().getDisplayName());
        sendJson(exchange, 200, response);
    }

    private Map<String, String> readJsonBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return JsonUtils.parseObject(builder.toString());
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Map<String, ?> payload) throws IOException {
        CORSFilter.addCORSHeaders(exchange);
        String json = JsonUtils.toJson(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private String extractToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String header : cookies) {
            for (HttpCookie cookie : HttpCookie.parse(header)) {
                if (authService.getSessionCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

