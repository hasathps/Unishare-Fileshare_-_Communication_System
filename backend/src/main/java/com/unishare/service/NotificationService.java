package com.unishare.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user notifications
 */
public class NotificationService {

    // Map of userId -> List of notifications
    private final Map<UUID, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private final ModuleSubscriptionService subscriptionService;

    public NotificationService(ModuleSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Notify subscribers when a file is uploaded to a module
     */
    public void notifyFileUpload(String moduleCode, String moduleName, String filename, String uploaderName,
            UUID uploaderId) {
        // Get all subscribers for this module
        List<UUID> subscribers = subscriptionService.getSubscribers(moduleCode);

        // Remove the uploader from the notification list (don't notify yourself)
        subscribers.remove(uploaderId);

        String message = String.format("New file uploaded to %s: %s by %s", moduleName, filename, uploaderName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Create notification for each subscriber
        for (UUID subscriberId : subscribers) {
            Notification notification = new Notification(
                    UUID.randomUUID(),
                    subscriberId,
                    "FILE_UPLOAD",
                    message,
                    moduleCode,
                    moduleName,
                    filename,
                    uploaderName,
                    timestamp,
                    false);

            addNotification(subscriberId, notification);
        }

        System.out.println("📢 Notified " + subscribers.size() + " subscribers about file upload to " + moduleCode);
    }

    /**
     * Add a notification for a user
     */
    private void addNotification(UUID userId, Notification notification) {
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);

        // Keep only last 50 notifications per user
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications.size() > 50) {
            notifications.remove(0);
        }
    }

    /**
     * Get all unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(UUID userId) {
        List<Notification> notifications = userNotifications.getOrDefault(userId, new ArrayList<>());
        return notifications.stream()
                .filter(n -> !n.isRead())
                .toList();
    }

    /**
     * Get all notifications for a user (read and unread)
     */
    public List<Notification> getAllNotifications(UUID userId) {
        return new ArrayList<>(userNotifications.getOrDefault(userId, new ArrayList<>()));
    }

    /**
     * Mark a notification as read
     */
    public boolean markAsRead(UUID userId, UUID notificationId) {
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications == null) {
            return false;
        }

        for (Notification notification : notifications) {
            if (notification.getId().equals(notificationId)) {
                notification.setRead(true);
                return true;
            }
        }

        return false;
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = userNotifications.get(userId);
        if (notifications != null) {
            notifications.forEach(n -> n.setRead(true));
        }
    }

    /**
     * Clear all notifications for a user
     */
    public void clearNotifications(UUID userId) {
        userNotifications.remove(userId);
    }

    /**
     * Get count of unread notifications
     */
    public int getUnreadCount(UUID userId) {
        return (int) userNotifications.getOrDefault(userId, new ArrayList<>())
                .stream()
                .filter(n -> !n.isRead())
                .count();
    }

    /**
     * Notification model class
     */
    public static class Notification {
        private final UUID id;
        private final UUID userId;
        private final String type;
        private final String message;
        private final String moduleCode;
        private final String moduleName;
        private final String filename;
        private final String uploaderName;
        private final String timestamp;
        private boolean isRead;

        public Notification(UUID id, UUID userId, String type, String message,
                String moduleCode, String moduleName, String filename,
                String uploaderName, String timestamp, boolean isRead) {
            this.id = id;
            this.userId = userId;
            this.type = type;
            this.message = message;
            this.moduleCode = moduleCode;
            this.moduleName = moduleName;
            this.filename = filename;
            this.uploaderName = uploaderName;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }

        public UUID getId() {
            return id;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getFilename() {
            return filename;
        }

        public String getUploaderName() {
            return uploaderName;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public boolean isRead() {
            return isRead;
        }

        public void setRead(boolean read) {
            this.isRead = read;
        }

        public String toJson() {
            return String.format(
                    "{\"id\":\"%s\",\"userId\":\"%s\",\"type\":\"%s\",\"message\":\"%s\"," +
                            "\"moduleCode\":\"%s\",\"moduleName\":\"%s\",\"filename\":\"%s\"," +
                            "\"uploaderName\":\"%s\",\"timestamp\":\"%s\",\"isRead\":%b}",
                    id, userId, type, escapeJson(message), moduleCode, moduleName,
                    escapeJson(filename), escapeJson(uploaderName), timestamp, isRead);
        }

        private String escapeJson(String str) {
            if (str == null)
                return "";
            return str.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
