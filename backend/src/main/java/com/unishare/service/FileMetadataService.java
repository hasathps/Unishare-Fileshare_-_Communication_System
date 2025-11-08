package com.unishare.service;

import com.unishare.model.FileInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles persistence of file metadata in the database.
 */
public class FileMetadataService {

    private final DatabaseService databaseService;

    public FileMetadataService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public FileInfo saveFileMetadata(String module,
                                     String uploaderEmail,
                                     String originalFilename,
                                     String storageKey,
                                     String secureUrl,
                                     long sizeBytes) throws SQLException {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO files (id, module, uploader_email, filename, storage_key, secure_url, size_bytes, uploaded_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, module);
            statement.setString(3, uploaderEmail);
            statement.setString(4, originalFilename);
            statement.setString(5, storageKey);
            statement.setString(6, secureUrl);
            statement.setLong(7, sizeBytes);
            statement.setTimestamp(8, Timestamp.from(now));
            statement.executeUpdate();
        }

        FileInfo info = new FileInfo();
        info.setId(id);
        info.setModule(module);
        info.setUploaderName(uploaderEmail);
        info.setFilename(originalFilename);
        info.setStorageKey(storageKey);
        info.setSecureUrl(secureUrl);
        info.setFileSize(sizeBytes);
        info.setUploadInstant(now);
        return info;
    }

    public List<FileInfo> getFilesForModule(String module) throws SQLException {
        List<FileInfo> files = new ArrayList<>();

        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, module, uploader_email, filename, storage_key, secure_url, size_bytes, uploaded_at " +
                             "FROM files WHERE module = ? ORDER BY uploaded_at DESC")) {
            statement.setString(1, module);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    files.add(mapRow(rs));
                }
            }
        }

        return files;
    }

    public Optional<FileInfo> findById(UUID id) throws SQLException {
        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, module, uploader_email, filename, storage_key, secure_url, size_bytes, uploaded_at " +
                             "FROM files WHERE id = ?")) {
            statement.setObject(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void deleteById(UUID id) throws SQLException {
        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM files WHERE id = ?")) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
    }

    public List<String> listModules() throws SQLException {
        List<String> modules = new ArrayList<>();
        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT DISTINCT module FROM files ORDER BY module ASC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                modules.add(rs.getString("module"));
            }
        }
        return modules;
    }

    private FileInfo mapRow(ResultSet rs) throws SQLException {
        FileInfo info = new FileInfo();
        info.setId((UUID) rs.getObject("id"));
        info.setModule(rs.getString("module"));
        info.setUploaderName(rs.getString("uploader_email"));
        info.setFilename(rs.getString("filename"));
        info.setStorageKey(rs.getString("storage_key"));
        info.setSecureUrl(rs.getString("secure_url"));
        info.setFileSize(rs.getLong("size_bytes"));
        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        if (uploadedAt != null) {
            info.setUploadInstant(uploadedAt.toInstant());
        }
        return info;
    }
}

