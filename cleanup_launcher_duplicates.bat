@echo off
echo Cleaning up duplicate launcher icon files...
echo.

REM Delete .webp files from mipmap-mdpi
del "app\src\main\res\mipmap-mdpi\ic_launcher.webp" 2>nul
del "app\src\main\res\mipmap-mdpi\ic_launcher_foreground.webp" 2>nul
del "app\src\main\res\mipmap-mdpi\ic_launcher_round.webp" 2>nul
echo Cleaned mipmap-mdpi

REM Delete .webp files from mipmap-hdpi
del "app\src\main\res\mipmap-hdpi\ic_launcher.webp" 2>nul
del "app\src\main\res\mipmap-hdpi\ic_launcher_foreground.webp" 2>nul
del "app\src\main\res\mipmap-hdpi\ic_launcher_round.webp" 2>nul
echo Cleaned mipmap-hdpi

REM Delete .webp files from mipmap-xhdpi
del "app\src\main\res\mipmap-xhdpi\ic_launcher.webp" 2>nul
del "app\src\main\res\mipmap-xhdpi\ic_launcher_foreground.webp" 2>nul
del "app\src\main\res\mipmap-xhdpi\ic_launcher_round.webp" 2>nul
echo Cleaned mipmap-xhdpi

REM Delete .webp files from mipmap-xxhdpi
del "app\src\main\res\mipmap-xxhdpi\ic_launcher.webp" 2>nul
del "app\src\main\res\mipmap-xxhdpi\ic_launcher_foreground.webp" 2>nul
del "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.webp" 2>nul
echo Cleaned mipmap-xxhdpi

REM Delete .webp files from mipmap-xxxhdpi
del "app\src\main\res\mipmap-xxxhdpi\ic_launcher.webp" 2>nul
del "app\src\main\res\mipmap-xxxhdpi\ic_launcher_foreground.webp" 2>nul
del "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.webp" 2>nul
echo Cleaned mipmap-xxxhdpi

echo.
echo ✅ All duplicate .webp launcher files have been deleted!
echo.
echo Next steps:
echo 1. In Android Studio, go to Build ^> Clean Project
echo 2. Then go to Build ^> Rebuild Project
echo 3. Your project should now build successfully!
echo.
pause

