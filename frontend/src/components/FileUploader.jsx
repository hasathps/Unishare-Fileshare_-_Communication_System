import React, { useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { Upload, File, X } from 'lucide-react'
import toast from 'react-hot-toast'
import axios from 'axios'

const FileUploader = () => {
  const [selectedFiles, setSelectedFiles] = useState([])
  const [uploading, setUploading] = useState(false)
  const [selectedModule, setSelectedModule] = useState('')
  const [uploaderName, setUploaderName] = useState('')

  const modules = ['IN3111', 'CS101', 'MATH201']

  const onDrop = (acceptedFiles) => {
    const newFiles = acceptedFiles.map(file => ({
      file,
      id: Math.random().toString(36).substr(2, 9),
      name: file.name,
      size: file.size,
      type: file.type
    }))
    setSelectedFiles(prev => [...prev, ...newFiles])
    toast.success(`${acceptedFiles.length} file(s) added`)
  }

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'text/plain': ['.txt'],
      'image/*': ['.png', '.jpg', '.jpeg', '.gif']
    },
    maxSize: 10 * 1024 * 1024 // 10MB
  })

  const removeFile = (fileId) => {
    setSelectedFiles(prev => prev.filter(file => file.id !== fileId))
    toast.success('File removed')
  }

  const handleUpload = async () => {
    if (!selectedModule || !uploaderName || selectedFiles.length === 0) {
      toast.error('Please fill all fields and select files')
      return
    }

    setUploading(true)
    
    try {
      // Send actual files with multipart form data
      const formData = new FormData()
      formData.append('module', selectedModule)
      formData.append('uploaderName', uploaderName)
      
      selectedFiles.forEach((fileObj, index) => {
        formData.append(`files`, fileObj.file)
      })

      // Upload to backend
      const response = await axios.post('http://localhost:8080/api/upload', formData, {
        headers: { 
          'Content-Type': 'multipart/form-data',
          'Accept': 'application/json'
        },
        timeout: 10000 // 10 second timeout
      })
      
      console.log('Upload response:', response.data)
      toast.success('Files uploaded successfully!')
      setSelectedFiles([])
      setUploaderName('')
      
    } catch (error) {
      console.error('Upload error:', error)
      if (error.response) {
        console.error('Error response:', error.response.data)
        toast.error('Upload failed: ' + (error.response.data?.error || error.response.statusText))
      } else {
        toast.error('Upload failed: ' + error.message)
      }
    } finally {
      setUploading(false)
    }
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return (
    <div className="max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-lg">
      <h2 className="text-2xl font-bold text-gray-800 mb-6">Upload Study Materials</h2>
      
      {/* Module Selection */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Select Module
        </label>
        <select
          value={selectedModule}
          onChange={(e) => setSelectedModule(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Choose a module...</option>
          {modules.map(module => (
            <option key={module} value={module}>{module}</option>
          ))}
        </select>
      </div>

      {/* Uploader Name */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Your Name
        </label>
        <input
          type="text"
          value={uploaderName}
          onChange={(e) => setUploaderName(e.target.value)}
          placeholder="Enter your name"
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {/* File Drop Zone */}
      <div
        {...getRootProps()}
        className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
          isDragActive
            ? 'border-blue-400 bg-blue-50'
            : 'border-gray-300 hover:border-gray-400'
        }`}
      >
        <input {...getInputProps()} />
        <Upload className="mx-auto h-12 w-12 text-gray-400 mb-4" />
        {isDragActive ? (
          <p className="text-blue-600">Drop the files here...</p>
        ) : (
          <div>
            <p className="text-gray-600 mb-2">
              Drag & drop files here, or click to select files
            </p>
            <p className="text-sm text-gray-500">
              Supports: PDF, DOC, DOCX, TXT, Images (max 10MB each)
            </p>
          </div>
        )}
      </div>

      {/* Selected Files */}
      {selectedFiles.length > 0 && (
        <div className="mt-4">
          <h3 className="text-lg font-medium text-gray-800 mb-2">Selected Files:</h3>
          <div className="space-y-2">
            {selectedFiles.map((fileObj) => (
              <div
                key={fileObj.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
              >
                <div className="flex items-center">
                  <File className="h-5 w-5 text-gray-500 mr-2" />
                  <div>
                    <p className="text-sm font-medium text-gray-800">{fileObj.name}</p>
                    <p className="text-xs text-gray-500">{formatFileSize(fileObj.size)}</p>
                  </div>
                </div>
                <button
                  onClick={() => removeFile(fileObj.id)}
                  className="text-red-500 hover:text-red-700"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Upload Button */}
      <button
        onClick={handleUpload}
        disabled={uploading || selectedFiles.length === 0 || !selectedModule || !uploaderName}
        className={`w-full mt-6 py-3 px-4 rounded-md font-medium transition-colors ${
          uploading || selectedFiles.length === 0 || !selectedModule || !uploaderName
            ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
            : 'bg-blue-600 text-white hover:bg-blue-700'
        }`}
      >
        {uploading ? 'Uploading...' : `Upload ${selectedFiles.length} File(s)`}
      </button>
    </div>
  )
}

export default FileUploader
