// Download service for managing concurrent downloads
class DownloadService {
  constructor() {
    this.activeDownloads = new Map();
    this.eventListeners = new Map();
  }

  /**
   * Initiate a download and return session information
   */
  async download(fileId, filename) {
    try {
      console.log(`📥 Starting file download: ${filename}`);
      
      const response = await fetch(`/api/download/${fileId}`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`Download initiation failed: ${response.status}`);
      }

      const data = await response.json();
      
      // Store download session
      this.activeDownloads.set(data.sessionId, {
        sessionId: data.sessionId,
        filename: filename || data.filename,
        status: 'initiated',
        progress: 0,
        bytesDownloaded: 0,
        totalBytes: 0,
        startTime: Date.now()
      });

      // Start monitoring progress
      this.monitorProgress(data.sessionId);

      return data.sessionId;
    } catch (error) {
      console.error('Download initiation failed:', error);
      throw error;
    }
  }

  /**
   * Monitor download progress
   */
  async monitorProgress(sessionId) {
    const monitor = async () => {
      try {
        const response = await fetch(`/api/download-status/${sessionId}`, {
          credentials: 'include'
        });

        if (!response.ok) {
          this.updateDownloadStatus(sessionId, { status: 'failed', error: 'Status check failed' });
          return;
        }

        const status = await response.json();
        
        // Update local cache
        const download = this.activeDownloads.get(sessionId);
        if (download) {
          Object.assign(download, status);
          this.activeDownloads.set(sessionId, download);
        }

        // Notify listeners
        this.notifyListeners(sessionId, status);

        // Continue monitoring if still in progress
        if (status.status === 'downloading' || status.status === 'starting' || status.status === 'queued') {
          setTimeout(monitor, 1000); // Check every second
        } else if (status.status === 'completed') {
          // Auto-download the completed file
          this.downloadCompletedFile(sessionId);
          // Auto-cleanup completed downloads after 30 seconds
          setTimeout(() => this.removeDownload(sessionId), 30000);
        } else {
          // Remove failed or cancelled downloads after 10 seconds
          setTimeout(() => this.removeDownload(sessionId), 10000);
        }
      } catch (error) {
        console.error('Progress monitoring failed:', error);
        this.updateDownloadStatus(sessionId, { status: 'failed', error: error.message });
      }
    };

    monitor();
  }

  /**
   * Download completed file from session
   */
  async downloadCompletedFile(sessionId) {
    try {
      const download = this.activeDownloads.get(sessionId);
      if (!download) {
        console.warn('No download session found for:', sessionId);
        return;
      }

      // Get the file content from the download session
      const response = await fetch(`/api/download-file/${sessionId}`, {
        method: 'GET',
        headers: {
          'Accept': 'application/octet-stream'
        },
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`File download failed: ${response.status}`);
      }

      // Create download link
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = download.filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);

      console.log(`✅ File downloaded: ${download.filename}`);
    } catch (error) {
      console.error('Failed to download completed file:', error);
      // Update status to show download failed
      this.updateDownloadStatus(sessionId, { status: 'failed', error: 'File download failed' });
    }
  }

  /**
   * Cancel a download
   */
  async cancelDownload(sessionId) {
    try {
      const response = await fetch(`/api/download-cancel/${sessionId}`, {
        method: 'POST',
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`Cancellation failed: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.cancelled) {
        this.updateDownloadStatus(sessionId, { status: 'cancelled' });
        setTimeout(() => this.removeDownload(sessionId), 5000);
      }

      return result.cancelled;
    } catch (error) {
      console.error('Download cancellation failed:', error);
      throw error;
    }
  }

  /**
   * Get download statistics
   */
  async getStatistics() {
    try {
      const response = await fetch('/api/download-stats', {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`Stats retrieval failed: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Stats retrieval failed:', error);
      return { activeDownloads: 0, queuedDownloads: 0, availableSlots: 0, bandwidthUsage: 0 };
    }
  }

  /**
   * Get active downloads
   */
  getActiveDownloads() {
    return Array.from(this.activeDownloads.values());
  }

  
  getDownloadStatus(sessionId) {
    return this.activeDownloads.get(sessionId);
  }

  addProgressListener(sessionId, callback) {
    if (!this.eventListeners.has(sessionId)) {
      this.eventListeners.set(sessionId, []);
    }
    this.eventListeners.get(sessionId).push(callback);
  }

  removeProgressListener(sessionId, callback) {
    const listeners = this.eventListeners.get(sessionId);
    if (listeners) {
      const index = listeners.indexOf(callback);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  
  updateDownloadStatus(sessionId, updates) {
    const download = this.activeDownloads.get(sessionId);
    if (download) {
      Object.assign(download, updates);
      this.activeDownloads.set(sessionId, download);
      this.notifyListeners(sessionId, download);
    }
  }

  notifyListeners(sessionId, status) {
    const listeners = this.eventListeners.get(sessionId);
    if (listeners) {
      listeners.forEach(callback => {
        try {
          callback(status);
        } catch (error) {
          console.error('Listener callback failed:', error);
        }
      });
    }
  }

// Remove download from tracking
  removeDownload(sessionId) {
    this.activeDownloads.delete(sessionId);
    this.eventListeners.delete(sessionId);
  }

  /**
   * Format bytes for display
   */
  formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  /**
   * Format speed for display
   */
  formatSpeed(bytesPerSecond) {
    return this.formatBytes(bytesPerSecond) + '/s';
  }

  /**
   * Calculate download speed
   */
  calculateSpeed(sessionId) {
    const download = this.activeDownloads.get(sessionId);
    if (!download || !download.startTime || download.bytesDownloaded === 0) {
      return 0;
    }

    const timeElapsed = (Date.now() - download.startTime) / 1000; // seconds
    return download.bytesDownloaded / timeElapsed; // bytes per second
  }

 
  /**
   * Get estimated time remaining
   */
  getETA(sessionId) {
    const download = this.activeDownloads.get(sessionId);
    if (!download || download.totalBytes === 0 || download.bytesDownloaded === 0) {
      return 'Unknown';
    }

    const speed = this.calculateSpeed(sessionId);
    if (speed === 0) return 'Unknown';

    const remainingBytes = download.totalBytes - download.bytesDownloaded;
    const eta = remainingBytes / speed; // seconds

    if (eta < 60) {
      return `${Math.round(eta)}s`;
    } else if (eta < 3600) {
      return `${Math.round(eta / 60)}m`;
    } else {
      return `${Math.round(eta / 3600)}h`;
    }
  }
}

// Create singleton instance
export const downloadService = new DownloadService();