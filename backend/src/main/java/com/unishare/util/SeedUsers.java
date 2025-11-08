package com.unishare.util;

import com.unishare.service.AuthService;
import com.unishare.service.DatabaseService;
import com.unishare.service.SchemaInitializer;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Command line utility to seed default users.
 */
public final class SeedUsers {

    private SeedUsers() {
    }

    public static void main(String[] args) throws IOException {
        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            SchemaInitializer.initialize(databaseService);
        } catch (SQLException e) {
            System.err.println("Failed to initialize schema: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        AuthService authService = new AuthService(databaseService);

        String email = "admin@unishare.local";
        String password = "Unishare@2025";
        String displayName = "UniShare Admin";

        try {
            authService.register(email, password, displayName);
            System.out.println("Created seed user:");
            System.out.println("   Email: " + email);
            System.out.println("   Password: " + password);
        } catch (AuthService.AuthenticationException | IllegalArgumentException e) {
            System.out.println("Seed user already exists. No changes made.");
        } catch (SQLException e) {
            System.err.println("Failed to seed user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

