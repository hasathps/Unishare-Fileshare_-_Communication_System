package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unishare.service.MonitoringService;
import com.unishare.util.CORSFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Exposes aggregated monitoring metrics for the frontend dashboard.
 */
public class MonitorController implements HttpHandler {

    private final MonitoringService monitoringService;

    public MonitorController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET":
                handleSnapshot(exchange);
                break;
            case "OPTIONS":
                CORSFilter.handlePreflightRequest(exchange);
                break;
            default:
                sendErrorResponse(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleSnapshot(HttpExchange exchange) throws IOException {
        try {
            String payload = monitoringService.getDashboardSnapshotJson();
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try {
                exchange.getResponseBody().write(bytes);
            } finally {
                exchange.getResponseBody().close();
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to build monitoring snapshot: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to load monitoring data");
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int status, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        CORSFilter.addCORSHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try {
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.getResponseBody().close();
        }
    }
}

