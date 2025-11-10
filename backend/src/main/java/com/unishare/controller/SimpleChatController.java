package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleChatController implements HttpHandler {
    private static Map<String, List<Map<String, String>>> moduleMessages = new ConcurrentHashMap<>();
    private static Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    
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
            
            Map<String, String> msgData = new HashMap<>();
            msgData.put("user", user);
            msgData.put("message", message);
            msgData.put("module", module);
            msgData.put("timestamp", timestamp);
            
            moduleMessages.computeIfAbsent(module, k -> new ArrayList<>()).add(msgData);
            activeUsers.add(user);
            
            System.out.println("[CHAT] " + module + " - " + user + ": " + message);
            
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
        
        List<Map<String, String>> messages = moduleMessages.getOrDefault(module, new ArrayList<>());
        
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
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        exchange.getResponseBody().write(response.toString().getBytes());
        exchange.getResponseBody().close();
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