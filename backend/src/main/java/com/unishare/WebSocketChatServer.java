package com.unishare;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketChatServer {
    private static final int WS_PORT = 8084;
    private static final String WS_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static Map<String, Set<WebSocketClient>> moduleClients = new ConcurrentHashMap<>();
    private static Set<WebSocketClient> allClients = ConcurrentHashMap.newKeySet();
    
    public static void main(String[] args) {
        System.out.println("🌐 Starting WebSocket Chat Server on port " + WS_PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(WS_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                WebSocketClient client = new WebSocketClient(clientSocket);
                allClients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("WebSocket server error: " + e.getMessage());
        }
    }
    
    public static void broadcastToModule(String module, String message, WebSocketClient sender) {
        Set<WebSocketClient> clients = moduleClients.get(module);
        if (clients != null) {
            clients.forEach(client -> {
                if (client != sender && client.isConnected()) {
                    client.sendMessage(message);
                }
            });
        }
    }
    
    public static void addClientToModule(String module, WebSocketClient client) {
        moduleClients.computeIfAbsent(module, k -> ConcurrentHashMap.newKeySet()).add(client);
    }
    
    public static void removeClientFromModule(String module, WebSocketClient client) {
        Set<WebSocketClient> clients = moduleClients.get(module);
        if (clients != null) {
            clients.remove(client);
        }
    }
    
    static class WebSocketClient implements Runnable {
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private String username;
        private String currentModule;
        private boolean connected = false;
        
        public WebSocketClient(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
                
                if (performHandshake()) {
                    connected = true;
                    System.out.println("WebSocket client connected");
                    
                    while (connected) {
                        String message = readWebSocketFrame();
                        if (message != null) {
                            handleMessage(message);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("WebSocket client disconnected: " + username);
            } finally {
                cleanup();
            }
        }
        
        private boolean performHandshake() throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            String webSocketKey = null;
            
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    webSocketKey = line.substring(18).trim();
                }
            }
            
            if (webSocketKey == null) return false;
            
            String acceptKey = generateAcceptKey(webSocketKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                            "Upgrade: websocket\r\n" +
                            "Connection: Upgrade\r\n" +
                            "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
            
            out.write(response.getBytes());
            out.flush();
            return true;
        }
        
        private String generateAcceptKey(String webSocketKey) throws Exception {
            String combined = webSocketKey + WS_MAGIC_STRING;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        }
        
        private String readWebSocketFrame() throws IOException {
            int firstByte = in.read();
            if (firstByte == -1) return null;
            
            boolean fin = (firstByte & 0x80) != 0;
            int opcode = firstByte & 0x0F;
            
            if (opcode == 8) { // Close frame
                connected = false;
                return null;
            }
            
            int secondByte = in.read();
            if (secondByte == -1) return null;
            
            boolean masked = (secondByte & 0x80) != 0;
            int payloadLength = secondByte & 0x7F;
            
            if (payloadLength == 126) {
                payloadLength = (in.read() << 8) | in.read();
            } else if (payloadLength == 127) {
                // Skip extended payload length for simplicity
                for (int i = 0; i < 8; i++) in.read();
                return null;
            }
            
            byte[] maskingKey = new byte[4];
            if (masked) {
                in.read(maskingKey);
            }
            
            byte[] payload = new byte[payloadLength];
            in.read(payload);
            
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= maskingKey[i % 4];
                }
            }
            
            return new String(payload, StandardCharsets.UTF_8);
        }
        
        private void handleMessage(String message) {
            String[] parts = message.split("\\|", 4);
            String command = parts[0];
            
            switch (command) {
                case "JOIN":
                    if (parts.length >= 3) {
                        username = parts[1];
                        currentModule = parts[2];
                        addClientToModule(currentModule, this);
                        
                        // Send chat history immediately
                        List<String> history = ChatStorage.getMessages(currentModule);
                        System.out.println("📜 Loading " + history.size() + " messages for " + username + " in " + currentModule);
                        for (String msg : history) {
                            System.out.println("📤 Sending history: " + msg);
                            sendMessage("MESSAGE|" + msg);
                        }
                        
                        System.out.println(username + " joined " + currentModule + " (sent " + history.size() + " messages)");
                    }
                    break;
                    
                case "MESSAGE":
                    if (parts.length >= 4) {
                        String user = parts[1];
                        String module = parts[2];
                        String msg = parts[3];
                        String timestamp = java.time.LocalTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                        
                        // Save message to persistent storage
                        ChatStorage.saveMessage(module, user, msg, timestamp);
                        
                        String broadcastMsg = String.format("MESSAGE|%s|%s|%s|%s", 
                            user, module, msg, timestamp);
                        broadcastToModule(module, broadcastMsg, this);
                        System.out.println("[" + module + "] " + user + ": " + msg);
                    }
                    break;
            }
        }
        
        public void sendMessage(String message) {
            if (!connected) return;
            
            try {
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                int length = messageBytes.length;
                
                // Simple frame format
                out.write(0x81); // Text frame, FIN=1
                
                if (length < 126) {
                    out.write(length);
                } else if (length < 65536) {
                    out.write(126);
                    out.write((length >> 8) & 0xFF);
                    out.write(length & 0xFF);
                }
                
                out.write(messageBytes);
                out.flush();
            } catch (IOException e) {
                connected = false;
            }
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        private void cleanup() {
            try {
                if (currentModule != null) {
                    removeClientFromModule(currentModule, this);
                }
                allClients.remove(this);
                connected = false;
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}