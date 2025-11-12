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
                            ")");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS login_events (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                            "login_at TIMESTAMPTZ DEFAULT NOW()," +
                            "remote_ip VARCHAR(64)" +
                            ")");

            // Create modules table early for foreign key references
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS modules (" +
                            "code VARCHAR(100) PRIMARY KEY," +
                            "name VARCHAR(255) NOT NULL," +
                            "description TEXT," +
                            "created_at TIMESTAMPTZ DEFAULT NOW()" +
                            ")");


            statement.execute(
                    "CREATE TABLE IF NOT EXISTS files (" +
                            "id uuid PRIMARY KEY," +
                            "module VARCHAR(100) NOT NULL," +
                            "uploader_email VARCHAR(255) NOT NULL," +
                            "filename VARCHAR(255) NOT NULL," +
                            "storage_key VARCHAR(255) NOT NULL," +
                            "secure_url TEXT NOT NULL," +
                            "size_bytes BIGINT NOT NULL," +
                            "uploaded_at TIMESTAMPTZ DEFAULT NOW()" +
                            ")");

            statement.execute(
                    "CREATE INDEX IF NOT EXISTS files_module_idx ON files (module)");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS files_uploaded_at_idx ON files (uploaded_at)");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS file_download_events (" +
                            "id BIGSERIAL PRIMARY KEY," +
                            "file_id uuid NOT NULL REFERENCES files(id) ON DELETE CASCADE," +
                            "user_id uuid REFERENCES users(id) ON DELETE SET NULL," +
                            "module_code VARCHAR(100) REFERENCES modules(code) ON DELETE SET NULL," +
                            "downloaded_at TIMESTAMPTZ DEFAULT NOW()" +
                            ")");

            statement.execute(
                    "CREATE INDEX IF NOT EXISTS file_download_events_file_idx ON file_download_events (file_id)");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS file_download_events_module_idx ON file_download_events (module_code)");
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS file_download_events_downloaded_at_idx ON file_download_events (downloaded_at)");
=======
            // Create files table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS files (" +
                            "id UUID PRIMARY KEY," +
                            "module VARCHAR(100) NOT NULL REFERENCES modules(code) ON DELETE CASCADE," +
                            "uploader_email VARCHAR(255) NOT NULL," +
                            "filename TEXT NOT NULL," +
                            "storage_key VARCHAR(255)," +
                            "secure_url TEXT," +
                            "size_bytes BIGINT," +
                            "uploaded_at TIMESTAMPTZ DEFAULT NOW()" +
                            ")");

            // Helpful indexes for faster module/file lookups
            statement.execute("CREATE INDEX IF NOT EXISTS files_module_idx ON files (module)");
            statement.execute("CREATE INDEX IF NOT EXISTS files_module_uploaded_idx ON files (module, uploaded_at DESC)");


            // Seed initial modules if table is empty
            seedModules(connection);

            // Create module subscriptions table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS module_subscriptions (" +
                            "id uuid PRIMARY KEY DEFAULT gen_random_uuid()," +
                            "user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                            "module_code VARCHAR(100) NOT NULL REFERENCES modules(code) ON DELETE CASCADE," +
                            "subscribed_at TIMESTAMPTZ DEFAULT NOW()," +
                            "is_active BOOLEAN DEFAULT TRUE," +
                            "UNIQUE(user_id, module_code)" +
                            ")");

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
                    alter.execute("ALTER TABLE users ALTER COLUMN username DROP NOT NULL");
                } catch (SQLException ignored) {
                    // username column may not exist; ignore
                }
                try {
                    alter.execute("UPDATE users SET username = email WHERE username IS NULL");
                } catch (SQLException ignored) {
                    // username column may not exist; ignore
                }
                try {
                    alter.execute("CREATE UNIQUE INDEX IF NOT EXISTS users_email_idx ON users (email)");
                } catch (SQLException ignored) {
                    // index may already exist with different name
                }
            }
        }
    }

    private static void seedModules(Connection connection) throws SQLException {
        // Check if modules table is empty
        try (Statement checkStmt = connection.createStatement();
                var rs = checkStmt.executeQuery("SELECT COUNT(*) FROM modules")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Modules already exist, skip seeding
                return;
            }
        }

        // Seed initial modules
        String[] modules = {
                "applied-numerical-methods|Applied Numerical Methods|Applied Numerical Methods",
                "automata-theory|Automata Theory|Automata Theory",
                "artificial-intelligence|Artificial Intelligence|Artificial Intelligence",
                "enterprise-application-development|Enterprise Application Development|Enterprise Application Development",
                "network-programming|Network programming|Network programming",
                "mobile-applications-development|Mobile Applications Development|Mobile Applications Development",
                "embedded-systems|Embedded Systems|Embedded Systems",
                "wireless-communication-and-mobile-networks|Wireless Communication & Mobile Networks|Wireless Communication & Mobile Networks",
                "human-computer-interaction|Human Computer Interaction|Human Computer Interaction",
                "communication-skills-and-professional-conduct|Communication Skills and Professional Conduct|Communication Skills and Professional Conduct",
                "management-information-systems|Management Information Systems|Management Information Systems"
        };

        try (var stmt = connection.prepareStatement(
                "INSERT INTO modules (code, name, description) VALUES (?, ?, ?)")) {
            for (String module : modules) {
                String[] parts = module.split("\\|");
                stmt.setString(1, parts[0]); // code
                stmt.setString(2, parts[1]); // name
                stmt.setString(3, parts[2]); // description
                stmt.addBatch();
            }
            stmt.executeBatch();
            System.out.println("✅ Seeded " + modules.length + " modules into database");
        }
    }
}
