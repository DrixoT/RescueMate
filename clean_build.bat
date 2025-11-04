@echo off
echo Cleaning RescueMate build artifacts...
echo.

REM Clean Gradle build directories
if exist build (
    echo Deleting root build directory...
    rmdir /s /q build
)

if exist app\build (
    echo Deleting app build directory...
    rmdir /s /q app\build
)

REM Clean Gradle caches
if exist .gradle (
    echo Deleting .gradle cache directory...
    rmdir /s /q .gradle
)

REM Clean Kotlin build caches
if exist app\.cxx (
    echo Deleting app\.cxx directory...
    rmdir /s /q app\.cxx
)

echo.
echo Build cleanup complete!
echo You can now rebuild the project.
pause

