package com.unishare.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unishare.model.FileInfo;
import com.unishare.model.User;
import com.unishare.service.AuthService;
import com.unishare.service.FileService;
import com.unishare.service.DownloadManager;
import com.unishare.service.ModuleService;
import com.unishare.service.MonitoringService;
import com.unishare.service.NotificationService;
import com.unishare.util.CORSFilter;
import java.io.*;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for handling file upload, download, and management
 */
public class FileController implements HttpHandler {

    private final FileService fileService;
    private final AuthService authService;
    private final DownloadManager downloadManager;
    private final NotificationService notificationService;
    private final ModuleService moduleService;
    private final MonitoringService monitoringService;

    public FileController(FileService fileService, AuthService authService, DownloadManager downloadManager) {
        this(fileService, authService, downloadManager, null, null, null);
    }

    public FileController(FileService fileService,
            AuthService authService,
            DownloadManager downloadManager,
            NotificationService notificationService,
            ModuleService moduleService,
            MonitoringService monitoringService) {
        this.fileService = fileService;
        this.authService = authService;
        this.downloadManager = downloadManager;
        this.notificationService = notificationService;
        this.moduleService = moduleService;
        this.monitoringService = monitoringService;
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
                    } else if (path.matches("/api/download-cancel/[^/]+")) {
                        handleDownloadCancel(exchange);
                    }
                    break;
                case "GET":
                    if (path.matches("/api/download/[^/]+")) {
                        handleDownload(exchange);
                    } else if (path.matches("/api/download-status/[^/]+")) {
                        handleDownloadStatus(exchange);
                    } else if (path.matches("/api/download-file/[^/]+")) {
                        handleDownloadFile(exchange);
                    } else if (path.equals("/api/download-stats")) {
                        handleDownloadStats(exchange);
                    } else if (path.startsWith("/api/files/") && path.endsWith("/download")) {
                        handleFileDownloadLink(exchange);
                    } else {
                        sendErrorResponse(exchange, 404, "Not Found");
                    }
                    break;
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
            if (!uploadedFiles.isEmpty() && notificationService != null) {
                String moduleName = module;
                if (moduleService != null) {
                    try {
                        com.unishare.model.ModuleInfo moduleInfo = moduleService.findByCode(module);
                        if (moduleInfo != null && moduleInfo.getName() != null) {
                            moduleName = moduleInfo.getName();
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to load module name: " + e.getMessage());
                    }
                }

                for (FileInfo file : uploadedFiles) {
                    try {
                        notificationService.notifyFileUpload(
                                module,
                                moduleName,
                                file.getFilename(),
                                uploaderLabel,
                                user.get().getId());
                    } catch (Exception notifyError) {
                        System.err.println("⚠️ Failed to notify subscribers: " + notifyError.getMessage());
                    }
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

    /**
     * Handle direct download link requests (legacy flow)
     * URL pattern: /api/files/{fileId}/download
     */
    private void handleFileDownloadLink(HttpExchange exchange) throws IOException {
        try {
            Optional<User> user = authService.findBySessionToken(extractToken(exchange));
            if (user.isEmpty()) {
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            final String prefix = "/api/files/";
            final String suffix = "/download";

            if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
                sendErrorResponse(exchange, 404, "Not Found");
                return;
            }

            String idPart = path.substring(prefix.length(), path.length() - suffix.length());
            if (idPart.isBlank()) {
                sendErrorResponse(exchange, 400, "Missing file id");
                return;
            }

            java.util.UUID fileId;
            try {
                fileId = java.util.UUID.fromString(idPart);
            } catch (IllegalArgumentException ex) {
                sendErrorResponse(exchange, 400, "Invalid file id");
                return;
            }

            Optional<FileInfo> fileInfoOptional = fileService.findById(fileId);
            if (fileInfoOptional.isEmpty()) {
                sendErrorResponse(exchange, 404, "File not found");
                return;
            }

            FileInfo fileInfo = fileInfoOptional.get();

            if (monitoringService != null) {
                try {
                    monitoringService.recordFileDownload(fileInfo.getId(), user.get().getId(), fileInfo.getModule());
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to record download event: " + e.getMessage());
                }
            }

            String response = "{\"success\":true," +
                    "\"downloadUrl\":\"" + escapeJson(fileInfo.getSecureUrl()) + "\"," +
                    "\"filename\":\"" + escapeJson(fileInfo.getFilename()) + "\"," +
                    "\"module\":\"" + escapeJson(fileInfo.getModule()) + "\"," +
                    "\"fileSize\":" + fileInfo.getFileSize() +
                    "}";

            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            System.err.println("❌ Direct download link failed: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to prepare download link");
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

    /**
     * Handle file download requests through the download manager
     * URL pattern: /api/download/{fileId}
     */
    /**
     * Handle download requests through the download manager
     * URL pattern: /api/download/{fileId}
     */
    private void handleDownload(HttpExchange exchange) throws IOException {
        try {
            if (downloadManager == null) {
                sendErrorResponse(exchange, 503, "Download manager unavailable");
                return;
            }

            Optional<User> user = authService.findBySessionToken(extractToken(exchange));
            if (user.isEmpty()) {
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String fileIdStr = path.substring("/api/download/".length());
            
            UUID fileId;
            try {
                fileId = UUID.fromString(fileIdStr);
            } catch (IllegalArgumentException ex) {
                sendErrorResponse(exchange, 400, "Invalid file ID");
                return;
            }

            // Check if file exists
            Optional<FileInfo> fileInfo = fileService.findById(fileId);
            if (fileInfo.isEmpty()) {
                sendErrorResponse(exchange, 404, "File not found");
                return;
            }

            // Initiate download through the download manager
            String sessionId = downloadManager.initiateDownload(fileId, user.get().getEmail());
            
            // Always return session ID for progress tracking (managed downloads only)
            String response = String.format(
                "{\"sessionId\":\"%s\",\"filename\":\"%s\",\"status\":\"initiated\"}",
                sessionId, fileInfo.get().getFilename()
            );
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
            
        } catch (Exception e) {
            System.err.println("❌ Download request failed: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Download request failed");
        }
    }

    /**
     * Handle download status checking
     * URL pattern: /api/download-status/{sessionId}
     */
    private void handleDownloadStatus(HttpExchange exchange) throws IOException {
        try {
            if (downloadManager == null) {
                sendErrorResponse(exchange, 503, "Download manager unavailable");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String sessionId = path.substring("/api/download-status/".length());
            
            Optional<DownloadManager.DownloadSession> session = downloadManager.getDownloadStatus(sessionId);
            
            String response;
            if (session.isPresent()) {
                DownloadManager.DownloadSession s = session.get();
                String status = s.getStatus().toString().toLowerCase();
                response = String.format(
                    "{\"sessionId\":\"%s\",\"status\":\"%s\",\"progress\":%d,\"bytesDownloaded\":%d,\"totalBytes\":%d,\"elapsedTime\":%d,\"error\":\"%s\"}",
                    s.getSessionId(),
                    status,
                    s.getProgress(),
                    s.getBytesDownloaded(),
                    s.getTotalBytes(),
                    s.getElapsedTime(),
                    s.getErrorMessage() != null ? s.getErrorMessage() : ""
                );
            } else {
                response = "{\"error\":\"Session not found\"}";
            }
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
            
        } catch (Exception e) {
            System.err.println("❌ Download status check failed: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Status check failed");
        }
    }

    /**
     * Handle download statistics
     * URL pattern: /api/download-stats
     */
    private void handleDownloadStats(HttpExchange exchange) throws IOException {
        try {
            if (downloadManager == null) {
                sendErrorResponse(exchange, 503, "Download manager unavailable");
                return;
            }

            DownloadManager.DownloadStats stats = downloadManager.getStatistics();
            String response = stats.toJson();
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
            
        } catch (Exception e) {
            System.err.println("❌ Download stats failed: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Stats retrieval failed");
        }
    }

    /**
     * Handle downloading completed file content
     * URL pattern: /api/download-file/{sessionId}
     */
    private void handleDownloadFile(HttpExchange exchange) throws IOException {
        try {
            if (downloadManager == null) {
                sendErrorResponse(exchange, 503, "Download manager unavailable");
                return;
            }

            Optional<User> user = authService.findBySessionToken(extractToken(exchange));
            if (user.isEmpty()) {
                sendErrorResponse(exchange, 401, "Authentication required");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String sessionId = path.substring("/api/download-file/".length());
            
            // Get download session
            Optional<DownloadManager.DownloadSession> sessionOpt = downloadManager.getDownloadStatus(sessionId);
            if (sessionOpt.isEmpty()) {
                sendErrorResponse(exchange, 404, "Download session not found");
                return;
            }
            
            DownloadManager.DownloadSession session = sessionOpt.get();
            
            if (session.getStatus() != DownloadManager.DownloadStatus.COMPLETED) {
                sendErrorResponse(exchange, 400, "Download not completed yet");
                return;
            }
            
            byte[] fileContent = session.getFileContent();
            if (fileContent == null) {
                sendErrorResponse(exchange, 500, "File content not available");
                return;
            }
            
            FileInfo fileInfo = session.getFileInfo();
            if (fileInfo == null) {
                sendErrorResponse(exchange, 500, "File information not available");
                return;
            }
            
            // Set appropriate headers for file download
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", getContentType(fileInfo.getFilename()));
            exchange.getResponseHeaders().set("Content-Disposition", 
                "attachment; filename=\"" + fileInfo.getFilename() + "\"");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileContent.length));
            
            // Stream the file content
            exchange.sendResponseHeaders(200, fileContent.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(fileContent);
            }
            
            if (downloadManager != null) {
                downloadManager.clearSession(sessionId);
            }
            
            System.out.println("✅ File served: " + fileInfo.getFilename() + " (" + fileContent.length + " bytes)");
            
        } catch (Exception e) {
            System.err.println("❌ Download file serving failed: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "File serving failed");
        }
    }

    /**
     * Get appropriate content type for file
     */
    private String getContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt":
                return "text/plain";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Handle download cancellation
     * URL pattern: /api/download-cancel/{sessionId}
     */
    private void handleDownloadCancel(HttpExchange exchange) throws IOException {
        try {
            if (downloadManager == null) {
                sendErrorResponse(exchange, 503, "Download manager unavailable");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String sessionId = path.substring("/api/download-cancel/".length());
            
            boolean cancelled = downloadManager.cancelDownload(sessionId);
            
            String response = String.format(
                "{\"sessionId\":\"%s\",\"cancelled\":%b}",
                sessionId, cancelled
            );
            
            CORSFilter.addCORSHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
            
        } catch (Exception e) {
            System.err.println("❌ Download cancellation failed: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Cancellation failed");
        }
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

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
