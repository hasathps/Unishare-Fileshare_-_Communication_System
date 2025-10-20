# UniShare Backend

Core Java HTTP server for the UniShare university file sharing platform.

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- No external dependencies required (pure Core Java)

### Building and Running

#### Windows:
```bash
cd backend
build.bat
java -cp build\classes com.unishare.UniShareServer
```

#### Linux/Mac:
```bash
cd backend
chmod +x build.sh
./build.sh
java -cp build/classes com.unishare.UniShareServer
```

### Manual Compilation:
```bash
# Create directories
mkdir -p build/classes

# Compile
javac -d build/classes -cp "src/main/java" src/main/java/com/unishare/**/*.java

# Run
java -cp build/classes com.unishare.UniShareServer
```

## 🌐 Server Endpoints

### File Operations
- `POST /api/upload` - Upload files to a module
- `GET /api/download?module=MODULE&filename=FILE` - Download a file
- `GET /api/files?module=MODULE` - Get list of files in a module
- `GET /uploads/MODULE/FILE` - Serve static files

### Module Operations
- `GET /api/modules` - Get list of available modules
- `GET /api/modules/MODULE` - Get files for specific module

### Chat Operations
- `GET /api/chat` - Chat service status
- `POST /api/chat` - Send chat message
- `GET /chat` - WebSocket endpoint (future)

### System
- `GET /api/health` - Health check

## 📁 Project Structure

```
backend/
├── src/main/java/com/unishare/
│   ├── UniShareServer.java          ← Main server class
│   ├── controller/
│   │   ├── FileController.java      ← File upload/download handling
│   │   ├── ModuleController.java    ← Module management
│   │   └── ChatController.java      ← Chat functionality
│   ├── service/
│   │   ├── FileService.java         ← File business logic
│   │   ├── ModuleService.java       ← Module business logic
│   │   └── ChatService.java         ← Chat business logic
│   ├── model/
│   │   └── FileInfo.java            ← File data model
│   ├── util/
│   │   └── CORSFilter.java          ← CORS handling
│   └── config/
│       └── ServerConfig.java         ← Server configuration
├── build.bat                        ← Windows build script
├── build.sh                         ← Linux/Mac build script
└── README.md                        ← This file
```

## 🔧 Configuration

### Server Settings (ServerConfig.java)
- **Port**: 8080
- **Upload Directory**: `uploads/`
- **Max File Size**: 10MB
- **Allowed Extensions**: pdf, doc, docx, txt, png, jpg, jpeg, gif
- **Available Modules**: IN3111, CS101, MATH201, PHYS202, CHEM103

### File Storage
Files are stored in the `uploads/` directory with the following structure:
```
uploads/
├── IN3111/
├── CS101/
├── MATH201/
├── PHYS202/
└── CHEM103/
```

## 🎯 Features

### ✅ Implemented
- **File Upload** - Multipart form data handling
- **File Download** - Direct file serving
- **File Listing** - Get files by module
- **Module Management** - Available modules
- **CORS Support** - Cross-origin requests
- **Health Check** - Server status endpoint

### 🚧 Future Enhancements
- **WebSocket Chat** - Real-time messaging
- **User Authentication** - Login system
- **File Metadata** - Enhanced file information
- **Activity Logging** - User action tracking
- **File Validation** - Enhanced security

## 🔌 Frontend Integration

The backend is designed to work with the React frontend:

### File Upload (Frontend → Backend)
```javascript
const formData = new FormData();
formData.append('module', selectedModule);
formData.append('uploaderName', uploaderName);
formData.append('files', file);

fetch('http://localhost:8080/api/upload', {
  method: 'POST',
  body: formData
});
```

### File Download
```javascript
window.open(`http://localhost:8080/api/download?module=${module}&filename=${filename}`);
```

### Get File List
```javascript
fetch(`http://localhost:8080/api/files?module=${module}`)
  .then(response => response.json())
  .then(data => console.log(data.files));
```

## 🛠 Development

### Adding New Endpoints
1. Create controller method in appropriate controller class
2. Add route registration in `UniShareServer.java`
3. Implement business logic in service class
4. Update this README

### Adding New Modules
1. Add module name to `ServerConfig.AVAILABLE_MODULES`
2. Update `ModuleService.getAvailableModules()`
3. Create directory in `uploads/` folder

## 🐛 Troubleshooting

### Common Issues

**Port Already in Use:**
```bash
# Find process using port 8080
netstat -ano | findstr :8080
# Kill process (replace PID)
taskkill /PID <PID> /F
```

**Compilation Errors:**
- Ensure Java 11+ is installed
- Check file paths and package declarations
- Verify all dependencies are in classpath

**File Upload Issues:**
- Check `uploads/` directory permissions
- Verify file size limits
- Check allowed file extensions

## 📝 Notes

- **No Database**: Files stored directly on filesystem
- **No External Dependencies**: Pure Core Java implementation
- **CORS Enabled**: Allows frontend requests from any origin
- **Thread Pool**: 10 concurrent request handlers
- **File Validation**: Basic file type and size checking

## 🎉 Success!

When the server starts successfully, you'll see:
```
🚀 Starting UniShare Backend Server...
✅ UniShare Server started successfully!
🌐 Server running on: http://localhost:8080
📁 File uploads will be saved to: ./uploads/
💬 WebSocket chat available on: ws://localhost:8080/chat
```

Your backend is now ready to handle file uploads from your React frontend! 🎯
