// WebSocket service for real-time communication
class SocketService {
  constructor() {
    this.socket = null
    this.isConnected = false
    this.messageHandlers = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
  }

  connect(username, module) {
    try {
      // Connect to WebSocket proxy server
      this.socket = new WebSocket('ws://localhost:8084/chat')
      
      this.socket.onopen = () => {
        console.log('Connected to chat server')
        this.isConnected = true
        this.reconnectAttempts = 0
        
        // Join the module
        this.send(`JOIN|${username}|${module}`)
        
        // Notify handlers
        this.notifyHandlers('connected', { username, module })
      }

      this.socket.onmessage = (event) => {
        this.handleMessage(event.data)
      }

      this.socket.onclose = () => {
        console.log('Disconnected from chat server')
        this.isConnected = false
        this.notifyHandlers('disconnected')
        
        // Attempt to reconnect
        this.attemptReconnect(username, module)
      }

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error)
        this.notifyHandlers('error', error)
      }

    } catch (error) {
      console.error('Failed to connect to chat server:', error)
    }
  }

  handleMessage(data) {
    const parts = data.split('|')
    const command = parts[0]

    switch (command) {
      case 'MESSAGE':
        if (parts.length >= 5) {
          const [, user, module, message, timestamp] = parts
          this.notifyHandlers('message', { user, module, message, timestamp })
        }
        break
      
      case 'NOTIFICATION':
        if (parts.length >= 4) {
          const [, type, title, message, module] = parts
          this.notifyHandlers('notification', { type, title, message, module })
        }
        break
      
      case 'USER_LIST':
        if (parts.length >= 2) {
          const users = parts[1].split(',')
          this.notifyHandlers('userList', users)
        }
        break
    }
  }

  send(message) {
    if (this.socket && this.isConnected) {
      this.socket.send(message)
    }
  }

  sendMessage(username, module, message) {
    this.send(`MESSAGE|${username}|${module}|${message}`)
  }

  disconnect() {
    if (this.socket) {
      this.send('LEAVE')
      this.socket.close()
      this.socket = null
      this.isConnected = false
    }
  }

  attemptReconnect(username, module) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      
      setTimeout(() => {
        this.connect(username, module)
      }, 2000 * this.reconnectAttempts) // Exponential backoff
    }
  }

  // Event handler management
  on(event, handler) {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, [])
    }
    this.messageHandlers.get(event).push(handler)
  }

  off(event, handler) {
    if (this.messageHandlers.has(event)) {
      const handlers = this.messageHandlers.get(event)
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  notifyHandlers(event, data) {
    if (this.messageHandlers.has(event)) {
      this.messageHandlers.get(event).forEach(handler => {
        try {
          handler(data)
        } catch (error) {
          console.error('Error in message handler:', error)
        }
      })
    }
  }
}

// Create singleton instance
const socketService = new SocketService()

export default socketService