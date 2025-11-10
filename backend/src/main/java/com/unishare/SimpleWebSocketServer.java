package com.unishare;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleWebSocketServer {
    private static final int PORT = 8084;
    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final String CHAT_DIR = "chat_data";
    
    private static Map<String, List<String>> moduleMessages = new ConcurrentHashMap<>();
    private static Map<String, Set<WebSocketConnection>> moduleClients = new ConcurrentHashMap<>();
    
    static {
        loadMessages();
    }
    
    public static void main(String[] args) {
        System.out.println("🌐 Starting WebSocket Chat Server on port " + PORT);
        
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                new Thread(new WebSocketConnection(client)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private static void loadMessages() {
        try {
            Files.createDirectories(Paths.get(CHAT_DIR));
            Files.list(Paths.get(CHAT_DIR))
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(path -> {
                    String module = path.getFileName().toString().replace(".txt", "");
                    List<String> messages = new ArrayList<>();
                    try (BufferedReader reader = Files.newBufferedReader(path)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            messages.add(line);
                        }
                    } catch (IOException e) {
                        System.err.println("Error loading " + module);
                    }
                    moduleMessages.put(module, messages);
                    System.out.println("📚 Loaded " + messages.size() + " messages for " + module);
                });
        } catch (IOException e) {
            System.err.println("Error creating chat directory");
        }
    }
    
    private static void saveMessage(String module, String message) {
        moduleMessages.computeIfAbsent(module, k -> new ArrayList<>()).add(message);
        try {
            Path filePath = Paths.get(CHAT_DIR, module + ".txt");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                for (String msg : moduleMessages.get(module)) {
                    writer.println(msg);
                }
            }
            System.out.println("💾 Saved message to " + module);
        } catch (IOException e) {
            System.err.println("Error saving message");
        }
    }
    
    public static void broadcast(String module, String message, WebSocketConnection sender) {
        Set<WebSocketConnection> clients = moduleClients.get(module);
        if (clients != null) {
            clients.removeIf(client -> {
                if (client != sender && client.isConnected()) {
                    return !client.send(message);
                }
                return !client.isConnected();
            });
        }
    }
    
    static class WebSocketConnection implements Runnable {
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private boolean connected = false;
        private String currentModule;
        
        public WebSocketConnection(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
                
                if (performHandshake()) {
                    connected = true;
                    System.out.println("✅ WebSocket client connected");
                    
                    while (connected) {
                        String message = readFrame();
                        if (message != null) {
                            handleMessage(message);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Client disconnected");
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
            String combined = webSocketKey + WS_MAGIC;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        }
        
        private String readFrame() throws IOException {
            int firstByte = in.read();
            if (firstByte == -1) return null;
            
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
            String[] parts = message.split("\\|");
            String command = parts[0];
            
            if ("JOIN".equals(command) && parts.length >= 3) {
                String username = parts[1];
                currentModule = parts[2];
                
                moduleClients.computeIfAbsent(currentModule, k -> ConcurrentHashMap.newKeySet()).add(this);
                
                // Send chat history
                List<String> history = moduleMessages.getOrDefault(currentModule, new ArrayList<>());
                for (String msg : history) {
                    send("MESSAGE|" + msg);
                }
                
                System.out.println("👤 " + username + " joined " + currentModule + " (sent " + history.size() + " messages)");
                
            } else if ("MESSAGE".equals(command) && parts.length >= 4) {
                String user = parts[1];
                String module = parts[2];
                String msg = parts[3];
                String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                
                String fullMessage = user + "|" + module + "|" + msg + "|" + timestamp;
                saveMessage(module, fullMessage);
                
                String broadcastMsg = "MESSAGE|" + fullMessage;
                broadcast(module, broadcastMsg, this);
                
                System.out.println("💬 [" + module + "] " + user + ": " + msg);
            }
        }
        
        public boolean send(String message) {
            if (!connected) return false;
            
            try {
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                int length = messageBytes.length;
                
                out.write(0x81); // Text frame
                
                if (length < 126) {
                    out.write(length);
                } else {
                    out.write(126);
                    out.write((length >> 8) & 0xFF);
                    out.write(length & 0xFF);
                }
                
                out.write(messageBytes);
                out.flush();
                return true;
            } catch (IOException e) {
                connected = false;
                return false;
            }
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        private void cleanup() {
            try {
                if (currentModule != null) {
                    Set<WebSocketConnection> clients = moduleClients.get(currentModule);
                    if (clients != null) {
                        clients.remove(this);
                    }
                }
                connected = false;
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}