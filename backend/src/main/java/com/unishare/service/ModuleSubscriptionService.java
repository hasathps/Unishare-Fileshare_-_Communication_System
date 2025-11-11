package com.unishare.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling module subscription operations.
 */
public class ModuleSubscriptionService {

    private final DatabaseService databaseService;

    public ModuleSubscriptionService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Subscribe a user to a module.
     * 
     * @return "SUCCESS" if subscribed, "ALREADY_SUBSCRIBED" if already exists,
     *         "ERROR" on failure
     */
    public String subscribe(UUID userId, String moduleCode) {
        try (Connection conn = databaseService.getConnection()) {
            // Check if subscription already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT is_active FROM module_subscriptions WHERE user_id = ? AND module_code = ?")) {
                checkStmt.setObject(1, userId);
                checkStmt.setString(2, moduleCode);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        boolean isActive = rs.getBoolean("is_active");
                        if (isActive) {
                            return "ALREADY_SUBSCRIBED";
                        } else {
                            // Re-activate existing subscription
                            try (PreparedStatement updateStmt = conn.prepareStatement(
                                    "UPDATE module_subscriptions SET is_active = TRUE, subscribed_at = NOW() WHERE user_id = ? AND module_code = ?")) {
                                updateStmt.setObject(1, userId);
                                updateStmt.setString(2, moduleCode);
                                updateStmt.executeUpdate();
                                return "SUCCESS";
                            }
                        }
                    }
                }
            }

            // Insert new subscription
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO module_subscriptions (user_id, module_code, subscribed_at, is_active) VALUES (?, ?, NOW(), TRUE)")) {
                insertStmt.setObject(1, userId);
                insertStmt.setString(2, moduleCode);
                insertStmt.executeUpdate();
                return "SUCCESS";
            }

        } catch (SQLException e) {
            System.err.println("Failed to subscribe user: " + e.getMessage());
            // Check for unique constraint violation
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                return "ALREADY_SUBSCRIBED";
            }
            return "ERROR";
        }
    }

    /**
     * Unsubscribe a user from a module.
     * 
     * @return "SUCCESS" if unsubscribed, "NOT_SUBSCRIBED" if not found, "ERROR" on
     *         failure
     */
    public String unsubscribe(UUID userId, String moduleCode) {
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE module_subscriptions SET is_active = FALSE WHERE user_id = ? AND module_code = ? AND is_active = TRUE")) {

            stmt.setObject(1, userId);
            stmt.setString(2, moduleCode);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                return "SUCCESS";
            } else {
                return "NOT_SUBSCRIBED";
            }

        } catch (SQLException e) {
            System.err.println("Failed to unsubscribe user: " + e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Check if a user is subscribed to a module.
     */
    public boolean isSubscribed(UUID userId, String moduleCode) {
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT 1 FROM module_subscriptions WHERE user_id = ? AND module_code = ? AND is_active = TRUE")) {

            stmt.setObject(1, userId);
            stmt.setString(2, moduleCode);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("Failed to check subscription: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all active subscribers for a module (for notifications).
     * 
     * @return List of user IDs
     */
    public List<UUID> getSubscribers(String moduleCode) {
        List<UUID> subscribers = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT user_id FROM module_subscriptions WHERE module_code = ? AND is_active = TRUE")) {

            stmt.setString(1, moduleCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subscribers.add((UUID) rs.getObject("user_id"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Failed to get subscribers: " + e.getMessage());
        }

        return subscribers;
    }

    /**
     * Get all modules a user is subscribed to.
     */
    public List<String> getUserSubscriptions(UUID userId) {
        List<String> modules = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT module_code FROM module_subscriptions WHERE user_id = ? AND is_active = TRUE")) {

            stmt.setObject(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modules.add(rs.getString("module_code"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Failed to get user subscriptions: " + e.getMessage());
        }

        return modules;
    }
}
