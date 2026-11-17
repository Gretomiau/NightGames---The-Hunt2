@echo off
cd /d "%~dp0"
call gradlew.bat --no-daemon prepareRunMods runClient
if errorlevel 1 (
    echo.
    echo ERROR: Revisa la consola. Gradle necesita JDK 25; Minecraft JDK 21; y conexion a Internet en el primer arranque.
    pause
    exit /b 1
)
