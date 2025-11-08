@echo off
setlocal enabledelayedexpansion

REM -----------------------------------------------------------------------------
REM UniShare backend helper script
REM - Checks for an existing process on port 8080 and terminates it (optional)
REM - Compiles the Java sources
REM - Launches the server with the PostgreSQL JDBC driver
REM -----------------------------------------------------------------------------

set PORT=8080
set DRIVER_JAR=lib\postgresql-42.7.3.jar
set BUILD_DIR=build\classes
set SECRET_FILE=config\neon-db-url.txt
set DEFAULT_DB_URL=postgresql://neondb_owner:npg_ZThxUb3LnBj2@ep-still-recipe-a1efuwr6-pooler.ap-southeast-1.aws.neon.tech/Unishare?sslmode=require^&channel_binding=require

if not exist "config" (
    mkdir "config" >nul 2>&1
)

echo.
echo === UniShare backend launcher ===
echo.

REM Ensure JDBC driver exists
if not exist "%DRIVER_JAR%" (
    echo [ERROR] PostgreSQL driver not found at %DRIVER_JAR%.
    echo Download it with:
    echo   powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar' -OutFile 'lib\\postgresql-42.7.3.jar'"
    exit /b 1
)

REM Load NEON_DB_URL from optional secret file if not already set
if "%NEON_DB_URL%"=="" (
    if exist "%SECRET_FILE%" (
        for /f "usebackq tokens=* delims=" %%A in ("%SECRET_FILE%") do (
            set "NEON_DB_URL=%%A"
        )
    )
)

REM Fallback to default connection string baked into this script (contains credentials)
if "%NEON_DB_URL%"=="" (
    set "NEON_DB_URL=%DEFAULT_DB_URL%"
)

REM Check for NEON_DB_URL env var
if "%NEON_DB_URL%"=="" (
    echo [ERROR] NEON_DB_URL environment variable is not set.
    echo Either:
    echo   1. setx NEON_DB_URL "postgresql://user:password@host/database?sslmode=require" ^& open a new terminal, or
    echo   2. create %SECRET_FILE% containing only the connection string.
    exit /b 1
)

REM Terminate any process already using port 8080 (optional)
echo Checking if anything is listening on port %PORT% ...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr :%PORT% ^| findstr LISTENING') do (
    echo Found PID %%P on port %PORT%. Attempting to terminate...
    taskkill /PID %%P /F >nul 2>&1
    if !errorlevel! equ 0 (
        echo Terminated PID %%P.
    ) else (
        echo Warning: could not terminate PID %%P. You may need to stop it manually.
    )
)

REM Prepare build directory
if exist "%BUILD_DIR%" (
    echo Cleaning %BUILD_DIR% ...
    rmdir /S /Q "%BUILD_DIR%"
)
mkdir "%BUILD_DIR%" >nul

REM Compile Java sources
echo Compiling sources...
set "CLASSPATH=%DRIVER_JAR%"
for /f "delims=" %%F in ('dir /b /s src\main\java\*.java') do (
    set "JAVA_FILES=!JAVA_FILES! %%F"
)

javac -cp "%CLASSPATH%" -d "%BUILD_DIR%" !JAVA_FILES!
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

REM Launch the server
echo.
echo Starting UniShare server...
echo (Press Ctrl+C to stop)
echo.

java -cp "%BUILD_DIR%;%DRIVER_JAR%" com.unishare.UniShareServer

endlocal

