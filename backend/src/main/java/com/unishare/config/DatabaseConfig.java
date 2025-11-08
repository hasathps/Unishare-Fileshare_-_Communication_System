package com.unishare.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Holds configuration values for connecting to the Neon Postgres database.
 * <p>
 * The connection string is expected to be provided through the {@code NEON_DB_URL}
 * environment variable in the standard PostgreSQL URI format, for example:
 * <pre>
 * postgresql://user:password@host/database?sslmode=require&channel_binding=require
 * </pre>
 * The class converts this URI into a JDBC-compatible form and exposes the pieces
 * needed by {@link java.sql.DriverManager}.
 */
public final class DatabaseConfig {

    private static final String DEFAULT_ENV_VARIABLE = "NEON_DB_URL";

    private DatabaseConfig() {
        // Utility class
    }

    public static DatabaseCredentials loadCredentials() {
        String rawUrl = System.getenv(DEFAULT_ENV_VARIABLE);

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT,
                    "Environment variable %s must be set with the Neon connection string.",
                    DEFAULT_ENV_VARIABLE
            ));
        }

        return parseConnectionString(rawUrl.trim());
    }

    private static DatabaseCredentials parseConnectionString(String connectionString) {
        Objects.requireNonNull(connectionString, "connectionString");

        String normalized = connectionString.replaceFirst("^postgres(ql)?://", "https://");

        URI uri;
        try {
            uri = new URI(normalized);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Neon connection string format", e);
        }

        String userInfo = uri.getUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("Connection string must include username and password.");
        }

        String[] userParts = userInfo.split(":", 2);
        String username = userParts[0];
        String password = userParts[1];

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Connection string must include a host.");
        }

        String path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            throw new IllegalArgumentException("Connection string must include a database name.");
        }

        StringBuilder jdbcUrl = new StringBuilder();
        jdbcUrl.append("jdbc:postgresql://").append(host);

        if (uri.getPort() != -1) {
            jdbcUrl.append(":").append(uri.getPort());
        }

        jdbcUrl.append(path);

        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbcUrl.append("?").append(uri.getQuery());
        } else {
            // Ensure TLS is always required when using Neon.
            jdbcUrl.append("?sslmode=require");
        }

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        return new DatabaseCredentials(jdbcUrl.toString(), properties);
    }

    /**
     * Simple value object that exposes the JDBC URL and properties.
     */
    public static final class DatabaseCredentials {
        private final String jdbcUrl;
        private final Properties properties;

        private DatabaseCredentials(String jdbcUrl, Properties properties) {
            this.jdbcUrl = jdbcUrl;
            this.properties = properties;
        }

        public String jdbcUrl() {
            return jdbcUrl;
        }

        public Properties properties() {
            return properties;
        }
    }
}

