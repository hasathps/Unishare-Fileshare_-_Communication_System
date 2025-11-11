package com.unishare.service;

import com.unishare.model.ModuleInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling module-related operations.
 * Fetches modules from database.
 */
public class ModuleService {

    private final DatabaseService databaseService;

    public ModuleService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public List<ModuleInfo> getModules() {
        List<ModuleInfo> modules = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT code, name, description FROM modules ORDER BY name ASC");
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String code = rs.getString("code");
                String name = rs.getString("name");
                String description = rs.getString("description");
                modules.add(new ModuleInfo(code, name, description));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch modules: " + e.getMessage());
        }
        return modules;
    }

    public ModuleInfo findByCode(String code) {
        if (code == null)
            return null;

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT code, name, description FROM modules WHERE code = ?")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ModuleInfo(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find module: " + e.getMessage());
        }
        return null;
    }

    public boolean isValidModule(String code) {
        return findByCode(code) != null;
    }

    public String getModuleDescription(String code) {
        ModuleInfo info = findByCode(code);
        return info != null ? info.getDescription() : "Unknown Module";
    }

    /**
     * Convenience: return just module codes (used by legacy callers).
     */
    public List<String> getAvailableModules() {
        return getModules().stream()
                .map(ModuleInfo::getCode)
                .collect(Collectors.toList());
    }
}
