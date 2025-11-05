package com.unishare.controller;

import com.unishare.service.MessageStorage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketChatController implements HttpHandler {
    private static Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    
    static {
        MessageStorage.initialize();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        String method = exchange.getRequestMethod();
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        if ("POST".equals(method)) {
            handleSendMessage(exchange);
        } else if ("GET".equals(method)) {
            handleGetMessages(exchange);
        }
    }
    
    private void handleSendMessage(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(body);
        
        String user = params.get("user");
        String message = params.get("message");
        String module = params.get("module");
        
        if (user != null && message != null && module != null) {
            String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            
            MessageStorage.saveMessage(module, user, message, timestamp);
            activeUsers.add(user);
            
            // Also send to TCP chat server for real-time broadcasting
            sendToTCPServer(user, module, message);
            
            String response = "{\"status\":\"success\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
        } else {
            exchange.sendResponseHeaders(400, -1);
        }
        exchange.getResponseBody().close();
    }
    
    private void handleGetMessages(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String module = "";
        
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && "module".equals(keyValue[0])) {
                    module = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                }
            }
        }
        
        System.out.println("[DEBUG] Getting messages for module: " + module);
        List<Map<String, String>> messages = MessageStorage.getMessages(module);
        System.out.println("[DEBUG] Found " + messages.size() + " messages");
        
        StringBuilder response = new StringBuilder("{\"messages\":[");
        
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) response.append(",");
            Map<String, String> msg = messages.get(i);
            response.append(String.format("{\"user\":\"%s\",\"message\":\"%s\",\"module\":\"%s\",\"timestamp\":\"%s\"}", 
                msg.get("user"), msg.get("message"), msg.get("module"), msg.get("timestamp")));
        }
        
        response.append("],\"onlineUsers\":[");
        int i = 0;
        for (String user : activeUsers) {
            if (i > 0) response.append(",");
            response.append("\"").append(user).append("\"");
            i++;
        }
        response.append("]}");
        
        System.out.println("[DEBUG] Response: " + response.toString());
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        exchange.getResponseBody().write(response.toString().getBytes());
        exchange.getResponseBody().close();
    }
    
    private void sendToTCPServer(String user, String module, String message) {
        // This would connect to the TCP chat server and send the message
        // For now, we'll just log it
        System.out.println("[TCP BRIDGE] " + user + " in " + module + ": " + message);
    }
    
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    params.put(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
                              URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                } catch (Exception e) {
                    // Skip invalid pairs
                }
            }
        }
        return params;
    }
}