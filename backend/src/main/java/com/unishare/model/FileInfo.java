package com.unishare.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Model class representing file information stored in Cloudinary with metadata persisted in the database.
 */
public class FileInfo {

    private UUID id;
    private String filename;
    private String module;
    private String uploaderName;
    private Instant uploadInstant;
    private long fileSize;
    private String storageKey;
    private String secureUrl;

    public FileInfo() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public Instant getUploadInstant() {
        return uploadInstant;
    }

    public void setUploadInstant(Instant uploadInstant) {
        this.uploadInstant = uploadInstant;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public void setSecureUrl(String secureUrl) {
        this.secureUrl = secureUrl;
    }

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
        if (uploadInstant != null) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(uploadInstant);
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
        if (id != null) {
            json.append("\"id\":\"").append(id).append("\",");
        }
        json.append("\"filename\":\"").append(escapeJson(filename)).append("\",");
        json.append("\"module\":\"").append(escapeJson(module)).append("\",");
        json.append("\"uploaderName\":\"").append(escapeJson(uploaderName)).append("\",");
        json.append("\"uploadDate\":\"").append(getFormattedUploadDate()).append("\",");
        json.append("\"fileSize\":").append(fileSize).append(",");
        json.append("\"formattedFileSize\":\"").append(getFormattedFileSize()).append("\",");
        json.append("\"fileExtension\":\"").append(getFileExtension()).append("\"");
        if (secureUrl != null) {
            json.append(",\"secureUrl\":\"").append(escapeJson(secureUrl)).append("\"");
        }
        if (storageKey != null) {
            json.append(",\"storageKey\":\"").append(escapeJson(storageKey)).append("\"");
        }
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
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", module='" + module + '\'' +
                ", uploaderName='" + uploaderName + '\'' +
                ", uploadInstant=" + uploadInstant +
                ", fileSize=" + fileSize +
                ", storageKey='" + storageKey + '\'' +
                '}';
    }
}
