package com.unishare.config;

/**
 * Utility for obtaining Cloudinary configuration from environment variables.
 */
public final class CloudinaryConfig {

    private CloudinaryConfig() {
    }

    /**
     * Returns the Cloudinary URL in the form cloudinary://key:secret@cloud.
     */
    public static String getCloudinaryUrl() {
        String url = System.getenv("CLOUDINARY_URL");
        if (url != null && !url.isBlank()) {
            return url.trim();
        }

        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");

        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException(
                    "Cloudinary credentials are not configured. Set CLOUDINARY_URL " +
                            "or CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET environment variables.");
        }

        return String.format("cloudinary://%s:%s@%s", apiKey, apiSecret, cloudName);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

