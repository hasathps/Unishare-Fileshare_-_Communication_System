package com.unishare.service;

import com.unishare.model.FileInfo;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;

//Advanced download manager that handles concurrent file downloads efficiently.

public class DownloadManager {
    
    private static final int MAX_CONCURRENT_DOWNLOADS = 10;
    private static final int DOWNLOAD_TIMEOUT_SECONDS = 300; // 5 minutes
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for streaming
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Thread pool for handling download requests concurrently
    private final ExecutorService downloadExecutor;
    
    // Queue to manage download requests when max concurrent limit is reached
    private final BlockingQueue<DownloadRequest> downloadQueue;
    
    // Track active downloads
    private final Map<String, DownloadSession> activeSessions;
    
    // Semaphore to control concurrent download limit
    private final Semaphore downloadSemaphore;
    
    // Bandwidth monitoring (bytes per second)
    private final AtomicLong totalBytesDownloaded = new AtomicLong(0);
    private volatile long lastResetTime = System.currentTimeMillis();
    
    private final FileMetadataService metadataService;
    
    public DownloadManager(FileMetadataService metadataService) {
        this.metadataService = metadataService;
        this.downloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.downloadQueue = new LinkedBlockingQueue<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.downloadSemaphore = new Semaphore(MAX_CONCURRENT_DOWNLOADS);
        
        // Start queue processor thread
        startQueueProcessor();
        
        // Start bandwidth monitoring thread
        startBandwidthMonitor();
        
        System.out.println("✅ DownloadManager initialized with " + MAX_CONCURRENT_DOWNLOADS + " concurrent slots");
    }
    
    /**
     * Initiates a download request and returns a session ID for tracking
     */
    public String initiateDownload(UUID fileId, String userEmail) {
        String sessionId = UUID.randomUUID().toString();
        DownloadRequest request = new DownloadRequest(sessionId, fileId, userEmail, System.currentTimeMillis());
        
        try {
            downloadQueue.offer(request, 5, TimeUnit.SECONDS);
            System.out.println("📥 Download request queued: " + sessionId + " for file: " + fileId);
            return sessionId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Download request queue is full", e);
        }
    }
    
    /**
     * Get download progress for a session
     */
    public Optional<DownloadSession> getDownloadStatus(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }
    
    /**
     * Cancel an active download
     */
    public boolean cancelDownload(String sessionId) {
        DownloadSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.cancel();
            activeSessions.remove(sessionId);
            System.out.println("❌ Download cancelled: " + sessionId);
            return true;
        }
        return false;
    }
    
    /**
     * Get current download statistics
     */
    public DownloadStats getStatistics() {
        return new DownloadStats(
            activeSessions.size(),
            downloadQueue.size(),
            downloadSemaphore.availablePermits(),
            getCurrentBandwidthUsage()
        );
    }
    
    private void startQueueProcessor() {
        Thread queueProcessor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DownloadRequest request = downloadQueue.take();
                    downloadExecutor.submit(() -> processDownload(request));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        queueProcessor.setDaemon(true);
        queueProcessor.setName("DownloadQueueProcessor");
        queueProcessor.start();
    }
    
    private void startBandwidthMonitor() {
        Thread bandwidthMonitor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000); // Reset every 5 seconds
                    resetBandwidthCounter();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        bandwidthMonitor.setDaemon(true);
        bandwidthMonitor.setName("BandwidthMonitor");
        bandwidthMonitor.start();
    }
    
    private void processDownload(DownloadRequest request) {
        try {
            // Acquire semaphore permit (blocks if max concurrent limit reached)
            downloadSemaphore.acquire();
            
            DownloadSession session = new DownloadSession(request.sessionId, DownloadStatus.STARTING);
            activeSessions.put(request.sessionId, session);
            
            try {
                executeDownload(request, session);
            } finally {
                // Always release the permit and clean up
                downloadSemaphore.release();
                activeSessions.remove(request.sessionId);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ Download interrupted: " + request.sessionId);
        }
    }
    
    private void executeDownload(DownloadRequest request, DownloadSession session) {
        int retryCount = 0;
        
        while (retryCount < MAX_RETRY_ATTEMPTS && !session.isCancelled()) {
            try {
                // Get file metadata
                Optional<FileInfo> fileInfoOpt = metadataService.findById(request.fileId);
                if (fileInfoOpt.isEmpty()) {
                    session.setError("File not found");
                    return;
                }
                
                FileInfo fileInfo = fileInfoOpt.get();
                session.setFileInfo(fileInfo);
                session.setStatus(DownloadStatus.DOWNLOADING);
                
                // Download file content from Cloudinary
                byte[] fileContent = downloadFromCloudinary(fileInfo.getSecureUrl(), session);
                
                if (fileContent != null && !session.isCancelled()) {
                    session.setFileContent(fileContent);
                    session.setStatus(DownloadStatus.COMPLETED);
                    totalBytesDownloaded.addAndGet(fileContent.length);
                    
                    System.out.println("✅ Download completed: " + request.sessionId + 
                                     " (" + formatBytes(fileContent.length) + ")");
                }
                return;
                
            } catch (Exception e) {
                retryCount++;
                System.err.println("❌ Download attempt " + retryCount + " failed for " + 
                                 request.sessionId + ": " + e.getMessage());
                
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    session.setError("Download failed after " + MAX_RETRY_ATTEMPTS + " attempts: " + e.getMessage());
                }
            }
        }
    }
    
    private byte[] downloadFromCloudinary(String url, DownloadSession session) throws IOException {
        if (url == null) {
            throw new IOException("Download URL is null");
        }
        
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(30000); // 30 seconds
        connection.setReadTimeout(DOWNLOAD_TIMEOUT_SECONDS * 1000);
        connection.setRequestProperty("User-Agent", "UniShare-DownloadManager/1.0");
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }
            
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                session.setTotalBytes(contentLength);
            }
            
            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1 && !session.isCancelled()) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    session.setBytesDownloaded(totalRead);
                    
                    // Update progress
                    if (contentLength > 0) {
                        session.setProgress((int) ((totalRead * 100) / contentLength));
                    }
                }
                
                if (session.isCancelled()) {
                    return null;
                }
                
                return outputStream.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }
    
    private long getCurrentBandwidthUsage() {
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastResetTime;
        if (timeElapsed > 0) {
            return (totalBytesDownloaded.get() * 1000) / timeElapsed; // bytes per second
        }
        return 0;
    }
    
    private void resetBandwidthCounter() {
        totalBytesDownloaded.set(0);
        lastResetTime = System.currentTimeMillis();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    public void shutdown() {
        System.out.println("🔄 Shutting down DownloadManager...");
        downloadExecutor.shutdown();
        try {
            if (!downloadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("✅ DownloadManager shutdown complete");
    }
    
    // Inner classes for data structures
    
    public static class DownloadRequest {
        final String sessionId;
        final UUID fileId;
        final String userEmail;
        final long timestamp;
        
        public DownloadRequest(String sessionId, UUID fileId, String userEmail, long timestamp) {
            this.sessionId = sessionId;
            this.fileId = fileId;
            this.userEmail = userEmail;
            this.timestamp = timestamp;
        }
    }
    
    public static class DownloadSession {
        private final String sessionId;
        private volatile DownloadStatus status;
        private volatile String errorMessage;
        private volatile boolean cancelled = false;
        private volatile FileInfo fileInfo;
        private volatile byte[] fileContent;
        private volatile long bytesDownloaded = 0;
        private volatile long totalBytes = 0;
        private volatile int progress = 0;
        private final long startTime = System.currentTimeMillis();
        
        public DownloadSession(String sessionId, DownloadStatus status) {
            this.sessionId = sessionId;
            this.status = status;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public DownloadStatus getStatus() { return status; }
        public void setStatus(DownloadStatus status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setError(String error) { 
            this.errorMessage = error; 
            this.status = DownloadStatus.FAILED;
        }
        public boolean isCancelled() { return cancelled; }
        public void cancel() { 
            this.cancelled = true; 
            this.status = DownloadStatus.CANCELLED;
        }
        public FileInfo getFileInfo() { return fileInfo; }
        public void setFileInfo(FileInfo fileInfo) { this.fileInfo = fileInfo; }
        public byte[] getFileContent() { return fileContent; }
        public void setFileContent(byte[] content) { this.fileContent = content; }
        public long getBytesDownloaded() { return bytesDownloaded; }
        public void setBytesDownloaded(long bytes) { this.bytesDownloaded = bytes; }
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long bytes) { this.totalBytes = bytes; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public long getElapsedTime() { return System.currentTimeMillis() - startTime; }
    }
    
    public enum DownloadStatus {
        QUEUED, STARTING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    }
    
    public static class DownloadStats {
        final int activeDownloads;
        final int queuedDownloads;
        final int availableSlots;
        final long bandwidthUsage;
        
        public DownloadStats(int active, int queued, int available, long bandwidth) {
            this.activeDownloads = active;
            this.queuedDownloads = queued;
            this.availableSlots = available;
            this.bandwidthUsage = bandwidth;
        }
        
        public String toJson() {
            return String.format(
                "{\"activeDownloads\":%d,\"queuedDownloads\":%d,\"availableSlots\":%d,\"bandwidthUsage\":%d}",
                activeDownloads, queuedDownloads, availableSlots, bandwidthUsage
            );
        }
    }
}