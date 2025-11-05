package com.unishare.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    private String user;
    private String message;
    private String module;
    private String timestamp;
    
    public ChatMessage(String user, String message, String module) {
        this.user = user;
        this.message = message;
        this.module = module;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    public String getUser() { return user; }
    public String getMessage() { return message; }
    public String getModule() { return module; }
    public String getTimestamp() { return timestamp; }
    
    public String toJson() {
        return String.format("{\"user\":\"%s\",\"message\":\"%s\",\"module\":\"%s\",\"timestamp\":\"%s\"}", 
                           user, message, module, timestamp);
    }
}