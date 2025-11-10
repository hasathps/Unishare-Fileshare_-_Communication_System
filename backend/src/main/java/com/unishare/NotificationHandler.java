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
    
    public static void notifyUserJoined(String module, String username) {
        String message = String.format("👋 %s joined the chat", username);
        broadcastNotification(module, message);
    }
    
    public static void notifyUserLeft(String module, String username) {
        String message = String.format("👋 %s left the chat", username);
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
    
    // Test notifications
    public static void main(String[] args) {
        // Simulate notifications
        notifyFileUpload("IN3111", "lecture_notes.pdf", "John Doe");
        notifyUserJoined("CS101", "Jane Smith");
        notifyFileDownload("MATH201", "assignment.docx", "Mike Johnson");
    }
}