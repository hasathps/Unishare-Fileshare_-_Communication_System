import React from 'react'
import { MessageCircle, Send, Users } from 'lucide-react'

const ChatSection = () => {
  // Mock data for demonstration
  const messages = [
    { id: 1, user: 'John Doe', message: 'Anyone have notes for CS101 assignment?', time: '2:30 PM', module: 'CS101' },
    { id: 2, user: 'Jane Smith', message: 'I can share mine! Check the files section.', time: '2:32 PM', module: 'CS101' },
    { id: 3, user: 'Mike Johnson', message: 'Thanks Jane! Really helpful.', time: '2:35 PM', module: 'CS101' },
    { id: 4, user: 'Sarah Wilson', message: 'Study group for MATH201 tomorrow?', time: '3:15 PM', module: 'MATH201' }
  ]

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md h-[600px] flex flex-col">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-2xl font-semibold text-gray-800 flex items-center">
            <MessageCircle className="mr-2 text-blue-600" size={24} />
            Chat & Discussion
          </h2>
          <p className="text-gray-600 mt-2">Connect with your peers and discuss course materials</p>
        </div>

        <div className="flex-1 flex">
          {/* Chat Messages */}
          <div className="flex-1 p-6 overflow-y-auto">
            <div className="space-y-4">
              {messages.map((msg) => (
                <div key={msg.id} className="flex items-start space-x-3">
                  <div className="flex-shrink-0">
                    <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                      <span className="text-white text-sm font-medium">
                        {msg.user.charAt(0)}
                      </span>
                    </div>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm font-medium text-gray-900">{msg.user}</span>
                      <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                        {msg.module}
                      </span>
                      <span className="text-xs text-gray-500">{msg.time}</span>
                    </div>
                    <p className="text-sm text-gray-700 mt-1">{msg.message}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Online Users */}
          <div className="w-64 border-l border-gray-200 p-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center">
              <Users className="mr-2 text-green-600" size={18} />
              Online Users
            </h3>
            <div className="space-y-2">
              {['John Doe', 'Jane Smith', 'Mike Johnson', 'Sarah Wilson'].map((user, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <span className="text-sm text-gray-700">{user}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Message Input */}
        <div className="border-t border-gray-200 p-4">
          <div className="flex space-x-4">
            <select className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
              <option value="">Select Module</option>
              <option value="IN3111">IN3111</option>
              <option value="CS101">CS101</option>
              <option value="MATH201">MATH201</option>
            </select>
            <input
              type="text"
              placeholder="Type your message..."
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 flex items-center">
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
