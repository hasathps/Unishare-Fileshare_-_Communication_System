import React, { useState, useEffect } from 'react'
import { Bell, X, Upload, Download, MessageSquare } from 'lucide-react'

const NotificationPanel = ({ selectedModule }) => {
  const [notifications, setNotifications] = useState([])
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    // Listen for notifications from the chat system
    const handleNotification = (event) => {
      if (event.detail && event.detail.module === selectedModule) {
        addNotification(event.detail)
      }
    }

    window.addEventListener('chatNotification', handleNotification)
    return () => window.removeEventListener('chatNotification', handleNotification)
  }, [selectedModule])

  const addNotification = (notification) => {
    const newNotification = {
      id: Date.now(),
      ...notification,
      timestamp: new Date().toLocaleTimeString('en-US', { 
        hour12: false, 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
    
    setNotifications(prev => [newNotification, ...prev.slice(0, 9)]) // Keep last 10
    setIsVisible(true)
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
      removeNotification(newNotification.id)
    }, 5000)
  }

  const removeNotification = (id) => {
    setNotifications(prev => prev.filter(n => n.id !== id))
  }

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'upload':
        return <Upload size={16} className="text-blue-500" />
      case 'download':
        return <Download size={16} className="text-green-500" />
      case 'message':
        return <MessageSquare size={16} className="text-purple-500" />
      default:
        return <Bell size={16} className="text-gray-500" />
    }
  }

  const getNotificationColor = (type) => {
    switch (type) {
      case 'upload':
        return 'border-l-blue-500 bg-blue-50'
      case 'download':
        return 'border-l-green-500 bg-green-50'
      case 'message':
        return 'border-l-purple-500 bg-purple-50'
      default:
        return 'border-l-gray-500 bg-gray-50'
    }
  }

  if (notifications.length === 0) {
    return null
  }

  return (
    <div className="fixed top-4 right-4 z-50 space-y-2 max-w-sm">
      {notifications.map((notification) => (
        <div
          key={notification.id}
          className={`p-4 rounded-lg shadow-lg border-l-4 ${getNotificationColor(notification.type)} 
                     transform transition-all duration-300 ease-in-out`}
        >
          <div className="flex items-start justify-between">
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 mt-0.5">
                {getNotificationIcon(notification.type)}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900">
                  {notification.title}
                </p>
                <p className="text-sm text-gray-600 mt-1">
                  {notification.message}
                </p>
                <p className="text-xs text-gray-500 mt-2">
                  {notification.module} • {notification.timestamp}
                </p>
              </div>
            </div>
            <button
              onClick={() => removeNotification(notification.id)}
              className="flex-shrink-0 ml-2 text-gray-400 hover:text-gray-600"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

// Helper function to trigger notifications from other components
export const triggerNotification = (type, title, message, module) => {
  const event = new CustomEvent('chatNotification', {
    detail: { type, title, message, module }
  })
  window.dispatchEvent(event)
}

export default NotificationPanel