import React, { useState, useEffect, useRef } from 'react'
import { MessageCircle, Send, Users } from 'lucide-react'

const ChatSection = () => {
  const [messages, setMessages] = useState([])
  const [onlineUsers, setOnlineUsers] = useState([])
  const [selectedModule, setSelectedModule] = useState('')
  const [messageText, setMessageText] = useState('')
  const [userName, setUserName] = useState('')
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
    if (selectedModule && userName) {
      if (wsRef.current) {
        wsRef.current.close()
      }
      // Load saved messages for the new module
      const savedMessages = localStorage.getItem(`chat_${selectedModule}`)
      setMessages(savedMessages ? JSON.parse(savedMessages) : [])
      setTimeout(() => connectWebSocket(), 100) // Small delay to ensure clean connection
    }
    
    return () => {
      if (wsRef.current) {
        wsRef.current.close()
      }
    }
  }, [selectedModule, userName])
  
  const connectWebSocket = () => {
    try {
      wsRef.current = new WebSocket('ws://localhost:8084')
      
      wsRef.current.onopen = () => {
        setIsConnected(true)
        console.log('Connected to WebSocket chat server')
        // Send JOIN message after a small delay to ensure connection is stable
        setTimeout(() => {
          if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(`JOIN|${userName}|${selectedModule}`)
          }
        }, 100)
      }
      
      wsRef.current.onmessage = (event) => {
        console.log('Received WebSocket message:', event.data)
        const data = event.data.split('|')
        if (data[0] === 'MESSAGE' && data.length >= 5) {
          const [, user, module, message, timestamp] = data
          console.log('Parsed message:', { user, module, message, timestamp })
          if (module === selectedModule) {
            setMessages(prev => {
              const newMessages = [...prev, { user, module, message, timestamp }]
              console.log('Updated messages:', newMessages)
              // Auto-scroll to bottom after state update
              setTimeout(() => scrollToBottom(), 100)
              return newMessages
            })
          }
        }
      }
      
      wsRef.current.onclose = (event) => {
        setIsConnected(false)
        console.log('Disconnected from WebSocket chat server', event.code, event.reason)
        // Don't auto-reconnect to prevent infinite loops
      }
      
      wsRef.current.onerror = (error) => {
        console.error('WebSocket error:', error)
        setIsConnected(false)
      }
    } catch (error) {
      console.error('Failed to connect to WebSocket:', error)
    }
  }
  
  const sendMessage = () => {
    if (!messageText.trim() || !selectedModule || !userName.trim() || !isConnected) return
    
    const message = `MESSAGE|${userName}|${selectedModule}|${messageText}`
    wsRef.current.send(message)
    
    // Add own message to display
    const timestamp = new Date().toLocaleTimeString('en-US', { 
      hour12: false, 
      hour: '2-digit', 
      minute: '2-digit' 
    })
    setMessages(prev => {
      const newMessages = [...prev, { 
        user: userName, 
        module: selectedModule, 
        message: messageText, 
        timestamp 
      }]
      // Auto-scroll to bottom after sending message
      setTimeout(() => scrollToBottom(), 100)
      return newMessages
    })
    
    setMessageText('')
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md h-[600px] flex flex-col">
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-semibold text-gray-800 flex items-center">
                <MessageCircle className="mr-2 text-blue-600" size={24} />
                Chat & Discussion
              </h2>
              <p className="text-gray-600 mt-2">Connect with your peers and discuss course materials</p>
            </div>
            <div className="flex items-center">
              <div className={`w-2 h-2 rounded-full mr-2 ${
                isConnected ? 'bg-green-500' : 'bg-red-500'
              }`}></div>
              <span className="text-sm text-gray-600">
                {isConnected ? 'Connected' : 'Disconnected'}
              </span>
            </div>
          </div>
        </div>

        <div className="flex-1 flex">
          {/* Chat Messages */}
          <div className="flex-1 p-6 overflow-y-auto">
            {!selectedModule ? (
              <div className="flex items-center justify-center h-full text-gray-500">
                <p>Select a module to start chatting</p>
              </div>
            ) : (
              <div className="space-y-4">
                {messages.map((msg, index) => (
                  <div key={index} className="flex items-start space-x-3">
                    <div className="flex-shrink-0">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                        msg.user === userName ? 'bg-blue-500' : 'bg-green-500'
                      }`}>
                        <span className="text-white text-sm font-medium">
                          {msg.user.charAt(0)}
                        </span>
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2">
                        <span className={`text-sm font-medium ${
                          msg.user === userName ? 'text-blue-600' : 'text-gray-900'
                        }`}>{msg.user}</span>
                        <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                          {msg.module}
                        </span>
                        <span className="text-xs text-gray-500">{msg.timestamp}</span>
                      </div>
                      <p className="text-sm text-gray-700 mt-1">{msg.message}</p>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Online Users */}
          <div className="w-64 border-l border-gray-200 p-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center">
              <Users className="mr-2 text-green-600" size={18} />
              Online Users
            </h3>
            <div className="space-y-2">
              {onlineUsers.map((user, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <span className="text-sm text-gray-700">{user}</span>
                </div>
              ))}
              {onlineUsers.length === 0 && (
                <p className="text-sm text-gray-500">No users online</p>
              )}
            </div>
          </div>
        </div>

        {/* Message Input */}
        <div className="border-t border-gray-200 p-4">
          <div className="flex space-x-2 mb-3">
            <input
              type="text"
              placeholder="Your name..."
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <select 
              value={selectedModule}
              onChange={(e) => setSelectedModule(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">Select Module</option>
              <option value="IN3111">IN3111</option>
              <option value="CS101">CS101</option>
              <option value="MATH201">MATH201</option>
            </select>
          </div>
          <div className="flex space-x-2">
            <input
              type="text"
              placeholder="Type your message..."
              value={messageText}
              onChange={(e) => setMessageText(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <button 
              onClick={sendMessage}
              disabled={!messageText.trim() || !selectedModule || !userName.trim() || !isConnected}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Send size={16} className="mr-1" />
              Send
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ChatSection
