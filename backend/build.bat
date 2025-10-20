@echo off
echo 🚀 Building UniShare Backend...

echo 📁 Creating class directories...
if not exist "build\classes" mkdir build\classes
if not exist "build\lib" mkdir build\lib

echo 🔨 Compiling Java source files...
javac -d build\classes -cp "src\main\java" src\main\java\com\unishare\*.java src\main\java\com\unishare\controller\*.java src\main\java\com\unishare\service\*.java src\main\java\com\unishare\model\*.java src\main\java\com\unishare\util\*.java src\main\java\com\unishare\config\*.java

if %ERRORLEVEL% EQU 0 (
    echo ✅ Compilation successful!
    echo 🎯 To run the server:
    echo    java -cp build\classes com.unishare.UniShareServer
) else (
    echo ❌ Compilation failed!
    echo Please check the error messages above.
)

pause
