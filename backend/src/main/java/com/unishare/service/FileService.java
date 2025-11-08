package com.unishare.service;

import com.unishare.util.CloudinaryClient;
import com.unishare.model.FileInfo;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Service for handling file operations via Cloudinary with metadata stored in the database.
 */
public class FileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg", "gif"
    );

    private final CloudinaryClient cloudinaryClient;
    private final FileMetadataService metadataService;

    public FileService(FileMetadataService metadataService) {
        this.cloudinaryClient = CloudinaryClient.getInstance();
        this.metadataService = metadataService;
    }

    public List<FileInfo> saveUploadedFiles(HttpExchange exchange,
                                            String module,
                                            String uploaderEmail,
                                            byte[] requestBody) throws IOException, SQLException {

        System.out.println("📤 Received upload request for module: " + module + " by: " + uploaderEmail);
        System.out.println("📊 Request body size: " + requestBody.length + " bytes");

        List<UploadedFile> parsedFiles = parseMultipartData(requestBody);
        List<FileInfo> results = new ArrayList<>();

        for (UploadedFile uploadedFile : parsedFiles) {
            if (!isValidFile(uploadedFile.filename, uploadedFile.content.length)) {
                System.err.println("❌ Invalid file skipped: " + uploadedFile.filename);
                continue;
            }

            CloudinaryClient.UploadResult uploadResult = cloudinaryClient.uploadRaw(
                    uploadedFile.content,
                    uploadedFile.filename,
                    "unishare/" + module
            );

            FileInfo info = metadataService.saveFileMetadata(
                    module,
                    uploaderEmail,
                    uploadedFile.filename,
                    uploadResult.publicId(),
                    uploadResult.secureUrl(),
                    uploadResult.bytes()
            );

            results.add(info);
            System.out.println("✅ Uploaded to Cloudinary: " + uploadedFile.filename + " -> " + uploadResult.secureUrl());
        }

        return results;
    }

    public List<FileInfo> getFilesForModule(String module) throws SQLException {
        if (module == null || module.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return metadataService.getFilesForModule(module);
    }

    public void deleteFile(UUID fileId) throws SQLException, IOException {
        Optional<FileInfo> info = metadataService.findById(fileId);
        if (info.isEmpty()) {
            return;
        }

        FileInfo fileInfo = info.get();
        cloudinaryClient.deleteRaw(fileInfo.getStorageKey());

        metadataService.deleteById(fileId);
        System.out.println("🗑️ File metadata removed for " + fileInfo.getFilename());
    }

    public List<String> getAvailableModules() {
        try {
            return metadataService.listModules();
        } catch (SQLException e) {
            System.err.println("Error listing modules: " + e.getMessage());
            return List.of();
        }
    }

    public boolean isValidFile(String filename, long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            return false;
        }
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

    private List<UploadedFile> parseMultipartData(byte[] data) throws IOException {
        List<UploadedFile> files = new ArrayList<>();

        String body = new String(data, StandardCharsets.ISO_8859_1);
        String boundary = extractBoundary(body);
        if (boundary == null) {
            return files;
        }

        String[] parts = body.split(boundary);
        for (String part : parts) {
            if (!part.contains("filename=\"")) {
                continue;
            }

            String filename = extractFilename(part);
            if (filename == null) {
                continue;
            }

            byte[] content = extractFileBytes(part, body, data, boundary);
            if (content != null && content.length > 0) {
                files.add(new UploadedFile(filename, content));
            }
        }

        return files;
    }

    private String extractBoundary(String body) {
        String[] lines = body.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("------WebKitFormBoundary")) {
                return line;
            }
        }
        return null;
    }

    private String extractFilename(String part) {
        String[] headers = part.split("\r\n");
        for (String header : headers) {
            if (header.contains("filename=\"")) {
                int start = header.indexOf("filename=\"") + 10;
                int end = header.indexOf("\"", start);
                if (end > start) {
                    return header.substring(start, end);
                }
            }
        }
        return null;
    }

    private byte[] extractFileBytes(String part, String body, byte[] data, String boundary) throws IOException {
        int headerEnd = part.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            return null;
        }

        String headers = part.substring(0, headerEnd + 4);
        int offset = body.indexOf(headers) + headers.getBytes(StandardCharsets.ISO_8859_1).length;

        int boundaryIndex = body.indexOf("\r\n" + boundary, offset);
        if (boundaryIndex == -1) {
            boundaryIndex = data.length;
        }

        int length = boundaryIndex - offset;
        if (length <= 0) {
            return null;
        }

        byte[] content = new byte[length];
        System.arraycopy(data, offset, content, 0, length);
        return content;
    }

    private static class UploadedFile {
        final String filename;
        final byte[] content;

        UploadedFile(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }
    }
}
