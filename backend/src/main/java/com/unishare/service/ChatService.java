package com.unishare.service;

import com.unishare.model.ChatMessage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatService {
    private final Map<String, List<ChatMessage>> moduleMessages = new ConcurrentHashMap<>();
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    
    public void addMessage(ChatMessage message) {
        moduleMessages.computeIfAbsent(message.getModule(), k -> new CopyOnWriteArrayList<>()).add(message);
    }
    
    public List<ChatMessage> getMessages(String module) {
        return moduleMessages.getOrDefault(module, new ArrayList<>());
    }
    
    public void addUser(String user) {
        onlineUsers.add(user);
    }
    
    public void removeUser(String user) {
        onlineUsers.remove(user);
    }
    
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers);
    }
}