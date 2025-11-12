package com.unishare.util;

import com.unishare.config.CloudinaryConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal Cloudinary client that performs raw uploads and deletions via REST
 * API.
 */
public final class CloudinaryClient {

    private static final AtomicReference<CloudinaryClient> INSTANCE = new AtomicReference<>();

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    private CloudinaryClient(String cloudName, String apiKey, String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public static CloudinaryClient getInstance() {
        CloudinaryClient existing = INSTANCE.get();
        if (existing != null) {
            return existing;
        }

        ParsedCredentials credentials = ParsedCredentials.parse(CloudinaryConfig.getCloudinaryUrl());
        CloudinaryClient created = new CloudinaryClient(credentials.cloudName, credentials.apiKey,
                credentials.apiSecret);
        if (INSTANCE.compareAndSet(null, created)) {
            return created;
        }
        return INSTANCE.get();
    }

    /**
     * Uploads a file to Cloudinary using resource_type=auto so PDFs, images, and
     * other
     * supported types receive correct Content-Type headers for inline preview.
     */
    public UploadResult uploadRaw(byte[] content, String filename, String folder) throws IOException {
        long timestamp = Instant.now().getEpochSecond();

        String folderParam = folder != null ? folder : "";

        // Build signature parameters in alphabetical order (Cloudinary requirement)
        StringBuilder toSign = new StringBuilder();
        if (!folderParam.isEmpty()) {
            toSign.append("folder=").append(folderParam).append("&");
        }
        toSign.append("timestamp=").append(timestamp);
        // Don't include type in signature - it's a preset parameter

        String signature = sha1Hex(toSign + apiSecret);

        // Use 'auto' resource type to let Cloudinary detect MIME type
        // No explicit type parameter needed - defaults to 'upload' (public)
        String endpoint = String.format("https://api.cloudinary.com/v1_1/%s/auto/upload", cloudName);
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        String boundary = "----UniShareBoundary" + System.currentTimeMillis();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = connection.getOutputStream()) {
            writeFormField(os, boundary, "api_key", apiKey);
            writeFormField(os, boundary, "timestamp", String.valueOf(timestamp));
            writeFormField(os, boundary, "signature", signature);
            if (!folderParam.isEmpty()) {
                writeFormField(os, boundary, "folder", folderParam);
            }
            writeFileField(os, boundary, "file", filename, content);
            os.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        InputStream responseStream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String responseBody = readStream(responseStream);
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("Cloudinary upload failed (" + status + "): " + responseBody);
        }

        Map<String, String> payload = JsonUtils.parseObject(responseBody);
        String publicId = payload.get("public_id");
        String secureUrl = payload.get("secure_url");
        String bytesStr = payload.get("bytes");

        long bytes = bytesStr != null ? Long.parseLong(bytesStr) : content.length;
        if (publicId == null || secureUrl == null) {
            throw new IOException("Cloudinary response missing identifiers: " + responseBody);
        }

        return new UploadResult(publicId, secureUrl, bytes);
    }

    public void deleteRaw(String publicId) throws IOException {
        Objects.requireNonNull(publicId, "publicId");

        long timestamp = Instant.now().getEpochSecond();
        String toSign = "public_id=" + publicId + "&timestamp=" + timestamp;
        String signature = sha1Hex(toSign + apiSecret);

        String endpoint = String.format("https://api.cloudinary.com/v1_1/%s/raw/destroy", cloudName);
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        String boundary = "----UniShareBoundary" + System.currentTimeMillis();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = connection.getOutputStream()) {
            writeFormField(os, boundary, "api_key", apiKey);
            writeFormField(os, boundary, "timestamp", String.valueOf(timestamp));
            writeFormField(os, boundary, "signature", signature);
            writeFormField(os, boundary, "public_id", publicId);
            os.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            String responseBody = readStream(connection.getErrorStream());
            throw new IOException("Cloudinary destroy failed (" + status + "): " + responseBody);
        }
        connection.disconnect();
    }

    private void writeFormField(OutputStream os, String boundary, String name, String value) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(value.getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileField(OutputStream os, String boundary, String fieldName, String filename, byte[] content)
            throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        os.write(content);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream is = stream; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }

    public record UploadResult(String publicId, String secureUrl, long bytes) {
    }

    private record ParsedCredentials(String cloudName, String apiKey, String apiSecret) {

        static ParsedCredentials parse(String cloudinaryUrl) {
            if (cloudinaryUrl == null || !cloudinaryUrl.startsWith("cloudinary://")) {
                throw new IllegalArgumentException("Invalid Cloudinary URL");
            }

            String withoutScheme = cloudinaryUrl.substring("cloudinary://".length());
            int atIndex = withoutScheme.indexOf('@');
            if (atIndex <= 0) {
                throw new IllegalArgumentException("Cloudinary URL missing cloud name");
            }

            String credentials = withoutScheme.substring(0, atIndex);
            String cloud = withoutScheme.substring(atIndex + 1);

            int colonIndex = credentials.indexOf(':');
            if (colonIndex <= 0) {
                throw new IllegalArgumentException("Cloudinary URL missing API secret");
            }

            String key = credentials.substring(0, colonIndex);
            String secret = credentials.substring(colonIndex + 1);

            return new ParsedCredentials(cloud, key, secret);
        }
    }
}
