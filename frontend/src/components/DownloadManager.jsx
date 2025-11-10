import React, { useState, useEffect } from 'react';
import { Download, X, Pause, Play, Clock, AlertCircle, CheckCircle } from 'lucide-react';
import { downloadService } from '../services/downloadService';

const DownloadManager = ({ isOpen, onClose }) => {
  const [downloads, setDownloads] = useState([]);
  const [stats, setStats] = useState({ activeDownloads: 0, queuedDownloads: 0, availableSlots: 0, bandwidthUsage: 0 });

  useEffect(() => {
    if (isOpen) {
      const interval = setInterval(() => {
        setDownloads(downloadService.getActiveDownloads());
        updateStats();
      }, 1000);

      return () => clearInterval(interval);
    }
  }, [isOpen]);

  const updateStats = async () => {
    try {
      const newStats = await downloadService.getStatistics();
      setStats(newStats);
    } catch (error) {
      console.error('Failed to update stats:', error);
    }
  };

  const handleCancel = async (sessionId) => {
    try {
      await downloadService.cancelDownload(sessionId);
    } catch (error) {
      console.error('Failed to cancel download:', error);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'completed':
        return <CheckCircle size={16} className="text-green-500" />;
      case 'failed':
      case 'cancelled':
        return <AlertCircle size={16} className="text-red-500" />;
      case 'downloading':
        return <Download size={16} className="text-blue-500 animate-pulse" />;
      case 'queued':
        return <Clock size={16} className="text-yellow-500" />;
      default:
        return <Download size={16} className="text-gray-500" />;
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'queued':
        return 'Queued';
      case 'starting':
        return 'Starting...';
      case 'downloading':
        return 'Downloading';
      case 'completed':
        return 'Completed';
      case 'failed':
        return 'Failed';
      case 'cancelled':
        return 'Cancelled';
      default:
        return 'Unknown';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-11/12 max-w-4xl max-h-5/6 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b">
          <div className="flex items-center space-x-3">
            <Download className="text-blue-600" size={24} />
            <h2 className="text-xl font-semibold">Download Manager</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded"
          >
            <X size={20} />
          </button>
        </div>

        {/* Stats */}
        <div className="p-4 bg-gray-50 border-b">
          <div className="grid grid-cols-4 gap-4 text-center">
            <div>
              <div className="text-2xl font-bold text-blue-600">{stats.activeDownloads}</div>
              <div className="text-sm text-gray-600">Active</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-yellow-600">{stats.queuedDownloads}</div>
              <div className="text-sm text-gray-600">Queued</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-green-600">{stats.availableSlots}</div>
              <div className="text-sm text-gray-600">Available</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-purple-600">
                {downloadService.formatSpeed(stats.bandwidthUsage)}
              </div>
              <div className="text-sm text-gray-600">Bandwidth</div>
            </div>
          </div>
        </div>

        {/* Downloads List */}
        <div className="flex-1 overflow-y-auto max-h-96">
          {downloads.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              <Download size={48} className="mx-auto mb-4 text-gray-300" />
              <p>No active downloads</p>
            </div>
          ) : (
            <div className="p-4">
              {downloads.map((download) => (
                <div key={download.sessionId} className="mb-4 p-4 border rounded-lg bg-white">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-3">
                      {getStatusIcon(download.status)}
                      <div>
                        <div className="font-medium">{download.filename}</div>
                        <div className="text-sm text-gray-500">
                          {getStatusText(download.status)}
                          {download.error && ` - ${download.error}`}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      {(download.status === 'downloading' || download.status === 'queued') && (
                        <button
                          onClick={() => handleCancel(download.sessionId)}
                          className="p-1 text-red-500 hover:bg-red-50 rounded"
                          title="Cancel Download"
                        >
                          <X size={16} />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Progress Bar */}
                  {download.totalBytes > 0 && (
                    <div className="mb-2">
                      <div className="flex justify-between text-sm text-gray-600 mb-1">
                        <span>{downloadService.formatBytes(download.bytesDownloaded)} of {downloadService.formatBytes(download.totalBytes)}</span>
                        <span>{download.progress}%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                          style={{ width: `${Math.min(download.progress, 100)}%` }}
                        ></div>
                      </div>
                    </div>
                  )}

                  {/* Download Info */}
                  {download.status === 'downloading' && (
                    <div className="flex justify-between text-sm text-gray-500">
                      <span>Speed: {downloadService.formatSpeed(downloadService.calculateSpeed(download.sessionId))}</span>
                      <span>ETA: {downloadService.getETA(download.sessionId)}</span>
                    </div>
                  )}

                  {download.status === 'completed' && download.totalBytes > 0 && (
                    <div className="text-sm text-green-600">
                      Downloaded {downloadService.formatBytes(download.totalBytes)} successfully
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t bg-gray-50">
          <div className="text-sm text-gray-600 text-center">
            Concurrent downloads with automatic queue management and progress tracking
          </div>
        </div>
      </div>
    </div>
  );
};

export default DownloadManager;