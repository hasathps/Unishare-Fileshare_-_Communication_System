package com.unishare.service;

import com.unishare.model.FileInfo;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service for handling file operations
 */
public class FileService {
    
    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg", "gif"
    );
    
    public FileService() {
        createUploadDirectories();
    }
    
    private void createUploadDirectories() {
        try {
            // Create main upload directory
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Create module directories
            String[] modules = {"IN3111", "CS101", "MATH201"};
            for (String module : modules) {
                Path modulePath = uploadPath.resolve(module);
                if (!Files.exists(modulePath)) {
                    Files.createDirectories(modulePath);
                }
            }
            
            System.out.println("📁 Upload directories created successfully");
            
        } catch (IOException e) {
            System.err.println("❌ Failed to create upload directories: " + e.getMessage());
        }
    }
    
    public List<FileInfo> saveUploadedFiles(HttpExchange exchange, String module, String uploaderName) 
            throws IOException {
        
        List<FileInfo> uploadedFiles = new ArrayList<>();
        
        // Create module directory if it doesn't exist
        Path modulePath = Paths.get(UPLOAD_DIR, module);
        Files.createDirectories(modulePath);
        
        // Read the request body
        InputStream inputStream = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        byte[] bodyBytes = buffer.toByteArray();
        
        System.out.println("📤 Received upload request for module: " + module + " by: " + uploaderName);
        System.out.println("📊 Request body size: " + bodyBytes.length + " bytes");
        
        // For now, let's save the raw request data to see what we're getting
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "raw_upload_" + timestamp + ".bin";
        Path filePath = modulePath.resolve(filename);
        
        // Save the raw request data
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(bodyBytes);
        }
        
        // Also create a text file with the first 1000 characters for debugging
        String bodyText = new String(bodyBytes, "UTF-8");
        String debugFilename = "debug_upload_" + timestamp + ".txt";
        Path debugPath = modulePath.resolve(debugFilename);
        
        try (FileWriter writer = new FileWriter(debugPath.toFile())) {
            writer.write("=== Upload Debug Info ===\n");
            writer.write("Module: " + module + "\n");
            writer.write("Uploader: " + uploaderName + "\n");
            writer.write("Request size: " + bodyBytes.length + " bytes\n");
            writer.write("Timestamp: " + timestamp + "\n\n");
            writer.write("=== First 1000 characters of request ===\n");
            writer.write(bodyText.substring(0, Math.min(1000, bodyText.length())));
            if (bodyText.length() > 1000) {
                writer.write("\n... (truncated)");
            }
        }
        
        // Create FileInfo object
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFilename(filename);
        fileInfo.setModule(module);
        fileInfo.setUploaderName(uploaderName);
        fileInfo.setUploadDate(LocalDateTime.now());
        fileInfo.setFileSize(bodyBytes.length);
        fileInfo.setFilePath(filePath.toString());
        
        uploadedFiles.add(fileInfo);
        
        System.out.println("💾 Raw upload data saved: " + filename + " (" + bodyBytes.length + " bytes) in module " + module);
        System.out.println("🔍 Debug file created: " + debugFilename);
        
        return uploadedFiles;
    }
    
    // Helper class to hold file data
    private static class UploadedFile {
        String filename;
        byte[] content;
        
        UploadedFile(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }
    }
    
    // Simple multipart parser
    private List<UploadedFile> parseMultipartData(byte[] data) throws IOException {
        List<UploadedFile> files = new ArrayList<>();
        
        // Convert to string to find boundaries
        String body = new String(data, "UTF-8");
        
        // Find the boundary
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
            return files;
        }
        
        System.out.println("🔍 Found boundary: " + boundary);
        
        // Find filename in the body
        int filenameIndex = body.indexOf("filename=\"");
        if (filenameIndex != -1) {
            int start = filenameIndex + 10;
            int end = body.indexOf("\"", start);
            if (end > start) {
                String originalFilename = body.substring(start, end);
                System.out.println("📁 Found original filename: " + originalFilename);
                
                // Extract the actual file content using a different approach
                byte[] fileContent = extractRealFileContent(data, body, boundary);
                if (fileContent != null && fileContent.length > 0) {
                    // Use original filename with timestamp
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String extension = "";
                    String nameWithoutExt = originalFilename;
                    
                    int lastDot = originalFilename.lastIndexOf('.');
                    if (lastDot > 0) {
                        extension = originalFilename.substring(lastDot);
                        nameWithoutExt = originalFilename.substring(0, lastDot);
                    }
                    
                    String newFilename = nameWithoutExt + "_" + timestamp + extension;
                    files.add(new UploadedFile(newFilename, fileContent));
                    System.out.println("💾 Saved real file: " + newFilename + " (" + fileContent.length + " bytes)");
                } else {
                    System.out.println("❌ Could not extract file content");
                }
            }
        }
        
        return files;
    }
    
    private byte[] extractRealFileContent(byte[] data, String body, String boundary) {
        try {
            // Find the position where file content starts
            int contentStartMarker = body.indexOf("\r\n\r\n");
            if (contentStartMarker == -1) {
                System.err.println("❌ Could not find content start marker");
                return null;
            }
            
            // Find the position where content ends (next boundary)
            int contentEndMarker = body.indexOf("\r\n" + boundary, contentStartMarker);
            if (contentEndMarker == -1) {
                contentEndMarker = body.length();
            }
            
            // Convert string positions to byte positions more carefully
            String beforeStart = body.substring(0, contentStartMarker + 4); // +4 for \r\n\r\n
            String beforeEnd = body.substring(0, contentEndMarker);
            
            int byteStart = beforeStart.getBytes("UTF-8").length;
            int byteEnd = beforeEnd.getBytes("UTF-8").length;
            
            // Ensure we don't exceed data bounds
            if (byteStart >= data.length) {
                System.err.println("❌ Byte start position exceeds data length");
                return null;
            }
            
            if (byteEnd > data.length) {
                byteEnd = data.length;
            }
            
            int contentLength = byteEnd - byteStart;
            if (contentLength <= 0) {
                System.err.println("❌ No content to extract");
                return null;
            }
            
            byte[] content = new byte[contentLength];
            System.arraycopy(data, byteStart, content, 0, contentLength);
            
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting real file content: " + e.getMessage());
            return null;
        }
    }
    
    private String extractFilename(String part) {
        String[] lines = part.split("\r\n");
        for (String line : lines) {
            if (line.contains("filename=")) {
                System.out.println("🔍 Found filename line: " + line);
                int start = line.indexOf("filename=\"") + 10;
                int end = line.indexOf("\"", start);
                if (start > 9 && end > start) {
                    String filename = line.substring(start, end);
                    System.out.println("📁 Extracted filename: " + filename);
                    return filename;
                }
            }
        }
        System.out.println("❌ No filename found in part");
        return null;
    }
    
    private byte[] extractFileContentSimple(String body, byte[] data, int filenameIndex) {
        try {
            // Find the position after filename in the string
            int afterFilename = body.indexOf("\"", filenameIndex + 10) + 1;
            
            // Look for the start of file content (after headers)
            int contentStart = body.indexOf("\r\n\r\n", afterFilename);
            if (contentStart == -1) {
                System.err.println("❌ Could not find content start");
                return null;
            }
            
            contentStart += 4; // Skip the \r\n\r\n
            
            // Find the end of content (next boundary)
            int contentEnd = body.indexOf("\r\n------WebKitFormBoundary", contentStart);
            if (contentEnd == -1) {
                contentEnd = body.length();
            }
            
            // Use a safer approach - find byte positions directly
            byte[] bodyBytes = body.getBytes("UTF-8");
            
            // Find the actual byte positions by searching in the byte array
            byte[] contentStartBytes = "\r\n\r\n".getBytes("UTF-8");
            byte[] boundaryBytes = "\r\n------WebKitFormBoundary".getBytes("UTF-8");
            
            int byteStart = findByteArray(bodyBytes, contentStartBytes, contentStart - 4);
            if (byteStart == -1) {
                System.err.println("❌ Could not find content start in bytes");
                return null;
            }
            byteStart += contentStartBytes.length;
            
            int byteEnd = findByteArray(bodyBytes, boundaryBytes, byteStart);
            if (byteEnd == -1) {
                byteEnd = data.length;
            }
            
            int contentLength = byteEnd - byteStart;
            if (contentLength <= 0) {
                System.err.println("❌ No content found");
                return null;
            }
            
            byte[] content = new byte[contentLength];
            System.arraycopy(data, byteStart, content, 0, contentLength);
            
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting file content: " + e.getMessage());
            return null;
        }
    }
    
    // Helper method to find byte array in another byte array
    private int findByteArray(byte[] haystack, byte[] needle) {
        return findByteArray(haystack, needle, 0);
    }
    
    private int findByteArray(byte[] haystack, byte[] needle, int startIndex) {
        for (int i = startIndex; i <= haystack.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
    
    public List<FileInfo> getFilesForModule(String module) throws IOException {
        List<FileInfo> files = new ArrayList<>();
        
        if (module == null || module.trim().isEmpty()) {
            return files;
        }
        
        Path modulePath = Paths.get(UPLOAD_DIR, module);
        
        if (!Files.exists(modulePath)) {
            return files;
        }
        
        try (Stream<Path> paths = Files.list(modulePath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         FileInfo fileInfo = new FileInfo();
                         fileInfo.setFilename(path.getFileName().toString());
                         fileInfo.setModule(module);
                         fileInfo.setUploaderName("Unknown"); // Would be stored in metadata
                         fileInfo.setUploadDate(LocalDateTime.now()); // Would be from file metadata
                         fileInfo.setFileSize(Files.size(path));
                         fileInfo.setFilePath(path.toString());
                         
                         files.add(fileInfo);
                     } catch (IOException e) {
                         System.err.println("Error reading file info: " + e.getMessage());
                     }
                 });
        }
        
        return files;
    }
    
    public Path getFilePath(String module, String filename) {
        return Paths.get(UPLOAD_DIR, module, filename);
    }
    
    public boolean isValidFile(String filename, long fileSize) {
        // Check file size
        if (fileSize > MAX_FILE_SIZE) {
            return false;
        }
        
        // Check file extension
        String extension = getFileExtension(filename);
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    public void deleteFile(String module, String filename) throws IOException {
        Path filePath = getFilePath(module, filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            System.out.println("🗑️ File deleted: " + filename + " from module " + module);
        }
    }
    
    public List<String> getAvailableModules() {
        List<String> modules = new ArrayList<>();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        
        if (Files.exists(uploadPath)) {
            try (Stream<Path> paths = Files.list(uploadPath)) {
                paths.filter(Files::isDirectory)
                     .forEach(path -> modules.add(path.getFileName().toString()));
            } catch (IOException e) {
                System.err.println("Error reading modules: " + e.getMessage());
            }
        }
        
        return modules;
    }
}
