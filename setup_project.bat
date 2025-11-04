@echo off
echo ========================================
echo RescueMate Project Setup
echo ========================================
echo.

echo [1/3] Setting up Gradle Wrapper...
echo.

REM Create gradle wrapper directory if it doesn't exist
if not exist gradle\wrapper (
    mkdir gradle\wrapper
)

echo Downloading Gradle 8.13...
echo This may take a few minutes...
echo.

REM Use PowerShell to download gradle wrapper jar
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'}"

if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to download gradle-wrapper.jar
    echo.
    echo Please manually download from:
    echo https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
    echo.
    echo And place it in: gradle\wrapper\gradle-wrapper.jar
    pause
    exit /b 1
)

echo Gradle wrapper downloaded successfully!
echo.

echo [2/3] Installing Node.js dependencies...
call npm install
if %ERRORLEVEL% neq 0 (
    echo WARNING: npm install failed or is not available
    echo Please ensure Node.js is installed
)
echo.

echo [3/3] Setup complete!
echo.
echo ========================================
echo Next steps:
echo 1. Run 'build_and_test.bat' to build the project
echo 2. Or run 'gradlew.bat assembleDebug' for Android only
echo 3. Or run 'npm run dev' for web development
echo ========================================
pause

