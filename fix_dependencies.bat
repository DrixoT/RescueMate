@echo off
echo ========================================
echo FIXING DEPENDENCY ISSUES
echo ========================================
echo.

echo [Step 1/4] Cleaning configuration cache and build directories...
if exist .gradle (
    echo Removing .gradle directory...
    rmdir /s /q .gradle
)
if exist build (
    echo Removing build directory...
    rmdir /s /q build
)
if exist app\build (
    echo Removing app\build directory...
    rmdir /s /q app\build
)
echo Clean complete!
echo.

echo [Step 2/4] Gradle configuration summary:
echo - Added JitPack repository to settings.gradle.kts
echo - Repository: https://jitpack.io
echo - Purpose: Resolve audioswitch dependency (com.github.davidliu)
echo.

echo [Step 3/4] Dependencies that will be downloaded:
echo - io.elevenlabs:elevenlabs-android:0.3.0
echo - io.livekit:livekit-android:2.19.0 (transitive)
echo - com.github.davidliu:audioswitch:89582c47c9 (transitive, from JitPack)
echo.

echo [Step 4/4] Next steps:
echo.
echo IN ANDROID STUDIO:
echo 1. Open Android Studio
echo 2. File -^> Sync Project with Gradle Files
echo 3. Wait for sync to complete
echo 4. Build -^> Rebuild Project
echo.
echo OR VIA COMMAND LINE (if gradlew.bat exists):
echo 1. gradlew.bat clean
echo 2. gradlew.bat build --refresh-dependencies
echo.
echo ========================================
echo FIXES APPLIED:
echo ========================================
echo ✓ Added JitPack repository to settings.gradle.kts
echo ✓ Fixed FontWeight import in HomeDashboard.kt
echo ✓ Fixed syntax error in WellnessAIConversationScreen.kt
echo ✓ Removed invalid label reference
echo ✓ Cleaned build cache
echo.
echo ========================================
echo REMAINING ISSUES:
echo ========================================
echo ⚠ ElevenLabs SDK classes unresolved
echo   → This will be fixed after Gradle sync
echo   → Classes needed: ConversationClient, ConversationConfig, ConversationSession
echo   → These are in: io.elevenlabs:elevenlabs-android:0.3.0
echo.
echo ========================================
echo STATUS: Ready for Gradle Sync!
echo ========================================
echo.
pause

