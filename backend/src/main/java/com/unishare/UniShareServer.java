package com.unishare;

import com.unishare.controller.AuthController;
import com.unishare.controller.FileController;
import com.unishare.controller.ModuleController;
import com.unishare.service.AuthService;
import com.unishare.service.DatabaseService;
import com.unishare.service.FileMetadataService;
import com.unishare.service.FileService;
import com.unishare.service.ModuleService;
import com.unishare.service.SchemaInitializer;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.sql.SQLException;
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
        
        // Initialize database connection
        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            long latency = databaseService.verifyConnection();
            System.out.println("🗄️  Connected to Neon database (latency: " + latency + " ms)");
            SchemaInitializer.initialize(databaseService);
            System.out.println("🛠️  Database schema verified.");
        } catch (SQLException e) {
            System.err.println("❌ Unable to connect to Neon database: " + e.getMessage());
            throw new IOException("Database connection failed", e);
        }

        // Create services
        FileMetadataService fileMetadataService = new FileMetadataService(databaseService);
        FileService fileService = new FileService(fileMetadataService);
        ModuleService moduleService = new ModuleService();
        AuthService authService = new AuthService(databaseService);

        // Create controllers
        FileController fileController = new FileController(fileService, authService);
        ModuleController moduleController = new ModuleController(moduleService);
        AuthController authController = new AuthController(authService);

        // Register routes
        server.createContext("/api/upload", fileController);
        server.createContext("/api/modules", moduleController);
        server.createContext("/api/auth/login", authController);
        server.createContext("/api/auth/logout", authController);
        server.createContext("/api/auth/register", authController);
        server.createContext("/api/auth/me", authController);
        
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