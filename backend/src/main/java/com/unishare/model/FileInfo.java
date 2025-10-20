package com.unishare.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model class representing file information
 */
public class FileInfo {
    
    private String filename;
    private String module;
    private String uploaderName;
    private LocalDateTime uploadDate;
    private long fileSize;
    private String filePath;
    
    // Constructors
    public FileInfo() {}
    
    public FileInfo(String filename, String module, String uploaderName) {
        this.filename = filename;
        this.module = module;
        this.uploaderName = uploaderName;
        this.uploadDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getModule() {
        return module;
    }
    
    public void setModule(String module) {
        this.module = module;
    }
    
    public String getUploaderName() {
        return uploaderName;
    }
    
    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }
    
    public LocalDateTime getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    // Utility methods
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    public String getFormattedUploadDate() {
        if (uploadDate != null) {
            return uploadDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        return "Unknown";
    }
    
    public String getFileExtension() {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";
    }
    
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"filename\":\"").append(escapeJson(filename)).append("\",");
        json.append("\"module\":\"").append(escapeJson(module)).append("\",");
        json.append("\"uploaderName\":\"").append(escapeJson(uploaderName)).append("\",");
        json.append("\"uploadDate\":\"").append(getFormattedUploadDate()).append("\",");
        json.append("\"fileSize\":").append(fileSize).append(",");
        json.append("\"formattedFileSize\":\"").append(getFormattedFileSize()).append("\",");
        json.append("\"fileExtension\":\"").append(getFileExtension()).append("\"");
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    @Override
    public String toString() {
        return "FileInfo{" +
                "filename='" + filename + '\'' +
                ", module='" + module + '\'' +
                ", uploaderName='" + uploaderName + '\'' +
                ", uploadDate=" + uploadDate +
                ", fileSize=" + fileSize +
                '}';
    }
}
