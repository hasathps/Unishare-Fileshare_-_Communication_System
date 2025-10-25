package com.unishare;

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
 * Main server class for UniShare application
 */
public class UniShareServer {
    
    private static final int PORT = 8080;
    private HttpServer server;
    
    public static void main(String[] args) {
        try {
            UniShareServer app = new UniShareServer();
            app.start();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void start() throws IOException {
        System.out.println("🚀 Starting UniShare Server...");
        
        // Create HTTP server
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Create services
        FileService fileService = new FileService();
        ModuleService moduleService = new ModuleService();
        
        // Create controllers
        FileController fileController = new FileController(fileService);
        ModuleController moduleController = new ModuleController(moduleService);
        
        // Register routes
        server.createContext("/api/upload", fileController);
        server.createContext("/api/modules", moduleController);
        
        // Set thread pool
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Start server
        server.start();
        
        System.out.println("✅ UniShare Server started successfully!");
        System.out.println("🌐 Server running on: http://localhost:" + PORT);
        System.out.println("📁 Upload endpoint: http://localhost:" + PORT + "/api/upload");
        System.out.println("📋 Modules endpoint: http://localhost:" + PORT + "/api/modules");
        System.out.println("⏹️  Press Ctrl+C to stop the server");
        
        // Keep server running
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down UniShare Server...");
            server.stop(0);
            System.out.println("✅ Server stopped successfully!");
        }));
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
//netstat -ano | findstr :8080
//   taskkill /PID 13264 /F
//java -cp build\classes com.unishare.UniShareServer