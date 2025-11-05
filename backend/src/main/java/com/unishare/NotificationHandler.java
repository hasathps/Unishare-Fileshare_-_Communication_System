package com.unishare;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationHandler {
    private static final String SYSTEM_USER = "System";
    
    public static void notifyFileUpload(String module, String filename, String uploader) {
        String message = String.format("📁 New file uploaded: %s by %s", filename, uploader);
        broadcastNotification(module, message);
    }
    
    public static void notifyFileDownload(String module, String filename, String downloader) {
        String message = String.format("📥 %s downloaded %s", downloader, filename);
        broadcastNotification(module, message);
    }
    
    private static void broadcastNotification(String module, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String notification = String.format("MESSAGE|%s|%s|%s|%s", 
            SYSTEM_USER, module, message, timestamp);
        
        // Broadcast to all clients in the module
        ChatServer.broadcastToModule(module, notification, null);
        System.out.println("[NOTIFICATION] " + module + ": " + message);
    }
}