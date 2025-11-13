package com.unishare.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Produces monitoring metrics for the UniShare system and records telemetry events.
 */
public class MonitoringService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final DatabaseService databaseService;
    private final Instant serverStartedAt;

    public MonitoringService(DatabaseService databaseService, Instant serverStartedAt) {
        this.databaseService = databaseService;
        this.serverStartedAt = serverStartedAt != null ? serverStartedAt : Instant.now();
    }

    /**
     * Persists a download telemetry event.
     */
    public void recordFileDownload(UUID fileId, UUID userId, String moduleCode) throws SQLException {
        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO file_download_events (file_id, user_id, module_code, downloaded_at) VALUES (?, ?, ?, ?)")) {
            statement.setObject(1, fileId);
            if (userId != null) {
                statement.setObject(2, userId);
            } else {
                statement.setNull(2, java.sql.Types.OTHER);
            }
            if (moduleCode != null) {
                statement.setString(3, moduleCode);
            } else {
                statement.setNull(3, java.sql.Types.VARCHAR);
            }
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    /**
     * Returns a JSON payload with aggregated monitoring metrics.
     */
    public String getDashboardSnapshotJson() throws SQLException {
        try (Connection connection = databaseService.getConnection()) {
            MonitoringSnapshot snapshot = collectSnapshot(connection);
            return snapshot.toJson();
        }
    }

    private MonitoringSnapshot collectSnapshot(Connection connection) throws SQLException {
        MonitoringSnapshot snapshot = new MonitoringSnapshot();

        snapshot.totalFiles = scalarLong(connection, "SELECT COUNT(*) FROM files");
        snapshot.uploadsLast24h = scalarLong(connection,
                "SELECT COUNT(*) FROM files WHERE uploaded_at >= NOW() - INTERVAL '1 day'");
        snapshot.uploadsPrevious24h = scalarLong(connection,
                "SELECT COUNT(*) FROM files WHERE uploaded_at >= NOW() - INTERVAL '2 days' AND uploaded_at < NOW() - INTERVAL '1 day'");
        snapshot.storageBytes = scalarLong(connection, "SELECT COALESCE(SUM(size_bytes), 0) FROM files");
        snapshot.averageFileSizeBytes = scalarDouble(connection, "SELECT COALESCE(AVG(size_bytes), 0) FROM files");

        snapshot.downloadsLast24h = scalarLong(connection,
                "SELECT COUNT(*) FROM file_download_events WHERE downloaded_at >= NOW() - INTERVAL '1 day'");
        snapshot.downloadsPrevious24h = scalarLong(connection,
                "SELECT COUNT(*) FROM file_download_events WHERE downloaded_at >= NOW() - INTERVAL '2 days' AND downloaded_at < NOW() - INTERVAL '1 day'");
        snapshot.totalDownloads = scalarLong(connection,
                "SELECT COUNT(*) FROM file_download_events");

        snapshot.totalUsers = scalarLong(connection, "SELECT COUNT(*) FROM users");
        snapshot.activeUsers24h = scalarLong(connection,
                "SELECT COUNT(DISTINCT user_id) FROM login_events WHERE login_at >= NOW() - INTERVAL '1 day'");
        snapshot.activeUsers7d = scalarLong(connection,
                "SELECT COUNT(DISTINCT user_id) FROM login_events WHERE login_at >= NOW() - INTERVAL '7 days'");

        snapshot.dailyUploads = fillDailySeries(connection,
                "SELECT date_trunc('day', uploaded_at) AS day, COUNT(*) AS uploads " +
                        "FROM files WHERE uploaded_at >= NOW() - INTERVAL '7 days' " +
                        "GROUP BY day ORDER BY day",
                "uploads");

        snapshot.dailyDownloads = fillDailySeries(connection,
                "SELECT date_trunc('day', downloaded_at) AS day, COUNT(*) AS downloads " +
                        "FROM file_download_events WHERE downloaded_at >= NOW() - INTERVAL '7 days' " +
                        "GROUP BY day ORDER BY day",
                "downloads");

        snapshot.topUploaders = fetchTopUploaders(connection);
        snapshot.recentUploads = fetchRecentUploads(connection);
        snapshot.recentDownloads = fetchRecentDownloads(connection);
        snapshot.recentLogins = fetchRecentLogins(connection);
        snapshot.moduleStats = fetchModuleStats(connection);

        snapshot.activityFeed = buildActivityFeed(snapshot.recentUploads, snapshot.recentDownloads, snapshot.recentLogins);

        snapshot.databaseLatencyMs = measureDatabaseLatency();
        snapshot.performance = gatherPerformanceMetrics();
        snapshot.generatedAt = Instant.now();

        return snapshot;
    }

    private long scalarLong(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        }
        return 0L;
    }

    private double scalarDouble(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getDouble(1);
            }
        }
        return 0.0;
    }

    private List<DailyValue> fillDailySeries(Connection connection, String sql, String column) throws SQLException {
        Map<String, Long> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp day = resultSet.getTimestamp("day");
                long count = resultSet.getLong(column);
                Instant instant = day.toInstant();
                String key = DATE_FORMATTER.format(instant);
                values.put(key, count);
            }
        }

        List<DailyValue> series = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 6; i >= 0; i--) {
            Instant day = now.minus(Duration.ofDays(i));
            String key = DATE_FORMATTER.format(day);
            series.add(new DailyValue(key, values.getOrDefault(key, 0L)));
        }
        return series;
    }

    private List<KeyCount> fetchTopUploaders(Connection connection) throws SQLException {
        List<KeyCount> uploaders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uploader_email, COUNT(*) AS uploads " +
                        "FROM files " +
                        "GROUP BY uploader_email " +
                        "ORDER BY uploads DESC, uploader_email ASC " +
                        "LIMIT 5");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String uploader = resultSet.getString("uploader_email");
                long count = resultSet.getLong("uploads");
                uploaders.add(new KeyCount(uploader, count));
            }
        }
        return uploaders;
    }

    private List<UploadEntry> fetchRecentUploads(Connection connection) throws SQLException {
        List<UploadEntry> uploads = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, filename, module, uploader_email, uploaded_at " +
                        "FROM files " +
                        "ORDER BY uploaded_at DESC " +
                        "LIMIT 10");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UploadEntry entry = new UploadEntry();
                entry.fileId = (UUID) resultSet.getObject("id");
                entry.filename = resultSet.getString("filename");
                entry.module = resultSet.getString("module");
                entry.uploader = resultSet.getString("uploader_email");
                Timestamp uploadedAt = resultSet.getTimestamp("uploaded_at");
                if (uploadedAt != null) {
                    entry.timestamp = uploadedAt.toInstant();
                }
                uploads.add(entry);
            }
        }
        return uploads;
    }

    private List<DownloadEntry> fetchRecentDownloads(Connection connection) throws SQLException {
        List<DownloadEntry> downloads = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT e.file_id, f.filename, f.module, e.downloaded_at, u.email, u.display_name " +
                        "FROM file_download_events e " +
                        "LEFT JOIN files f ON f.id = e.file_id " +
                        "LEFT JOIN users u ON u.id = e.user_id " +
                        "ORDER BY e.downloaded_at DESC " +
                        "LIMIT 10");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                DownloadEntry entry = new DownloadEntry();
                entry.fileId = (UUID) resultSet.getObject("file_id");
                entry.filename = resultSet.getString("filename");
                entry.module = resultSet.getString("module");
                Timestamp downloadedAt = resultSet.getTimestamp("downloaded_at");
                if (downloadedAt != null) {
                    entry.timestamp = downloadedAt.toInstant();
                }
                entry.userEmail = resultSet.getString("email");
                entry.userDisplayName = resultSet.getString("display_name");
                downloads.add(entry);
            }
        }
        return downloads;
    }

    private List<LoginEntry> fetchRecentLogins(Connection connection) throws SQLException {
        List<LoginEntry> logins = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT le.login_at, u.email, u.display_name " +
                        "FROM login_events le " +
                        "LEFT JOIN users u ON u.id = le.user_id " +
                        "ORDER BY le.login_at DESC " +
                        "LIMIT 10");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                LoginEntry entry = new LoginEntry();
                Timestamp loginAt = resultSet.getTimestamp("login_at");
                if (loginAt != null) {
                    entry.timestamp = loginAt.toInstant();
                }
                entry.userEmail = resultSet.getString("email");
                entry.userDisplayName = resultSet.getString("display_name");
                logins.add(entry);
            }
        }
        return logins;
    }

    private List<ModuleStat> fetchModuleStats(Connection connection) throws SQLException {
        List<ModuleStat> modules = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT m.code, m.name, " +
                        "COUNT(f.id) AS file_count, " +
                        "COALESCE(SUM(f.size_bytes), 0) AS storage_bytes, " +
                        "MAX(f.uploaded_at) AS last_upload, " +
                        "COALESCE(SUM(fd.download_count), 0) AS download_count, " +
                        "COALESCE(MAX(ms.subscription_count), 0) AS subscription_count " +
                        "FROM modules m " +
                        "LEFT JOIN files f ON f.module = m.code " +
                        "LEFT JOIN ( " +
                        "    SELECT file_id, COUNT(*) AS download_count " +
                        "    FROM file_download_events " +
                        "    GROUP BY file_id " +
                        ") fd ON fd.file_id = f.id " +
                        "LEFT JOIN ( " +
                        "    SELECT module_code, COUNT(*) AS subscription_count " +
                        "    FROM module_subscriptions " +
                        "    WHERE is_active = TRUE " +
                        "    GROUP BY module_code " +
                        ") ms ON ms.module_code = m.code " +
                        "GROUP BY m.code, m.name " +
                        "ORDER BY file_count DESC, download_count DESC, m.name ASC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ModuleStat stat = new ModuleStat();
                stat.code = resultSet.getString("code");
                stat.name = resultSet.getString("name");
                stat.fileCount = resultSet.getLong("file_count");
                stat.downloadCount = resultSet.getLong("download_count");
                stat.subscriptionCount = resultSet.getLong("subscription_count");
                stat.storageBytes = resultSet.getLong("storage_bytes");
                Timestamp lastUpload = resultSet.getTimestamp("last_upload");
                if (lastUpload != null) {
                    stat.lastUpload = lastUpload.toInstant();
                }
                modules.add(stat);
            }
        }
        return modules;
    }

    private List<ActivityEntry> buildActivityFeed(List<UploadEntry> uploads,
                                                  List<DownloadEntry> downloads,
                                                  List<LoginEntry> logins) {
        List<ActivityEntry> feed = new ArrayList<>();

        for (UploadEntry upload : uploads) {
            ActivityEntry entry = new ActivityEntry();
            entry.type = "UPLOAD";
            entry.module = upload.module;
            entry.primary = upload.filename;
            entry.secondary = upload.uploader;
            entry.timestamp = upload.timestamp;
            feed.add(entry);
        }

        for (DownloadEntry download : downloads) {
            ActivityEntry entry = new ActivityEntry();
            entry.type = "DOWNLOAD";
            entry.module = download.module;
            entry.primary = download.filename;
            entry.secondary = download.userDisplayName != null && !download.userDisplayName.isBlank()
                    ? download.userDisplayName
                    : download.userEmail;
            entry.timestamp = download.timestamp;
            feed.add(entry);
        }

        for (LoginEntry login : logins) {
            ActivityEntry entry = new ActivityEntry();
            entry.type = "LOGIN";
            entry.primary = login.userDisplayName != null && !login.userDisplayName.isBlank()
                    ? login.userDisplayName
                    : login.userEmail;
            entry.timestamp = login.timestamp;
            feed.add(entry);
        }

        feed.sort(Comparator.comparing((ActivityEntry a) -> a.timestamp == null ? Instant.MIN : a.timestamp).reversed());
        if (feed.size() > 15) {
            return new ArrayList<>(feed.subList(0, 15));
        }
        return feed;
    }

    private long measureDatabaseLatency() {
        try {
            return databaseService.verifyConnection();
        } catch (SQLException e) {
            return -1L;
        }
    }

    private PerformanceMetrics gatherPerformanceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;

        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.totalMemoryBytes = total;
        metrics.freeMemoryBytes = free;
        metrics.usedMemoryBytes = used;
        metrics.uptime = Duration.between(serverStartedAt, Instant.now());
        return metrics;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format(Locale.ENGLISH, "%.1f KB", kilobytes);
        }
        double megabytes = kilobytes / 1024.0;
        if (megabytes < 1024) {
            return String.format(Locale.ENGLISH, "%.1f MB", megabytes);
        }
        double gigabytes = megabytes / 1024.0;
        return String.format(Locale.ENGLISH, "%.1f GB", gigabytes);
    }

    private static double percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }
        return ((double) (current - previous) / (double) previous) * 100.0;
    }

    private static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0s";
        }
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (builder.length() == 0 || seconds > 0) {
            builder.append(seconds).append("s");
        }
        return builder.toString().trim();
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        return TIMESTAMP_FORMATTER.format(instant);
    }

    /**
     * Aggregated metrics container used to render the monitoring dashboard payload.
     */
    private static final class MonitoringSnapshot {
        long totalFiles;
        long uploadsLast24h;
        long uploadsPrevious24h;
        long downloadsLast24h;
        long downloadsPrevious24h;
        long totalDownloads;
        long storageBytes;
        double averageFileSizeBytes;

        long totalUsers;
        long activeUsers24h;
        long activeUsers7d;

        long databaseLatencyMs;
        PerformanceMetrics performance;
        Instant generatedAt;

        List<DailyValue> dailyUploads = Collections.emptyList();
        List<DailyValue> dailyDownloads = Collections.emptyList();
        List<KeyCount> topUploaders = Collections.emptyList();
        List<UploadEntry> recentUploads = Collections.emptyList();
        List<DownloadEntry> recentDownloads = Collections.emptyList();
        List<LoginEntry> recentLogins = Collections.emptyList();
        List<ModuleStat> moduleStats = Collections.emptyList();
        List<ActivityEntry> activityFeed = Collections.emptyList();

        String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{");

            appendSystemMonitor(json);
            json.append(",");
            appendUploadStatistics(json);
            json.append(",");
            appendUserActivity(json);
            json.append(",");
            appendModuleAnalytics(json);
            json.append(",");
            appendPerformance(json);
            json.append(",");
            appendActivityFeed(json);

            json.append("}");
            return json.toString();
        }

        private void appendSystemMonitor(StringBuilder json) {
            json.append("\"systemMonitor\":{");
            json.append("\"totalFiles\":").append(totalFiles).append(",");
            json.append("\"uploadsLast24h\":").append(uploadsLast24h).append(",");
            json.append("\"uploadsPrevious24h\":").append(uploadsPrevious24h).append(",");
            json.append("\"activeUsers24h\":").append(activeUsers24h).append(",");
            json.append("\"downloadsLast24h\":").append(downloadsLast24h).append(",");
            json.append("\"downloadsPrevious24h\":").append(downloadsPrevious24h).append(",");
            json.append("\"storageUsedBytes\":").append(storageBytes).append(",");
            json.append("\"storageUsedFormatted\":\"").append(escape(formatSize(storageBytes))).append("\",");
            json.append("\"uploadChangePercent\":")
                    .append(String.format(Locale.ENGLISH, "%.2f", percentChange(uploadsLast24h, uploadsPrevious24h))).append(",");
            json.append("\"downloadChangePercent\":")
                    .append(String.format(Locale.ENGLISH, "%.2f", percentChange(downloadsLast24h, downloadsPrevious24h)));
            json.append("}");
        }

        private void appendUploadStatistics(StringBuilder json) {
            json.append("\"uploadStatistics\":{");
            json.append("\"totalUploads\":").append(totalFiles).append(",");
            json.append("\"uploadsLast24h\":").append(uploadsLast24h).append(",");
            json.append("\"downloadsLast24h\":").append(downloadsLast24h).append(",");
            json.append("\"totalDownloads\":").append(totalDownloads).append(",");
            json.append("\"averageFileSizeBytes\":").append(String.format(Locale.ENGLISH, "%.2f", averageFileSizeBytes)).append(",");
            json.append("\"averageFileSizeFormatted\":\"")
                    .append(escape(formatSize((long) averageFileSizeBytes))).append("\",");
            json.append("\"dailyUploads\":");
            appendDailySeries(json, dailyUploads);
            json.append(",");
            json.append("\"dailyDownloads\":");
            appendDailySeries(json, dailyDownloads);
            json.append(",");
            json.append("\"topUploaders\":");
            appendKeyCounts(json, topUploaders);
            json.append(",");
            json.append("\"recentUploads\":");
            appendUploadEntries(json, recentUploads);
            json.append("}");
        }

        private void appendUserActivity(StringBuilder json) {
            json.append("\"userActivity\":{");
            json.append("\"totalUsers\":").append(totalUsers).append(",");
            json.append("\"activeUsers24h\":").append(activeUsers24h).append(",");
            json.append("\"activeUsers7d\":").append(activeUsers7d).append(",");
            json.append("\"recentLogins\":");
            appendLoginEntries(json, recentLogins);
            json.append(",");
            json.append("\"recentDownloads\":");
            appendDownloadEntries(json, recentDownloads);
            json.append("}");
        }

        private void appendModuleAnalytics(StringBuilder json) {
            json.append("\"moduleAnalytics\":{");
            json.append("\"modules\":");
            appendModuleStats(json, moduleStats);
            json.append(",");
            json.append("\"topModules\":");
            appendTopModules(json, moduleStats);
            json.append("}");
        }

        private void appendPerformance(StringBuilder json) {
            json.append("\"performanceMetrics\":{");
            json.append("\"databaseLatencyMs\":").append(databaseLatencyMs).append(",");
            json.append("\"status\":\"").append(databaseLatencyMs >= 0 && databaseLatencyMs <= 750 ? "HEALTHY" : "DEGRADED").append("\",");
            json.append("\"uptimeSeconds\":").append(performance.uptime != null ? performance.uptime.getSeconds() : 0).append(",");
            json.append("\"uptimeFormatted\":\"").append(escape(formatDuration(performance.uptime))).append("\",");
            json.append("\"memory\":{");
            json.append("\"totalBytes\":").append(performance.totalMemoryBytes).append(",");
            json.append("\"usedBytes\":").append(performance.usedMemoryBytes).append(",");
            json.append("\"freeBytes\":").append(performance.freeMemoryBytes).append(",");
            json.append("\"usedFormatted\":\"").append(escape(formatSize(performance.usedMemoryBytes))).append("\",");
            json.append("\"freeFormatted\":\"").append(escape(formatSize(performance.freeMemoryBytes))).append("\"");
            json.append("},");
            json.append("\"generatedAt\":\"").append(formatInstant(generatedAt)).append("\"");
            json.append("}");
        }

        private void appendActivityFeed(StringBuilder json) {
            json.append("\"activityFeed\":[");
            for (int i = 0; i < activityFeed.size(); i++) {
                ActivityEntry entry = activityFeed.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"type\":\"").append(escape(entry.type)).append("\",");
                json.append("\"primary\":\"").append(escape(entry.primary)).append("\",");
                json.append("\"secondary\":\"").append(escape(entry.secondary)).append("\",");
                json.append("\"module\":\"").append(escape(entry.module)).append("\",");
                json.append("\"timestamp\":\"").append(formatInstant(entry.timestamp)).append("\"");
                json.append("}");
            }
            json.append("]");
        }

        private void appendDailySeries(StringBuilder json, List<DailyValue> values) {
            json.append("[");
            for (int i = 0; i < values.size(); i++) {
                DailyValue value = values.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{\"date\":\"").append(escape(value.date)).append("\",\"value\":").append(value.value).append("}");
            }
            json.append("]");
        }

        private void appendKeyCounts(StringBuilder json, List<KeyCount> counts) {
            json.append("[");
            for (int i = 0; i < counts.size(); i++) {
                KeyCount count = counts.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{\"key\":\"").append(escape(count.key)).append("\",\"count\":").append(count.count).append("}");
            }
            json.append("]");
        }

        private void appendUploadEntries(StringBuilder json, List<UploadEntry> entries) {
            json.append("[");
            for (int i = 0; i < entries.size(); i++) {
                UploadEntry entry = entries.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"filename\":\"").append(escape(entry.filename)).append("\",");
                json.append("\"module\":\"").append(escape(entry.module)).append("\",");
                json.append("\"uploader\":\"").append(escape(entry.uploader)).append("\",");
                json.append("\"timestamp\":\"").append(formatInstant(entry.timestamp)).append("\"");
                json.append("}");
            }
            json.append("]");
        }

        private void appendDownloadEntries(StringBuilder json, List<DownloadEntry> entries) {
            json.append("[");
            for (int i = 0; i < entries.size(); i++) {
                DownloadEntry entry = entries.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"filename\":\"").append(escape(entry.filename)).append("\",");
                json.append("\"module\":\"").append(escape(entry.module)).append("\",");
                json.append("\"user\":\"").append(escape(entry.userDisplayName != null && !entry.userDisplayName.isBlank()
                        ? entry.userDisplayName
                        : entry.userEmail)).append("\",");
                json.append("\"timestamp\":\"").append(formatInstant(entry.timestamp)).append("\"");
                json.append("}");
            }
            json.append("]");
        }

        private void appendLoginEntries(StringBuilder json, List<LoginEntry> entries) {
            json.append("[");
            for (int i = 0; i < entries.size(); i++) {
                LoginEntry entry = entries.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"user\":\"").append(escape(entry.userDisplayName != null && !entry.userDisplayName.isBlank()
                        ? entry.userDisplayName
                        : entry.userEmail)).append("\",");
                json.append("\"timestamp\":\"").append(formatInstant(entry.timestamp)).append("\"");
                json.append("}");
            }
            json.append("]");
        }

        private void appendModuleStats(StringBuilder json, List<ModuleStat> modules) {
            json.append("[");
            for (int i = 0; i < modules.size(); i++) {
                ModuleStat stat = modules.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"code\":\"").append(escape(stat.code)).append("\",");
                json.append("\"name\":\"").append(escape(stat.name)).append("\",");
                json.append("\"fileCount\":").append(stat.fileCount).append(",");
                json.append("\"downloadCount\":").append(stat.downloadCount).append(",");
                json.append("\"subscriptionCount\":").append(stat.subscriptionCount).append(",");
                json.append("\"storageBytes\":").append(stat.storageBytes).append(",");
                json.append("\"storageFormatted\":\"").append(escape(formatSize(stat.storageBytes))).append("\",");
                json.append("\"lastUploadAt\":\"").append(formatInstant(stat.lastUpload)).append("\"");
                json.append("}");
            }
            json.append("]");
        }

        private void appendTopModules(StringBuilder json, List<ModuleStat> modules) {
            List<ModuleStat> sorted = new ArrayList<>(modules);
            sorted.sort(Comparator.comparingLong((ModuleStat m) -> m.downloadCount).reversed()
                    .thenComparing((ModuleStat m) -> m.fileCount, Comparator.reverseOrder()));
            if (sorted.size() > 5) {
                sorted = new ArrayList<>(sorted.subList(0, 5));
            }

            json.append("[");
            for (int i = 0; i < sorted.size(); i++) {
                ModuleStat stat = sorted.get(i);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"code\":\"").append(escape(stat.code)).append("\",");
                json.append("\"name\":\"").append(escape(stat.name)).append("\",");
                json.append("\"downloadCount\":").append(stat.downloadCount).append(",");
                json.append("\"fileCount\":").append(stat.fileCount).append("}");
            }
            json.append("]");
        }
    }

    private static final class DailyValue {
        final String date;
        final long value;

        DailyValue(String date, long value) {
            this.date = date;
            this.value = value;
        }
    }

    private static final class KeyCount {
        final String key;
        final long count;

        KeyCount(String key, long count) {
            this.key = key;
            this.count = count;
        }
    }

    private static final class UploadEntry {
        UUID fileId;
        String filename;
        String module;
        String uploader;
        Instant timestamp;
    }

    private static final class DownloadEntry {
        UUID fileId;
        String filename;
        String module;
        String userEmail;
        String userDisplayName;
        Instant timestamp;
    }

    private static final class LoginEntry {
        String userEmail;
        String userDisplayName;
        Instant timestamp;
    }

    private static final class ModuleStat {
        String code;
        String name;
        long fileCount;
        long downloadCount;
        long subscriptionCount;
        long storageBytes;
        Instant lastUpload;
    }

    private static final class ActivityEntry {
        String type;
        String primary;
        String secondary;
        String module;
        Instant timestamp;
    }

    private static final class PerformanceMetrics {
        long totalMemoryBytes;
        long usedMemoryBytes;
        long freeMemoryBytes;
        Duration uptime = Duration.ZERO;
    }
}

