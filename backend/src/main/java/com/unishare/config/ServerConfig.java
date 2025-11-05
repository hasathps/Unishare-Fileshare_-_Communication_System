package com.unishare.config;

/**
 * Configuration class for server settings
 */
public class ServerConfig {
    
    public static final int PORT = 8082;
    public static final String UPLOAD_DIR = "uploads";
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int MAX_CONCURRENT_UPLOADS = 5;
    
    // CORS settings
    public static final String ALLOWED_ORIGINS = "*";
    public static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS";
    public static final String ALLOWED_HEADERS = "Content-Type, Authorization, X-Requested-With";
    
    // File upload settings
    public static final String[] ALLOWED_FILE_EXTENSIONS = {
        "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg", "gif"
    };
    
    // Module settings
    public static final String[] AVAILABLE_MODULES = {
        "IN3111", "CS101", "MATH201", "PHYS202", "CHEM103"
    };
}
