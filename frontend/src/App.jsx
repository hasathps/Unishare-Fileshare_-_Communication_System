import React from 'react'
import FileUploader from './components/FileUploader'
import { Toaster } from 'react-hot-toast'

function App() {
  return (
    <div className="min-h-screen bg-gray-100">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-4xl font-bold text-center text-blue-600 mb-8">
          UniShare
        </h1>
        <p className="text-center text-gray-600 mb-8">
          University File Sharing Platform
        </p>
        
        {/* File Uploader Component */}
        <FileUploader />
        
        <div className="max-w-md mx-auto mt-8 bg-white rounded-lg shadow-md p-6">
          <h2 className="text-2xl font-semibold text-gray-800 mb-4">
            Welcome to UniShare
          </h2>
          <p className="text-gray-600 mb-4">
            Share study materials, notes, and resources with your university peers.
          </p>
        </div>
      </div>
      
      {/* Toast Notifications */}
      <Toaster position="top-right" />
    </div>
  )
}

export default App
