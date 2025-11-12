package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unishare.model.User;
import com.unishare.service.AuthService;
import com.unishare.service.NotificationService;
import com.unishare.util.CORSFilter;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for handling notification-related requests
 */
public class NotificationController implements HttpHandler {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // Handle preflight
            if ("OPTIONS".equals(method)) {
                CORSFilter.handlePreflightRequest(exchange);
                return;
            }

            // All notification endpoints require authentication
            Optional<User> user = authService.findBySessionToken(extractToken(exchange));
            if (user.isEmpty()) {
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

            UUID userId = user.get().getId();

            switch (method) {
                case "GET":
                    if (path.equals("/api/notifications")) {
                        handleGetNotifications(exchange, userId);
                    } else if (path.equals("/api/notifications/unread")) {
                        handleGetUnreadNotifications(exchange, userId);
                    } else if (path.equals("/api/notifications/count")) {
                        handleGetUnreadCount(exchange, userId);
                    } else {
                        sendErrorResponse(exchange, 404, "Not found");
                    }
                    break;

                case "POST":
                    if (path.equals("/api/notifications/mark-all-read")) {
                        handleMarkAllAsRead(exchange, userId);
                    } else if (path.startsWith("/api/notifications/") && path.endsWith("/read")) {
                        handleMarkAsRead(exchange, userId);
                    } else {
                        sendErrorResponse(exchange, 404, "Not found");
                    }
                    break;

                case "DELETE":
                    if (path.equals("/api/notifications")) {
                        handleClearNotifications(exchange, userId);
                    } else {
                        sendErrorResponse(exchange, 404, "Not found");
                    }
                    break;

                default:
                    sendErrorResponse(exchange, 405, "Method not allowed");
            }

        } catch (Exception e) {
            System.err.println("Error in NotificationController: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    /**
     * GET /api/notifications - Get all notifications (read and unread)
     */
    private void handleGetNotifications(HttpExchange exchange, UUID userId) throws IOException {
        List<NotificationService.Notification> notifications = notificationService.getAllNotifications(userId);

        String notificationsJson = notifications.stream()
                .map(NotificationService.Notification::toJson)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String response = String.format("{\"notifications\":[%s]}", notificationsJson);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * GET /api/notifications/unread - Get only unread notifications
     */
    private void handleGetUnreadNotifications(HttpExchange exchange, UUID userId) throws IOException {
        List<NotificationService.Notification> notifications = notificationService.getUnreadNotifications(userId);

        String notificationsJson = notifications.stream()
                .map(NotificationService.Notification::toJson)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String response = String.format("{\"notifications\":[%s]}", notificationsJson);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * GET /api/notifications/count - Get count of unread notifications
     */
    private void handleGetUnreadCount(HttpExchange exchange, UUID userId) throws IOException {
        int count = notificationService.getUnreadCount(userId);
        String response = String.format("{\"count\":%d}", count);
        sendJsonResponse(exchange, 200, response);
    }

    /**
     * POST /api/notifications/{id}/read - Mark a specific notification as read
     */
    private void handleMarkAsRead(HttpExchange exchange, UUID userId) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String idStr = path.substring("/api/notifications/".length(), path.lastIndexOf("/read"));

        try {
            UUID notificationId = UUID.fromString(idStr);
            boolean success = notificationService.markAsRead(userId, notificationId);

            if (success) {
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Notification marked as read\"}");
            } else {
                sendErrorResponse(exchange, 404, "Notification not found");
            }
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, "Invalid notification ID");
        }
    }

    /**
     * POST /api/notifications/mark-all-read - Mark all notifications as read
     */
    private void handleMarkAllAsRead(HttpExchange exchange, UUID userId) throws IOException {
        notificationService.markAllAsRead(userId);
        sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"All notifications marked as read\"}");
    }

    /**
     * DELETE /api/notifications - Clear all notifications
     */
    private void handleClearNotifications(HttpExchange exchange, UUID userId) throws IOException {
        notificationService.clearNotifications(userId);
        sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"All notifications cleared\"}");
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

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        CORSFilter.addCORSHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().close();
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = String.format("{\"error\":\"%s\"}", message);
        sendJsonResponse(exchange, statusCode, json);
    }
}
