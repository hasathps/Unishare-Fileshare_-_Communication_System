package com.unishare.controller;

import com.unishare.service.ModuleService;
import com.unishare.util.CORSFilter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Controller for handling module-related requests
 */
public class ModuleController implements HttpHandler {
    
    private final ModuleService moduleService;
    
    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            switch (method) {
                case "GET":
                    if (path.equals("/api/modules")) {
                        handleModules(exchange);
                    } else if (path.startsWith("/api/modules/")) {
                        handleModuleFiles(exchange);
                    }
                    break;
                case "OPTIONS":
                    CORSFilter.handlePreflightRequest(exchange);
                    break;
                default:
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("Error handling module request: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }
    
    public void handleModules(HttpExchange exchange) throws IOException {
        try {
            List<String> modules = moduleService.getAvailableModules();
            
            // Convert to JSON
            StringBuilder json = new StringBuilder("{\"modules\":[");
            for (int i = 0; i < modules.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(modules.get(i)).append("\"");
            }
            json.append("]}");
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length());
            exchange.getResponseBody().write(json.toString().getBytes());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to get modules: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to get modules");
        }
    }
    
    public void handleModuleFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String module = path.substring("/api/modules/".length());
        
        try {
            // This would return files for a specific module
            // For now, return a simple response
            String response = "{\"module\":\"" + module + "\",\"files\":[]}";
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to get module files: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to get module files");
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        CORSFilter.addCORSHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }
}
