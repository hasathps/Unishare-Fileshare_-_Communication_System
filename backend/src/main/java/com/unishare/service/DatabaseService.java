package com.unishare.service;

import com.unishare.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight database service responsible for creating JDBC connections to the Neon database.
 * <p>
 * Usage:
 * <pre>
 * DatabaseService databaseService = DatabaseService.getInstance();
 * try (Connection connection = databaseService.getConnection()) {
 *     // Execute SQL
 * }
 * </pre>
 */
public final class DatabaseService {

    private static final DatabaseService INSTANCE = new DatabaseService();

    private final DatabaseConfig.DatabaseCredentials credentials;
    private final AtomicBoolean driverLoaded = new AtomicBoolean(false);

    private DatabaseService() {
        this.credentials = DatabaseConfig.loadCredentials();
        logResolvedCredentials();
        loadDriver();
    }

    public static DatabaseService getInstance() {
        return INSTANCE;
    }

    private void logResolvedCredentials() {
        String jdbcUrl = credentials.jdbcUrl();
        String user = credentials.properties().getProperty("user");

        String maskedUrl = jdbcUrl.replaceAll("password=[^&]+", "password=****");

        System.out.println("[DatabaseService] JDBC URL resolved to: " + maskedUrl);
        System.out.println("[DatabaseService] Database user: " + user);
    }

    /**
     * Provides a fresh JDBC connection each time it is called. The caller is responsible for closing it.
     *
     * @return an open {@link Connection}
     * @throws SQLException if the database cannot be reached
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(credentials.jdbcUrl(), credentials.properties());
    }

    /**
     * Executes a basic connectivity check against the database. Useful during application start-up.
     *
     * @return latency in milliseconds between request and successful response
     * @throws SQLException if the connection attempt fails
     */
    public long verifyConnection() throws SQLException {
        Instant start = Instant.now();
        try (Connection connection = getConnection()) {
            connection.isValid(5);
            return Duration.between(start, Instant.now()).toMillis();
        }
    }

    private void loadDriver() {
        if (driverLoaded.compareAndSet(false, true)) {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                driverLoaded.set(false);
                throw new IllegalStateException(
                        "PostgreSQL JDBC driver not found on the classpath. " +
                                "Add org.postgresql:postgresql to the build.",
                        e
                );
            }
        }
    }

    @Override
    public String toString() {
        return "DatabaseService{" +
                "jdbcUrl='" + credentials.jdbcUrl() + '\'' +
                '}';
    }
}

