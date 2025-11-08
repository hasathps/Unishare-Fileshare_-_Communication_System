package com.unishare.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * Utility class for handling CORS headers
 */
public class CORSFilter {
    
    private static final String[] DEFAULT_ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    };

    private static final String[] ALLOWED_ORIGINS = resolveAllowedOrigins();

    public static void addCORSHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String allowOrigin = "*";
        boolean allowCredentials = false;

        if (origin != null && isAllowedOrigin(origin)) {
            allowOrigin = origin;
            allowCredentials = true;
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowOrigin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With, Accept");
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers",
                "Content-Disposition");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
        exchange.getResponseHeaders().add("Vary", "Origin");

        if (allowCredentials) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        }
    }

    public static void handlePreflightRequest(HttpExchange exchange) throws IOException {
        addCORSHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private static boolean isAllowedOrigin(String origin) {
        for (String allowed : ALLOWED_ORIGINS) {
            if (allowed.equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return false;
    }

    private static String[] resolveAllowedOrigins() {
        String configured = System.getenv("ALLOWED_ORIGINS");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_ALLOWED_ORIGINS;
        }
        return configured.trim()
                .replace(" ", "")
                .split(",");
    }
}
