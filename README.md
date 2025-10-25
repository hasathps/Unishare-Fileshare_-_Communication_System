# UniShare - University File Sharing & Communication System

A modern web-based file sharing platform designed specifically for university students to share study materials, notes, and learning resources.

## 🚀 Features

### 📁 Files Management
- **File Library**: Browse and download shared study materials
- **Search & Filter**: Find files by name, module, or uploader
- **File Preview**: View file details before downloading
- **Module Organization**: Organize files by university courses

### 📤 Upload Service
- **Multiple File Upload**: Upload multiple files simultaneously
- **Module Selection**: Choose specific course modules (IN3111, CS101, MATH201)
- **Drag & Drop**: Intuitive file upload with drag-and-drop support
- **File Validation**: Automatic file type and size validation
- **Real-time Feedback**: Toast notifications for upload success/failure

### 💬 Chat & Discussion
- **Module-based Chat**: Discuss course materials by module
- **Real-time Messaging**: Connect with peers instantly
- **Online Users**: See who's currently active
- **Study Groups**: Organize study sessions and discussions

### 📊 System Monitor
- **Upload Statistics**: Track file uploads and downloads
- **User Activity**: Monitor system usage and engagement
- **Module Analytics**: View statistics per course module
- **Performance Metrics**: System health and performance monitoring

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
│   │   │   ├── Navbar.jsx           # Modern navigation bar
│   │   │   ├── FileUploader.jsx    # Upload service component
│   │   │   ├── FilesSection.jsx    # Files library component
│   │   │   ├── ChatSection.jsx     # Chat & discussion component
│   │   │   └── MonitorSection.jsx  # System monitoring component
│   │   ├── App.jsx          # Main application component
│   │   └── index.css        # Global styles
│   ├── package.json         # Frontend dependencies
│   ├── vite.config.js      # Vite configuration
│   ├── tailwind.config.js  # Tailwind CSS configuration
│   └── postcss.config.js   # PostCSS configuration
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
│   │   └── util/                    # Utilities
│   │       └── CORSFilter.java      # CORS handling
│   ├── uploads/                     # File storage directory
│   │   ├── IN3111/                 # Module-specific folders
│   │   ├── CS101/
│   │   └── MATH201/
│   └── build/classes/               # Compiled Java classes
├── .gitignore               # Git ignore rules
└── README.md                # Main documentation
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

### Getting Started
1. **Start both frontend and backend servers** (see Quick Start section)
2. **Open your browser** to `http://localhost:3000`
3. **Navigate between sections** using the modern navbar:
   - **Files**: Browse and download shared materials
   - **Upload**: Share your study materials
   - **Chat**: Discuss with fellow students
   - **Monitor**: View system statistics

### Uploading Files
1. **Click on "Upload"** in the navigation bar
2. **Select a module** from the dropdown (IN3111, CS101, MATH201)
3. **Enter your name** as the uploader
4. **Drag and drop files** or click to select multiple files
5. **Click Upload** to share your files
6. **Files will be saved** to the correct module folder

### Browsing Files
1. **Click on "Files"** in the navigation bar
2. **Search for files** using the search bar
3. **Filter by module** using the dropdown
4. **View file details** and download files
5. **See uploader information** and file sizes

### Chatting
1. **Click on "Chat"** in the navigation bar
2. **Select a module** for your discussion
3. **Type your message** and send
4. **See online users** in the sidebar
5. **Participate in module-based discussions**

### Monitoring
1. **Click on "Monitor"** in the navigation bar
2. **View upload statistics** and user activity
3. **Check module analytics** for each course
4. **Monitor system performance** and health

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
See [ADDING_MODULES.md](ADDING_MODULES.md) for detailed instructions on adding new course modules.

### Adding New File Types
1. Update `FileService.ALLOWED_EXTENSIONS` in the backend
2. Update the file type validation in `FileUploader.jsx` frontend component
3. Test with the new file types to ensure proper handling

### Frontend Development
- **Components**: All UI components are in `frontend/src/components/`
- **Styling**: Uses Tailwind CSS with custom configurations
- **State Management**: React hooks for local state management
- **API Integration**: Axios for HTTP requests to backend

### Backend Development
- **Pure Java**: No external dependencies, uses only Java standard library
- **HTTP Server**: Built-in `com.sun.net.httpserver.HttpServer`
- **File Storage**: Direct filesystem storage in `uploads/` directory
- **CORS Support**: Built-in CORS handling for frontend communication

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
