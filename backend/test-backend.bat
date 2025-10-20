@echo off
echo 🧪 Testing UniShare Backend...

echo 📡 Testing health endpoint...
curl -s http://localhost:8080/api/health

echo.
echo 📋 Testing modules endpoint...
curl -s http://localhost:8080/api/modules

echo.
echo 📁 Testing file list endpoint...
curl -s "http://localhost:8080/api/files?module=IN3111"

echo.
echo ✅ Backend test complete!
echo 🌐 Server should be running on: http://localhost:8080
echo 📁 Upload directory: uploads/
echo 💬 Chat endpoint: http://localhost:8080/api/chat

pause
