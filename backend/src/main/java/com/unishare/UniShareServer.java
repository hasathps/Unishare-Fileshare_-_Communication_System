package com.unishare;

import com.unishare.config.ServerConfig;
import com.unishare.controller.FileController;
import com.unishare.controller.ModuleController;
import com.unishare.service.FileService;
import com.unishare.service.ModuleService;
import com.unishare.util.CORSFilter;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * UniShare Backend Server
 * Core Java HTTP server for file sharing and communication
 */
public class UniShareServer {
    
    private static final int PORT = 8080;
    private HttpServer server;
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting UniShare Backend Server...");
        
        try {
            UniShareServer app = new UniShareServer();
            app.startServer();
            
            System.out.println("✅ UniShare Server started successfully!");
            System.out.println("🌐 Server running on: http://localhost:" + PORT);
            System.out.println("📁 File uploads will be saved to: ./uploads/");
            System.out.println("💬 WebSocket chat available on: ws://localhost:" + PORT + "/chat");
            System.out.println("\nPress Ctrl+C to stop the server");
            
            // Keep server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void startServer() throws IOException {
        // Create HTTP server
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Initialize services
        FileService fileService = new FileService();
        ModuleService moduleService = new ModuleService();
        
        // Initialize controllers
        FileController fileController = new FileController(fileService);
        ModuleController moduleController = new ModuleController(moduleService);
        
        // Register routes
        registerRoutes(fileController, moduleController);
        
        // Set executor for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Start server
        server.start();
    }
    
    private void registerRoutes(FileController fileController, 
                              ModuleController moduleController) {
        
        // File upload routes
        server.createContext("/api/upload", fileController::handleUpload);
        
        // Module management routes
        server.createContext("/api/modules", moduleController::handleModules);
        
        // Health check
        server.createContext("/api/health", this::handleHealthCheck);
    }
    
    private void handleHealthCheck(HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"ok\",\"message\":\"UniShare Backend is running\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }
    
    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Server stopped");
        }
    }
}
//netstat -ano | findstr :8080
//   taskkill /PID 13264 /F
//java -cp build\classes com.unishare.UniShareServer