import React, { useState, useEffect, useRef } from 'react'
import { MessageCircle, Send, Users } from 'lucide-react'

const ChatBox = ({ selectedModule, username }) => {
  const [messages, setMessages] = useState([])
  const [messageText, setMessageText] = useState('')
  const [onlineUsers, setOnlineUsers] = useState([])
  const [isConnected, setIsConnected] = useState(false)
  const wsRef = useRef(null)
  const messagesEndRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  useEffect(() => {
    if (selectedModule && username) {
      connectToChat()
    }
    
    return () => {
      if (wsRef.current) {
        wsRef.current.close()
      }
    }
  }, [selectedModule, username])

  const connectToChat = () => {
    try {
      // Connect to WebSocket proxy (since browsers can't use raw TCP)
      wsRef.current = new WebSocket('ws://localhost:8084/chat')
      
      wsRef.current.onopen = () => {
        setIsConnected(true)
        // Join the module
        wsRef.current.send(`JOIN|${username}|${selectedModule}`)
        console.log('Connected to chat server')
      }

      wsRef.current.onmessage = (event) => {
        const data = event.data.split('|')
        if (data[0] === 'MESSAGE' && data.length >= 5) {
          const [, user, module, message, timestamp] = data
          setMessages(prev => [...prev, { user, module, message, timestamp }])
        }
      }

      wsRef.current.onclose = () => {
        setIsConnected(false)
        console.log('Disconnected from chat server')
      }

      wsRef.current.onerror = (error) => {
        console.error('WebSocket error:', error)
        setIsConnected(false)
      }
    } catch (error) {
      console.error('Failed to connect to chat:', error)
    }
  }

  const sendMessage = () => {
    if (!messageText.trim() || !isConnected) return

    const message = `MESSAGE|${username}|${selectedModule}|${messageText}`
    wsRef.current.send(message)
    
    // Add own message to display
    const timestamp = new Date().toLocaleTimeString('en-US', { 
      hour12: false, 
      hour: '2-digit', 
      minute: '2-digit' 
    })
    setMessages(prev => [...prev, { 
      user: username, 
      module: selectedModule, 
      message: messageText, 
      timestamp 
    }])
    
    setMessageText('')
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      sendMessage()
    }
  }

  if (!selectedModule || !username) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="text-center text-gray-500">
          <MessageCircle size={48} className="mx-auto mb-4 text-gray-300" />
          <p>Select a module and enter your name to start chatting</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow-md h-[500px] flex flex-col">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div className="flex items-center">
          <MessageCircle className="mr-2 text-blue-600" size={20} />
          <h3 className="font-semibold text-gray-800">{selectedModule} Chat</h3>
        </div>
        <div className="flex items-center">
          <div className={`w-2 h-2 rounded-full mr-2 ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
          <span className="text-sm text-gray-600">
            {isConnected ? 'Connected' : 'Disconnected'}
          </span>
        </div>
      </div>

      <div className="flex-1 flex">
        {/* Messages */}
        <div className="flex-1 p-4 overflow-y-auto">
          <div className="space-y-3">
            {messages
              .filter(msg => msg.module === selectedModule)
              .map((msg, index) => (
              <div key={index} className="flex items-start space-x-3">
                <div className="flex-shrink-0">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                    msg.user === 'System' ? 'bg-gray-500' : 
                    msg.user === username ? 'bg-blue-500' : 'bg-green-500'
                  }`}>
                    <span className="text-white text-sm font-medium">
                      {msg.user.charAt(0)}
                    </span>
                  </div>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-2">
                    <span className={`text-sm font-medium ${
                      msg.user === username ? 'text-blue-600' : 'text-gray-900'
                    }`}>
                      {msg.user}
                    </span>
                    <span className="text-xs text-gray-500">{msg.timestamp}</span>
                  </div>
                  <p className="text-sm text-gray-700 mt-1">{msg.message}</p>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        </div>

        {/* Online Users */}
        <div className="w-48 border-l border-gray-200 p-3">
          <h4 className="text-sm font-semibold text-gray-800 mb-3 flex items-center">
            <Users className="mr-1 text-green-600" size={16} />
            Online
          </h4>
          <div className="space-y-2">
            {onlineUsers.map((user, index) => (
              <div key={index} className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                <span className="text-sm text-gray-700">{user}</span>
              </div>
            ))}
            {onlineUsers.length === 0 && (
              <p className="text-xs text-gray-500">No users online</p>
            )}
          </div>
        </div>
      </div>

      {/* Message Input */}
      <div className="border-t border-gray-200 p-3">
        <div className="flex space-x-2">
          <input
            type="text"
            placeholder="Type your message..."
            value={messageText}
            onChange={(e) => setMessageText(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={!isConnected}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50"
          />
          <button 
            onClick={sendMessage}
            disabled={!messageText.trim() || !isConnected}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  )
}

export default ChatBox