@echo off
setlocal enabledelayedexpansion

set DRIVER_JAR=lib\postgresql-42.7.3.jar
set BUILD_DIR=build\classes
set SECRET_FILE=config\neon-db-url.txt
set DEFAULT_DB_URL=postgresql://neondb_owner:npg_ZThxUb3LnBj2@ep-still-recipe-a1efuwr6-pooler.ap-southeast-1.aws.neon.tech/Unishare?sslmode=require^&channel_binding=require

if not exist "config" (
    mkdir "config" >nul 2>&1
)

echo.
echo === UniShare seed script ===
echo.

if not exist "%DRIVER_JAR%" (
    echo [ERROR] PostgreSQL driver not found at %DRIVER_JAR%.
    echo Download it with:
    echo   powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar' -OutFile 'lib\\postgresql-42.7.3.jar'"
    exit /b 1
)

if "%NEON_DB_URL%"=="" (
    if exist "%SECRET_FILE%" (
        for /f "usebackq tokens=* delims=" %%A in ("%SECRET_FILE%") do (
            set "NEON_DB_URL=%%A"
        )
    )
)

if "%NEON_DB_URL%"=="" (
    set "NEON_DB_URL=%DEFAULT_DB_URL%"
)

if "%NEON_DB_URL%"=="" (
    echo [ERROR] NEON_DB_URL environment variable is not set.
    echo Either set it globally or add the connection string to %SECRET_FILE%.
    exit /b 1
)

if exist "%BUILD_DIR%" (
    rmdir /S /Q "%BUILD_DIR%"
)
mkdir "%BUILD_DIR%" >nul

echo Compiling backend...
set "JAVA_FILES="
for /f "delims=" %%F in ('dir /b /s src\main\java\*.java') do (
    set "JAVA_FILES=!JAVA_FILES! %%F"
)

javac -cp "%DRIVER_JAR%" -d "%BUILD_DIR%" !JAVA_FILES!
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    exit /b 1
)

echo.
echo Seeding users...
java -cp "%BUILD_DIR%;%DRIVER_JAR%" com.unishare.util.SeedUsers

echo.
echo Done.

endlocal

