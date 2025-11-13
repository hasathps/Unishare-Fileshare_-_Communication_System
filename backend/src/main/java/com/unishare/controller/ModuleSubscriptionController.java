package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unishare.model.User;
import com.unishare.service.AuthService;
import com.unishare.service.ModuleService;
import com.unishare.service.ModuleSubscriptionService;
import com.unishare.util.CORSFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.List;

/**
 * Controller for handling module subscription operations via HTTP.
 */
public class ModuleSubscriptionController implements HttpHandler {

    private final ModuleSubscriptionService subscriptionService;
    private final ModuleService moduleService;
    private final AuthService authService;

    public ModuleSubscriptionController(
            ModuleSubscriptionService subscriptionService,
            ModuleService moduleService,
            AuthService authService) {
        this.subscriptionService = subscriptionService;
        this.moduleService = moduleService;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // Authenticate user
            User user = authenticateUser(exchange);
            if (user == null) {
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

            switch (method) {
                case "POST":
                    if (path.matches("/api/subscriptions/[^/]+/subscribe")) {
                        handleSubscribe(exchange, user);
                    } else if (path.matches("/api/subscriptions/[^/]+/unsubscribe")) {
                        handleUnsubscribe(exchange, user);
                    } else {
                        sendErrorResponse(exchange, 404, "Not Found");
                    }
                    break;
                case "GET":
                    if (path.equals("/api/subscriptions")) {
                        handleGetUserSubscriptions(exchange, user);
                    } else if (path.matches("/api/subscriptions/[^/]+/status")) {
                        handleCheckSubscription(exchange, user);
                    } else {
                        sendErrorResponse(exchange, 404, "Not Found");
                    }
                    break;
                case "OPTIONS":
                    CORSFilter.handlePreflightRequest(exchange);
                    break;
                default:
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("Error handling subscription request: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void handleSubscribe(HttpExchange exchange, User user) throws IOException {
        String moduleCode = extractModuleCode(exchange.getRequestURI().getPath());

        // Validate module exists
        if (!moduleService.isValidModule(moduleCode)) {
            sendErrorResponse(exchange, 404, "Module not found");
            return;
        }

        String result = subscriptionService.subscribe(user.getId(), moduleCode);

        if (result.equals("SUCCESS")) {
            String response = String.format(
                    "{\"success\":true,\"message\":\"Subscribed to %s\",\"moduleCode\":\"%s\"}",
                    moduleCode, moduleCode);
            sendJsonResponse(exchange, 200, response);
        } else if (result.equals("ALREADY_SUBSCRIBED")) {
            String response = String.format(
                    "{\"success\":false,\"message\":\"Already subscribed to %s\"}",
                    moduleCode);
            sendJsonResponse(exchange, 200, response);
        } else {
            sendErrorResponse(exchange, 500, "Subscription failed");
        }
    }

    private void handleUnsubscribe(HttpExchange exchange, User user) throws IOException {
        String moduleCode = extractModuleCode(exchange.getRequestURI().getPath());

        String result = subscriptionService.unsubscribe(user.getId(), moduleCode);

        if (result.equals("SUCCESS")) {
            String response = String.format(
                    "{\"success\":true,\"message\":\"Unsubscribed from %s\",\"moduleCode\":\"%s\"}",
                    moduleCode, moduleCode);
            sendJsonResponse(exchange, 200, response);
        } else if (result.equals("NOT_SUBSCRIBED")) {
            String response = String.format(
                    "{\"success\":false,\"message\":\"Not subscribed to %s\"}",
                    moduleCode);
            sendJsonResponse(exchange, 200, response);
        } else {
            sendErrorResponse(exchange, 500, "Unsubscription failed");
        }
    }

    private void handleCheckSubscription(HttpExchange exchange, User user) throws IOException {
        String moduleCode = extractModuleCode(exchange.getRequestURI().getPath());

        boolean isSubscribed = subscriptionService.isSubscribed(user.getId(), moduleCode);

        String response = String.format(
                "{\"subscribed\":%s,\"moduleCode\":\"%s\"}",
                isSubscribed, moduleCode);
        sendJsonResponse(exchange, 200, response);
    }

    private void handleGetUserSubscriptions(HttpExchange exchange, User user) throws IOException {
        List<String> subscriptions = subscriptionService.getUserSubscriptions(user.getId());

        StringBuilder json = new StringBuilder("{\"subscriptions\":[");
        for (int i = 0; i < subscriptions.size(); i++) {
            if (i > 0)
                json.append(",");
            json.append("\"").append(subscriptions.get(i)).append("\"");
        }
        json.append("]}");

        sendJsonResponse(exchange, 200, json.toString());
    }

    private String extractModuleCode(String path) {
        // Extract module code from paths like
        // /api/subscriptions/artificial-intelligence/subscribe
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "";
    }

    private User authenticateUser(HttpExchange exchange) {
        String token = extractToken(exchange);
        if (token == null)
            return null;
        return authService.findBySessionToken(token).orElse(null);
    }

    private String extractToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null)
            return null;

        for (String header : cookies) {
            for (HttpCookie cookie : HttpCookie.parse(header)) {
                if (authService.getSessionCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        CORSFilter.addCORSHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        sendJsonResponse(exchange, code, response);
    }
}
