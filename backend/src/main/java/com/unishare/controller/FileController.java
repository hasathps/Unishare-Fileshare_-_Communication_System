package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unishare.model.FileInfo;
import com.unishare.model.User;
import com.unishare.service.AuthService;
import com.unishare.service.FileService;
import com.unishare.service.ModuleService;
import com.unishare.service.NotificationService;
import com.unishare.util.CORSFilter;
import java.io.*;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling file upload, download, and management
 */
public class FileController implements HttpHandler {

    private final FileService fileService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ModuleService moduleService;

    public FileController(FileService fileService, AuthService authService,
            NotificationService notificationService, ModuleService moduleService) {
        this.fileService = fileService;
        this.authService = authService;
        this.notificationService = notificationService;
        this.moduleService = moduleService;
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
                // No GET routes here for now (preview handled client-side via Cloudinary
                // secureUrl)
                case "DELETE":
                    if (path.startsWith("/api/files/")) {
                        handleDelete(exchange);
                    } else {
                        sendErrorResponse(exchange, 404, "Not Found");
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
            Optional<User> user = authService.findBySessionToken(extractToken(exchange));
            if (user.isEmpty()) {
                System.err.println("❌ Unauthorized upload attempt.");
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

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
            String uploaderLabel = formData.get("uploaderName");
            String uploaderEmail = user.get().getEmail();
            if (uploaderLabel == null || uploaderLabel.isBlank()) {
                uploaderLabel = user.get().getDisplayName() != null && !user.get().getDisplayName().isBlank()
                        ? user.get().getDisplayName()
                        : uploaderEmail;
            }

            System.out.println("📝 Parsed form data: module=" + module + ", uploaderName=" + uploaderLabel);

            if (module == null) {
                System.err.println("❌ Missing module or uploader name");
                sendErrorResponse(exchange, 400, "Missing module or uploader name");
                return;
            }

            // Get uploaded files
            List<FileInfo> uploadedFiles = fileService.saveUploadedFiles(exchange, module, uploaderEmail, requestBody);

            // Notify subscribers about the file upload
            if (!uploadedFiles.isEmpty()) {
                com.unishare.model.ModuleInfo moduleInfo = moduleService.findByCode(module);
                String moduleName = (moduleInfo != null) ? moduleInfo.getName() : module;

                for (FileInfo file : uploadedFiles) {
                    notificationService.notifyFileUpload(
                            module,
                            moduleName,
                            file.getFilename(),
                            uploaderLabel,
                            user.get().getId());
                }
            }

            // Send success response
            String filesJson = uploadedFiles.stream()
                    .map(FileInfo::toJson)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            String response = String.format(
                    "{\"success\":true,\"message\":\"Files uploaded successfully\",\"module\":\"%s\",\"files\":[%s]}",
                    module, filesJson);

            System.out.println("📤 Sending response: " + response);

            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();

            System.out.println("✅ Upload successful: " + uploadedFiles.size() + " files to module " + module
                    + " by " + user.get().getEmail());

        } catch (Exception e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Upload failed: " + e.getMessage());
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        try {
            Optional<User> user = authService.findBySessionToken(extractToken(exchange));
            if (user.isEmpty()) {
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/api/files/".length());
            java.util.UUID id;
            try {
                id = java.util.UUID.fromString(idStr);
            } catch (IllegalArgumentException ex) {
                sendErrorResponse(exchange, 400, "Invalid file id");
                return;
            }

            // Check if file exists and user owns it
            Optional<FileInfo> fileInfo = fileService.findById(id);
            if (fileInfo.isEmpty()) {
                sendErrorResponse(exchange, 404, "File not found");
                return;
            }

            // Verify the user owns this file
            if (!fileInfo.get().getUploaderName().equals(user.get().getEmail())) {
                sendErrorResponse(exchange, 403, "You can only delete your own files");
                return;
            }

            try {
                fileService.deleteFile(id);
            } catch (Exception e) {
                System.err.println("Failed to delete file: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Failed to delete file");
                return;
            }

            String response = "{\"success\":true}";
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        } catch (Exception e) {
            System.err.println("❌ Delete failed: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Delete failed");
        }
    }

    private String extractToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String header : cookies) {
            for (HttpCookie cookie : HttpCookie.parse(header)) {
                if (authService.getSessionCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private Map<String, String> parseMultipartFormData(byte[] requestBody) throws IOException {
        Map<String, String> formData = new HashMap<>();

        String body = new String(requestBody, StandardCharsets.ISO_8859_1);

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
                if (endIndex == -1)
                    endIndex = part.length();
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
                if (endIndex == -1)
                    endIndex = part.length();
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
