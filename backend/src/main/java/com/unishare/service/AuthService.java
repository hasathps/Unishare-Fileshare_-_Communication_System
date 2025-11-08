package com.unishare.service;

import com.unishare.model.User;
import com.unishare.util.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles user authentication, session management, and login tracking.
 */
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);
    private static final String SESSION_COOKIE_NAME = "UNISESSION";

    private final DatabaseService databaseService;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public String getSessionCookieName() {
        return SESSION_COOKIE_NAME;
    }

    public User register(String email, String password, String displayName) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        UUID userId = UUID.randomUUID();
        char[] passChars = password.toCharArray();
        String passwordHash;
        try {
            passwordHash = PasswordUtils.hashPassword(passChars);
        } finally {
            Arrays.fill(passChars, '\0');
        }

        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (id, email, password_hash, display_name, created_at) " +
                             "VALUES (?, ?, ?, ?, ?)")) {
            statement.setObject(1, userId);
            statement.setString(2, normalizedEmail);
            statement.setString(3, passwordHash);
            statement.setString(4, displayName);
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.executeUpdate();
            try (PreparedStatement usernameUpdate = connection.prepareStatement(
                    "UPDATE users SET username = ? WHERE id = ?")) {
                usernameUpdate.setString(1, normalizedEmail);
                usernameUpdate.setObject(2, userId);
                usernameUpdate.executeUpdate();
            } catch (SQLException ignored) {
                // username column is legacy; ignore if it doesn't exist
            }
        }

        return new User(userId, normalizedEmail, displayName, passwordHash, Instant.now());
    }

    public LoginResult login(String email, String password, String remoteIp) throws SQLException {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        Optional<User> optionalUser = findByEmail(email.trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            throw new AuthenticationException("Invalid credentials");
        }

        User user = optionalUser.get();
        char[] passChars = password.toCharArray();
        boolean verified;
        try {
            verified = PasswordUtils.verifyPassword(passChars, user.getPasswordHash());
        } finally {
            Arrays.fill(passChars, '\0');
        }

        if (!verified) {
            throw new AuthenticationException("Invalid credentials");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Session session = new Session(user, Instant.now().plus(SESSION_TTL));
        sessions.put(token, session);

        recordLoginEvent(user.getId(), remoteIp);

        return new LoginResult(token, user);
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Optional<User> findBySessionToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }

        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }

        Session refreshed = new Session(session.user(), Instant.now().plus(SESSION_TTL));
        sessions.put(token, refreshed);

        return Optional.of(session.user());
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, email, password_hash, display_name, created_at FROM users WHERE email = ?")) {
            statement.setString(1, email.trim().toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    UUID id = (UUID) resultSet.getObject("id");
                    String emailAddress = resultSet.getString("email");
                    String passwordHash = resultSet.getString("password_hash");
                    String displayName = resultSet.getString("display_name");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    return Optional.of(new User(id, emailAddress, displayName, passwordHash,
                            createdAt != null ? createdAt.toInstant() : null));
                }
            }
        }
        return Optional.empty();
    }

    private void recordLoginEvent(UUID userId, String remoteIp) throws SQLException {
        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO login_events (user_id, remote_ip, login_at) VALUES (?, ?, ?)")) {
            statement.setObject(1, userId);
            statement.setString(2, remoteIp);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    public static final class LoginResult {
        private final String token;
        private final User user;

        public LoginResult(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String token() {
            return token;
        }

        public User user() {
            return user;
        }
    }

    private static final class Session {
        private final User user;
        private final Instant expiresAt;

        private Session(User user, Instant expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }

        public User user() {
            return user;
        }

        public Instant expiresAt() {
            return expiresAt;
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}

