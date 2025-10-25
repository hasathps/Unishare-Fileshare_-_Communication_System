package com.unishare.controller;

import com.unishare.service.FileService;
import com.unishare.model.FileInfo;
import com.unishare.util.CORSFilter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for handling file upload, download, and management
 */
public class FileController implements HttpHandler {
    
    private final FileService fileService;
    
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            switch (method) {
                case "POST":
                    if (path.equals("/api/upload")) {
                        handleUpload(exchange);
                    }
                    break;
                case "OPTIONS":
                    CORSFilter.handlePreflightRequest(exchange);
                    break;
                default:
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }
    
    public void handleUpload(HttpExchange exchange) throws IOException {
        System.out.println("📤 File upload request received");
        
        try {
            // Read the request body once
            InputStream inputStream = exchange.getRequestBody();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            
            byte[] requestBody = buffer.toByteArray();
            
            // Parse multipart form data
            Map<String, String> formData = parseMultipartFormData(requestBody);
            
            String module = formData.get("module");
            String uploaderName = formData.get("uploaderName");
            
            System.out.println("📝 Parsed form data: module=" + module + ", uploaderName=" + uploaderName);
            
            if (module == null || uploaderName == null) {
                System.err.println("❌ Missing module or uploader name");
                sendErrorResponse(exchange, 400, "Missing module or uploader name");
                return;
            }
            
            // Get uploaded files
            List<FileInfo> uploadedFiles = fileService.saveUploadedFiles(exchange, module, uploaderName, requestBody);
            
            // Send success response
            String response = String.format(
                "{\"success\":true,\"message\":\"Files uploaded successfully\",\"files\":%d,\"module\":\"%s\"}",
                uploadedFiles.size(), module
            );
            
            System.out.println("📤 Sending response: " + response);
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
            
            System.out.println("✅ Upload successful: " + uploadedFiles.size() + " files to module " + module);
            
        } catch (Exception e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Upload failed: " + e.getMessage());
        }
    }
    
    
    private Map<String, String> parseMultipartFormData(byte[] requestBody) throws IOException {
        Map<String, String> formData = new HashMap<>();
        
        String body = new String(requestBody, "UTF-8");
        
        System.out.println("📝 Raw request body length: " + requestBody.length + " bytes");
        System.out.println("📝 First 200 chars of body: " + body.substring(0, Math.min(200, body.length())));
        
        // Find the boundary dynamically
        String boundary = null;
        String[] lines = body.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("------WebKitFormBoundary")) {
                boundary = line;
                break;
            }
        }
        
        if (boundary == null) {
            System.err.println("❌ No boundary found in multipart data");
            formData.put("module", "IN3111");
            formData.put("uploaderName", "Anonymous");
            return formData;
        }
        
        System.out.println("📝 Found boundary: " + boundary);
        
        // Split by boundary
        String[] parts = body.split(boundary);
        
        for (String part : parts) {
            System.out.println("📝 Processing part: " + part.substring(0, Math.min(100, part.length())) + "...");
            
            if (part.contains("name=\"module\"")) {
                // Extract module value
                int startIndex = part.indexOf("\r\n\r\n") + 4;
                int endIndex = part.indexOf("\r\n", startIndex);
                if (endIndex == -1) endIndex = part.length();
                if (startIndex < endIndex) {
                    String module = part.substring(startIndex, endIndex).trim();
                    formData.put("module", module);
                    System.out.println("📝 Found module: " + module);
                }
            }
            
            if (part.contains("name=\"uploaderName\"")) {
                // Extract uploaderName value
                int startIndex = part.indexOf("\r\n\r\n") + 4;
                int endIndex = part.indexOf("\r\n", startIndex);
                if (endIndex == -1) endIndex = part.length();
                if (startIndex < endIndex) {
                    String uploaderName = part.substring(startIndex, endIndex).trim();
                    formData.put("uploaderName", uploaderName);
                    System.out.println("📝 Found uploaderName: " + uploaderName);
                }
            }
        }
        
        // Fallback to defaults if not found
        if (!formData.containsKey("module")) {
            formData.put("module", "IN3111");
            System.out.println("⚠️ Module not found, using default: IN3111");
        }
        if (!formData.containsKey("uploaderName")) {
            formData.put("uploaderName", "Anonymous");
            System.out.println("⚠️ UploaderName not found, using default: Anonymous");
        }
        
        System.out.println("📝 Final parsed form data: " + formData);
        return formData;
    }
    
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    try {
                        params.put(URLDecoder.decode(keyValue[0], "UTF-8"),
                                  URLDecoder.decode(keyValue[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        // Handle error
                    }
                }
            }
        }
        return params;
    }
    
    private void sendErrorResponse(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        System.err.println("❌ Sending error response: " + response);
        CORSFilter.addCORSHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.getResponseBody().close();
    }
}
