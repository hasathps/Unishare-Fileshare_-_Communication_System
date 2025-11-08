package com.unishare.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an authenticated user of the system.
 */
public class User {

    private final UUID id;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final Instant createdAt;

    public User(UUID id, String email, String displayName, String passwordHash, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.displayName = displayName;
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

