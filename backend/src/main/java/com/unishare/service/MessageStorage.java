package com.unishare.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageStorage {
    private static final String CHAT_DIR = "chat_data";
    private static Map<String, List<Map<String, String>>> moduleMessages = new ConcurrentHashMap<>();
    
    public static void initialize() {
        createChatDirectory();
        loadAllMessages();
        System.out.println("💬 Chat storage initialized with " + moduleMessages.size() + " modules");
        for (String module : moduleMessages.keySet()) {
            System.out.println("  - " + module + ": " + moduleMessages.get(module).size() + " messages");
        }
    }
    
    private static void createChatDirectory() {
        try {
            Files.createDirectories(Paths.get(CHAT_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create chat directory: " + e.getMessage());
        }
    }
    
    public static void saveMessage(String module, String user, String message, String timestamp) {
        Map<String, String> msgData = new HashMap<>();
        msgData.put("user", user);
        msgData.put("message", message);
        msgData.put("module", module);
        msgData.put("timestamp", timestamp);
        
        moduleMessages.computeIfAbsent(module, k -> new ArrayList<>()).add(msgData);
        saveToFile(module);
    }
    
    public static List<Map<String, String>> getMessages(String module) {
        if (moduleMessages.isEmpty()) {
            initialize();
        }
        return moduleMessages.getOrDefault(module, new ArrayList<>());
    }
    
    private static void saveToFile(String module) {
        try {
            Path filePath = Paths.get(CHAT_DIR, module + ".txt");
            List<Map<String, String>> messages = moduleMessages.get(module);
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                for (Map<String, String> msg : messages) {
                    writer.println(String.format("%s|%s|%s|%s", 
                        msg.get("user"), msg.get("module"), msg.get("message"), msg.get("timestamp")));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save messages for " + module + ": " + e.getMessage());
        }
    }
    
    private static void loadAllMessages() {
        try {
            Files.list(Paths.get(CHAT_DIR))
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(MessageStorage::loadFromFile);
        } catch (IOException e) {
            System.err.println("Failed to load chat messages: " + e.getMessage());
        }
    }
    
    private static void loadFromFile(Path filePath) {
        String module = filePath.getFileName().toString().replace(".txt", "");
        List<Map<String, String>> messages = new ArrayList<>();
        
        System.out.println("[DEBUG] Loading messages from: " + filePath);
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DEBUG] Reading line: " + line);
                String[] parts = line.split("\\|", 4);
                if (parts.length >= 4) {
                    Map<String, String> msgData = new HashMap<>();
                    msgData.put("user", parts[0]);
                    msgData.put("module", parts[1]);
                    msgData.put("message", parts[2]);
                    msgData.put("timestamp", parts[3]);
                    messages.add(msgData);
                    System.out.println("[DEBUG] Added message: " + msgData);
                }
            }
            moduleMessages.put(module, messages);
            System.out.println("[DEBUG] Loaded " + messages.size() + " messages for module " + module);
        } catch (IOException e) {
            System.err.println("Failed to load messages from " + filePath + ": " + e.getMessage());
        }
    }
}