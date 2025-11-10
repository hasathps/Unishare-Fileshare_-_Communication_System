package com.unishare;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String module;
    private MessageListener messageListener;
    
    public interface MessageListener {
        void onMessageReceived(String user, String module, String message, String timestamp);
    }
    
    public ChatClient(String username, String module, MessageListener listener) {
        this.username = username;
        this.module = module;
        this.messageListener = listener;
    }
    
    public boolean connect() {
        try {
            socket = new Socket("localhost", 9090);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Join the module
            out.println("JOIN|" + username + "|" + module);
            
            // Start listening for messages
            new Thread(this::listenForMessages).start();
            
            System.out.println("Connected to chat server as " + username + " in " + module);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to chat server: " + e.getMessage());
            return false;
        }
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println("MESSAGE|" + username + "|" + module + "|" + message);
        }
    }
    
    private void listenForMessages() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split("\\|", 5);
                if (parts.length >= 5 && "MESSAGE".equals(parts[0])) {
                    String user = parts[1];
                    String msgModule = parts[2];
                    String message = parts[3];
                    String timestamp = parts[4];
                    
                    if (messageListener != null) {
                        messageListener.onMessageReceived(user, msgModule, message, timestamp);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Connection to chat server lost: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        try {
            if (out != null) {
                out.println("LEAVE");
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
    
    // Test client
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter module (IN3111/CS101/MATH201): ");
        String module = scanner.nextLine();
        
        ChatClient client = new ChatClient(username, module, 
            (user, mod, message, timestamp) -> 
                System.out.println("[" + timestamp + "] " + user + ": " + message));
        
        if (client.connect()) {
            System.out.println("Type messages (or 'quit' to exit):");
            String input;
            while (!(input = scanner.nextLine()).equals("quit")) {
                client.sendMessage(input);
            }
            client.disconnect();
        }
        
        scanner.close();
    }
}