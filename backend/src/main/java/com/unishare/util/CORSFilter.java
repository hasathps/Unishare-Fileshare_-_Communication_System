package com.unishare.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * Utility class for handling CORS headers
 */
public class CORSFilter {
    
    public static void addCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", 
            "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", 
            "Content-Type, Authorization, X-Requested-With");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
    }
    
    public static void handlePreflightRequest(HttpExchange exchange) throws IOException {
        addCORSHeaders(exchange);
        exchange.sendResponseHeaders(200, 0);
        exchange.close();
    }
}
