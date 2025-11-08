package com.unishare.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ensures required database tables exist.
 */
public final class SchemaInitializer {

    private SchemaInitializer() {
    }

    public static void initialize(DatabaseService databaseService) throws SQLException {
        try (Connection connection = databaseService.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id uuid PRIMARY KEY," +
                            "email VARCHAR(255) UNIQUE NOT NULL," +
                            "password_hash VARCHAR(512) NOT NULL," +
                            "display_name VARCHAR(128)," +
                            "created_at TIMESTAMPTZ DEFAULT NOW()" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS login_events (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                            "login_at TIMESTAMPTZ DEFAULT NOW()," +
                            "remote_ip VARCHAR(64)" +
                            ")"
            );

            try (Statement alter = connection.createStatement()) {
                alter.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255)");
                try {
                    alter.execute("UPDATE users SET email = username WHERE email IS NULL");
                } catch (SQLException ignored) {
                    // username column may not exist; ignore
                }
                try {
                    alter.execute("ALTER TABLE users ALTER COLUMN email SET NOT NULL");
                } catch (SQLException ignored) {
                    // existing null values prevent constraint; leave as-is
                }
                try {
                    alter.execute("CREATE UNIQUE INDEX IF NOT EXISTS users_email_idx ON users (email)");
                } catch (SQLException ignored) {
                    // index may already exist with different name
                }
            }
        }
    }
}

