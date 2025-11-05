package com.unishare;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 9090;
    private static Map<String, Set<ClientHandler>> moduleClients = new ConcurrentHashMap<>();
    private static Set<ClientHandler> allClients = ConcurrentHashMap.newKeySet();
    
    public static void main(String[] args) {
        System.out.println("💬 Starting Chat Server on port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                allClients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Chat server error: " + e.getMessage());
        }
    }
    
    public static void broadcastToModule(String module, String message, ClientHandler sender) {
        Set<ClientHandler> clients = moduleClients.get(module);
        if (clients != null) {
            clients.forEach(client -> {
                if (client != sender) {
                    client.sendMessage(message);
                }
            });
        }
    }
    
    public static void addClientToModule(String module, ClientHandler client) {
        moduleClients.computeIfAbsent(module, k -> ConcurrentHashMap.newKeySet()).add(client);
    }
    
    public static void removeClientFromModule(String module, ClientHandler client) {
        Set<ClientHandler> clients = moduleClients.get(module);
        if (clients != null) {
            clients.remove(client);
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private String currentModule;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] parts = inputLine.split("\\|", 4);
                    String command = parts[0];
                    
                    switch (command) {
                        case "JOIN":
                            if (parts.length >= 3) {
                                username = parts[1];
                                currentModule = parts[2];
                                addClientToModule(currentModule, this);
                                System.out.println(username + " joined " + currentModule);
                            }
                            break;
                            
                        case "MESSAGE":
                            if (parts.length >= 4) {
                                String user = parts[1];
                                String module = parts[2];
                                String message = parts[3];
                                String timestamp = java.time.LocalTime.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                                
                                String broadcastMsg = String.format("MESSAGE|%s|%s|%s|%s", 
                                    user, module, message, timestamp);
                                broadcastToModule(module, broadcastMsg, this);
                                System.out.println("[" + module + "] " + user + ": " + message);
                            }
                            break;
                            
                        case "LEAVE":
                            if (currentModule != null) {
                                removeClientFromModule(currentModule, this);
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + username);
            } finally {
                cleanup();
            }
        }
        
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
        
        private void cleanup() {
            try {
                if (currentModule != null) {
                    removeClientFromModule(currentModule, this);
                }
                allClients.remove(this);
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
        }
    }
}