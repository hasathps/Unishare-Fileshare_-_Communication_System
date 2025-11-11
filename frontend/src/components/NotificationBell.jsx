import React, { useState, useEffect } from "react";
import { Bell, X } from "lucide-react";
import api from "../services/api";
import toast from "react-hot-toast";

const NotificationBell = ({ onModuleClick }) => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);

  // Fetch notifications periodically
  useEffect(() => {
    fetchNotifications();
    fetchUnreadCount();

    // Poll for new notifications every 5 seconds
    const interval = setInterval(() => {
      fetchUnreadCount();
      if (isOpen) {
        fetchNotifications();
      }
    }, 5000);

    return () => clearInterval(interval);
  }, [isOpen]);

  const fetchNotifications = async () => {
    try {
      const { data } = await api.get("/api/notifications");
      if (data && Array.isArray(data.notifications)) {
        setNotifications(data.notifications);
      }
    } catch (e) {
      console.error("Failed to fetch notifications", e);
    }
  };

  const fetchUnreadCount = async () => {
    try {
      const { data } = await api.get("/api/notifications/count");
      setUnreadCount(data.count || 0);
    } catch (e) {
      console.error("Failed to fetch unread count", e);
    }
  };

  const markAsRead = async (notificationId) => {
    try {
      await api.post(`/api/notifications/${notificationId}/read`);
      setNotifications(
        notifications.map((n) =>
          n.id === notificationId ? { ...n, isRead: true } : n
        )
      );
      setUnreadCount(Math.max(0, unreadCount - 1));
    } catch (e) {
      console.error("Failed to mark as read", e);
    }
  };

  const markAllAsRead = async () => {
    try {
      await api.post("/api/notifications/mark-all-read");
      setNotifications(notifications.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
      toast.success("All notifications marked as read");
    } catch (e) {
      console.error("Failed to mark all as read", e);
      toast.error("Failed to mark all as read");
    }
  };

  const clearAll = async () => {
    try {
      await api.delete("/api/notifications");
      setNotifications([]);
      setUnreadCount(0);
      toast.success("All notifications cleared");
    } catch (e) {
      console.error("Failed to clear notifications", e);
      toast.error("Failed to clear notifications");
    }
  };

  const handleNotificationClick = (notification) => {
    if (!notification.isRead) {
      markAsRead(notification.id);
    }

    // If a module click handler is provided, call it with the module code
    if (onModuleClick && notification.moduleCode) {
      setIsOpen(false);
      onModuleClick({
        code: notification.moduleCode,
        name: notification.moduleName,
        description: notification.message,
      });
    }
  };

  return (
    <div className="relative">
      {/* Bell Icon Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 hover:bg-gray-100 rounded-full transition-colors"
        title="Notifications"
      >
        <Bell size={20} className="text-gray-700" />
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center font-semibold">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Dropdown */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-xl border border-gray-200 z-50">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-gray-200">
            <h3 className="font-semibold text-gray-800">Notifications</h3>
            <div className="flex items-center gap-2">
              {notifications.length > 0 && (
                <>
                  <button
                    onClick={markAllAsRead}
                    className="text-xs text-blue-600 hover:text-blue-800"
                  >
                    Mark all read
                  </button>
                  <button
                    onClick={clearAll}
                    className="text-xs text-red-600 hover:text-red-800"
                  >
                    Clear all
                  </button>
                </>
              )}
              <button
                onClick={() => setIsOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={18} />
              </button>
            </div>
          </div>

          {/* Notification List */}
          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <Bell size={48} className="mx-auto mb-2 text-gray-300" />
                <p>No notifications yet</p>
                <p className="text-sm text-gray-400 mt-1">
                  Subscribe to modules to get notified about new uploads
                </p>
              </div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={`p-4 border-b border-gray-100 hover:bg-gray-50 cursor-pointer transition-colors ${
                    !notification.isRead ? "bg-blue-50" : ""
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p
                        className={`text-sm ${
                          !notification.isRead
                            ? "font-semibold text-gray-900"
                            : "text-gray-700"
                        }`}
                      >
                        {notification.message}
                      </p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-xs text-blue-600 font-medium">
                          {notification.moduleName}
                        </span>
                        <span className="text-xs text-gray-400">•</span>
                        <span className="text-xs text-gray-500">
                          {new Date(notification.timestamp).toLocaleString()}
                        </span>
                      </div>
                    </div>
                    {!notification.isRead && (
                      <div className="ml-2 w-2 h-2 bg-blue-500 rounded-full"></div>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Backdrop to close dropdown when clicking outside */}
      {isOpen && (
        <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
      )}
    </div>
  );
};

export default NotificationBell;
