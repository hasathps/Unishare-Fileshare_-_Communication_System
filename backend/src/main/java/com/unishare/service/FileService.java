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
    
    public List<FileInfo> saveUploadedFiles(HttpExchange exchange, String module, String uploaderName, byte[] requestBody) 
            throws IOException {
        
        List<FileInfo> uploadedFiles = new ArrayList<>();
        
        // Create module directory if it doesn't exist
        Path modulePath = Paths.get(UPLOAD_DIR, module);
        Files.createDirectories(modulePath);
        
        System.out.println("📤 Received upload request for module: " + module + " by: " + uploaderName);
        System.out.println("📊 Request body size: " + requestBody.length + " bytes");
        
        // Parse multipart data to extract actual files
        List<UploadedFile> parsedFiles = parseMultipartData(requestBody);
        
        if (parsedFiles.isEmpty()) {
            System.err.println("❌ No files could be parsed from multipart data");
            return uploadedFiles;
        }
        
        // Save each parsed file
        for (UploadedFile uploadedFile : parsedFiles) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String originalFilename = uploadedFile.filename;
            
            // Clean uploader name (replace spaces and special characters with underscores)
            String cleanUploaderName = uploaderName.replaceAll("[^a-zA-Z0-9]", "_")
                                                  .replaceAll("_+", "_")
                                                  .replaceAll("^_|_$", "");
            
            // Create new filename: {module}_{uploaderName}_{originalFilename}
            String filename;
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot > 0) {
                String nameWithoutExt = originalFilename.substring(0, lastDot);
                String extension = originalFilename.substring(lastDot);
                filename = module + "_" + cleanUploaderName + "_" + nameWithoutExt + "_" + timestamp + extension;
            } else {
                filename = module + "_" + cleanUploaderName + "_" + originalFilename + "_" + timestamp;
            }
            
            Path filePath = modulePath.resolve(filename);
            
            // Save the actual file content
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(uploadedFile.content);
            }
            
            // Create FileInfo object
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilename(filename);
            fileInfo.setModule(module);
            fileInfo.setUploaderName(uploaderName);
            fileInfo.setUploadDate(LocalDateTime.now());
            fileInfo.setFileSize(uploadedFile.content.length);
            fileInfo.setFilePath(filePath.toString());
            
            uploadedFiles.add(fileInfo);
            
            System.out.println("✅ File saved successfully: " + filename + " (" + uploadedFile.content.length + " bytes) in module " + module);
        }
        
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
        
        // Convert to string to find boundaries and filename
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
                
                // Extract the actual file content
                byte[] fileContent = extractFileContentFromMultipart(data, body, boundary);
                if (fileContent != null && fileContent.length > 0) {
                    files.add(new UploadedFile(originalFilename, fileContent));
                    System.out.println("💾 Extracted file: " + originalFilename + " (" + fileContent.length + " bytes)");
                } else {
                    System.out.println("❌ Could not extract file content");
                }
            }
        }
        
        return files;
    }
    
    private byte[] extractFileContentFromMultipart(byte[] data, String body, String boundary) {
        try {
            // Find the file section in the multipart data
            String fileSectionStart = "Content-Type: application/pdf\r\n\r\n";
            int fileStartIndex = body.indexOf(fileSectionStart);
            
            if (fileStartIndex == -1) {
                // Try other content types
                String[] contentTypes = {
                    "Content-Type: application/pdf\r\n\r\n",
                    "Content-Type: application/msword\r\n\r\n", 
                    "Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document\r\n\r\n",
                    "Content-Type: text/plain\r\n\r\n",
                    "Content-Type: image/png\r\n\r\n",
                    "Content-Type: image/jpeg\r\n\r\n"
                };
                
                for (String contentType : contentTypes) {
                    fileStartIndex = body.indexOf(contentType);
                    if (fileStartIndex != -1) {
                        fileSectionStart = contentType;
                        break;
                    }
                }
            }
            
            if (fileStartIndex == -1) {
                System.err.println("❌ Could not find file content start");
                return null;
            }
            
            // Calculate byte position for content start
            int contentStartBytePos = body.substring(0, fileStartIndex + fileSectionStart.length()).getBytes("UTF-8").length;
            
            // Find the end of the file content (next boundary)
            int contentEndIndex = body.indexOf("\r\n" + boundary, fileStartIndex);
            if (contentEndIndex == -1) {
                contentEndIndex = body.length();
            }
            
            // Calculate byte position for content end
            int contentEndBytePos = body.substring(0, contentEndIndex).getBytes("UTF-8").length;
            
            // Ensure we don't exceed data bounds
            if (contentStartBytePos >= data.length) {
                System.err.println("❌ Content start position exceeds data length");
                return null;
            }
            
            if (contentEndBytePos > data.length) {
                contentEndBytePos = data.length;
            }
            
            int contentLength = contentEndBytePos - contentStartBytePos;
            if (contentLength <= 0) {
                System.err.println("❌ No content to extract");
                return null;
            }
            
            byte[] content = new byte[contentLength];
            System.arraycopy(data, contentStartBytePos, content, 0, contentLength);
            
            System.out.println("📊 Extracted " + contentLength + " bytes of file content");
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting file content: " + e.getMessage());
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
