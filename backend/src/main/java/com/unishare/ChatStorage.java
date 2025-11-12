package com.unishare;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatStorage {
    private static final String CHAT_DIR = "chat_messages";
    private static Map<String, List<String>> moduleMessages = new ConcurrentHashMap<>();
    
    static {
        loadAllMessages();
    }
    
    public static synchronized void saveMessage(String module, String user, String message, String timestamp) {
        String messageData = String.format("%s|%s|%s|%s", user, module, message, timestamp);
        List<String> messages = moduleMessages.computeIfAbsent(module, k -> Collections.synchronizedList(new ArrayList<>()));
        messages.add(messageData);
        saveToFile(module);
        System.out.println("💾 Saved message to " + module + ": " + messageData);
    }
    
    public static synchronized List<String> getMessages(String module) {
        List<String> messages = moduleMessages.getOrDefault(module, new ArrayList<>());
        System.out.println("📖 Retrieved " + messages.size() + " messages for " + module);
        return new ArrayList<>(messages); // Return copy to avoid concurrent modification
    }
    
    private static void saveToFile(String module) {
        try {
            Files.createDirectories(Paths.get(CHAT_DIR));
            Path filePath = Paths.get(CHAT_DIR, module + ".txt");
            List<String> messages = moduleMessages.get(module);
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                for (String msg : messages) {
                    writer.println(msg);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save messages for " + module + ": " + e.getMessage());
        }
    }
    
    private static void loadAllMessages() {
        try {
            Path chatDir = Paths.get(CHAT_DIR);
            if (!Files.exists(chatDir)) {
                Files.createDirectories(chatDir);
                return;
            }
            
            Files.list(chatDir)
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(ChatStorage::loadFromFile);
                
            System.out.println("📚 Loaded chat history for " + moduleMessages.size() + " modules");
        } catch (IOException e) {
            System.err.println("Failed to load chat messages: " + e.getMessage());
        }
    }
    
    private static void loadFromFile(Path filePath) {
        String module = filePath.getFileName().toString().replace(".txt", "");
        List<String> messages = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    messages.add(line.trim());
                }
            }
            moduleMessages.put(module, Collections.synchronizedList(messages));
            System.out.println("  - " + module + ": " + messages.size() + " messages loaded");
        } catch (IOException e) {
            System.err.println("Failed to load messages from " + filePath + ": " + e.getMessage());
        }
    }
}