# UniShare - University File Sharing & Communication System

A modern web-based file sharing platform designed specifically for university students to share study materials, notes, and learning resources.

## 🚀 Features

- **File Upload**: Upload PDFs, documents, and images to specific course modules
- **Module Organization**: Organize files by university courses (IN3111, CS101, MATH201, etc.)
- **Modern UI**: Clean, responsive interface built with React and Tailwind CSS
- **Real-time Feedback**: Toast notifications for upload success/failure
- **Drag & Drop**: Intuitive file upload with drag-and-drop support

## 🛠 Technology Stack

### Frontend
- **React.js** - Modern UI framework
- **Tailwind CSS** - Utility-first CSS framework
- **Vite** - Fast build tool and dev server
- **Axios** - HTTP client for API requests
- **React Hot Toast** - Beautiful notifications
- **Lucide React** - Modern icon library
- **React Dropzone** - Drag-and-drop file uploads

### Backend
- **Core Java** - Pure Java implementation (no external dependencies)
- **HTTP Server** - Built-in Java HTTP server
- **File System Storage** - Direct file storage (no database required)

## 📁 Project Structure

```
Unishare/
├── frontend/                 # React frontend application
│   ├── src/
│   │   ├── components/      # React components
│   │   │   └── FileUploader.jsx
│   │   ├── App.jsx          # Main application component
│   │   └── index.css        # Global styles
│   ├── package.json         # Frontend dependencies
│   └── vite.config.js      # Vite configuration
├── backend/                 # Java backend server
│   ├── src/main/java/com/unishare/
│   │   ├── UniShareServer.java      # Main server class
│   │   ├── controller/               # HTTP request handlers
│   │   │   ├── FileController.java  # File upload handling
│   │   │   └── ModuleController.java # Module management
│   │   ├── service/                 # Business logic
│   │   │   ├── FileService.java     # File operations
│   │   │   └── ModuleService.java   # Module operations
│   │   ├── model/                   # Data models
│   │   │   └── FileInfo.java        # File metadata
│   │   ├── util/                    # Utilities
│   │   │   └── CORSFilter.java      # CORS handling
│   │   └── config/                  # Configuration
│   │       └── ServerConfig.java    # Server settings
│   ├── build.bat                    # Windows build script
│   ├── build.sh                     # Linux/Mac build script
│   └── README.md                    # Backend documentation
└── uploads/                  # File storage directory
    ├── IN3111/              # Information Systems files
    ├── CS101/               # Computer Science files
    └── MATH201/             # Mathematics files
```

## 🚀 Quick Start

### Prerequisites
- **Node.js** (v16 or higher)
- **Java** (v11 or higher)
- **Git**

### Frontend Setup

1. **Navigate to frontend directory:**
   ```bash
   cd frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```
   
   Frontend will be available at: `http://localhost:3000`

### Backend Setup

1. **Navigate to backend directory:**
   ```bash
   cd backend
   ```

2. **Compile Java source:**
   ```bash
   # Windows
   build.bat
   
   # Linux/Mac
   chmod +x build.sh
   ./build.sh
   ```

3. **Start the server:**
   ```bash
   java -cp build\classes com.unishare.UniShareServer
   ```
   
   Backend will be available at: `http://localhost:8080`

## 📡 API Endpoints

### File Operations
- `POST /api/upload` - Upload files to a module
- `GET /api/modules` - Get list of available modules
- `GET /api/health` - Health check endpoint

### Example Upload Request
```javascript
const formData = new FormData();
formData.append('module', 'IN3111');
formData.append('uploaderName', 'John Doe');
formData.append('files', file);

fetch('http://localhost:8080/api/upload', {
  method: 'POST',
  body: formData
});
```

## 🎯 Available Modules

- **IN3111** - Information Systems
- **CS101** - Computer Science Fundamentals  
- **MATH201** - Advanced Mathematics
- **PHYS202** - Physics II
- **CHEM103** - Chemistry Fundamentals

## 📝 Usage

1. **Start both frontend and backend servers**
2. **Open your browser** to `http://localhost:3000`
3. **Select a module** from the dropdown
4. **Enter your name** as the uploader
5. **Drag and drop files** or click to select files
6. **Click Upload** to share your files

## 🔧 Configuration

### Backend Settings (ServerConfig.java)
- **Port**: 8080
- **Upload Directory**: `uploads/`
- **Max File Size**: 10MB
- **Allowed Extensions**: pdf, doc, docx, txt, png, jpg, jpeg, gif

### Frontend Settings (vite.config.js)
- **Port**: 3000
- **Auto-open**: Enabled
- **Hot Reload**: Enabled

## 🛠 Development

### Adding New Modules
1. Add module name to `ServerConfig.AVAILABLE_MODULES`
2. Update `ModuleService.getAvailableModules()`
3. Create directory in `uploads/` folder

### Adding New File Types
1. Update `ServerConfig.ALLOWED_FILE_EXTENSIONS`
2. Update `FileService.ALLOWED_EXTENSIONS`

## 🐛 Troubleshooting

### Common Issues

**Port Already in Use:**
```bash
# Find process using port 8080
netstat -ano | findstr :8080
# Kill process (replace PID)
taskkill /PID <PID> /F
```

**Frontend Build Issues:**
```bash
# Clear node modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

**Backend Compilation Errors:**
- Ensure Java 11+ is installed
- Check file paths and package declarations
- Verify all dependencies are in classpath

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For support, email hasathps@example.com or create an issue in this repository.

---

**Built with ❤️ for university students by university students**
