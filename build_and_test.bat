@echo off
echo ========================================
echo RescueMate Build and Test Script
echo ========================================
echo.

REM Step 1: Clean build artifacts
echo [1/5] Cleaning build artifacts...
call clean_build.bat
echo.

REM Step 2: Check if gradle wrapper jar exists
if not exist gradle\wrapper\gradle-wrapper.jar (
    echo ERROR: gradle-wrapper.jar is missing!
    echo Please download it from: https://services.gradle.org/distributions/gradle-8.13-bin.zip
    echo Extract and place gradle-wrapper.jar in gradle\wrapper\
    pause
    exit /b 1
)

REM Step 3: Build Android app
echo [2/5] Building Android application...
echo Running: gradlew.bat assembleDebug
call gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo ERROR: Android build failed!
    pause
    exit /b 1
)
echo Android build successful!
echo.

REM Step 4: Check for APK
echo [3/5] Verifying APK output...
if exist app\build\outputs\apk\debug\app-debug.apk (
    echo APK found: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo WARNING: APK not found in expected location
)
echo.

REM Step 5: Install Node dependencies and build web app
echo [4/5] Installing Node.js dependencies...
call npm install
if %ERRORLEVEL% neq 0 (
    echo ERROR: npm install failed!
    pause
    exit /b 1
)
echo.

echo [5/5] Building web application...
call npm run build
if %ERRORLEVEL% neq 0 (
    echo ERROR: Web build failed!
    pause
    exit /b 1
)
echo Web build successful!
echo.

echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo APK Location: app\build\outputs\apk\debug\app-debug.apk
echo Web Build: dist\
echo.
pause

