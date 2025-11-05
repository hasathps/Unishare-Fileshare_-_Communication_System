package com.unishare.controller;

import com.unishare.model.ChatMessage;
import com.unishare.service.ChatService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ChatController implements HttpHandler {
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        String method = exchange.getRequestMethod();
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        if ("POST".equals(method)) {
            handlePostMessage(exchange);
        } else if ("GET".equals(method)) {
            handleGetMessages(exchange);
        }
    }
    
    private void handlePostMessage(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(body);
        
        String user = params.get("user");
        String message = params.get("message");
        String module = params.get("module");
        
        if (user != null && message != null && module != null) {
            ChatMessage chatMessage = new ChatMessage(user, message, module);
            chatService.addMessage(chatMessage);
            chatService.addUser(user);
            
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
        
        StringBuilder response = new StringBuilder("{\"messages\":[");
        var messages = chatService.getMessages(module);
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) response.append(",");
            response.append(messages.get(i).toJson());
        }
        response.append("],\"onlineUsers\":[");
        
        var users = chatService.getOnlineUsers();
        int i = 0;
        for (String user : users) {
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