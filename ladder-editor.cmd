@echo off
setlocal
cd /d "%~dp0"

if exist "%USERPROFILE%\.gradle\jdks\adoptium-11-x64-hotspot-windows\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\adoptium-11-x64-hotspot-windows"
)

if not defined JAVA_HOME (
    echo Set JAVA_HOME to a Java 11 JDK, then try again.
    exit /b 1
)

call "%~dp0gradlew.bat" ":toolbox:runLadderEditor" --console=plain
exit /b %ERRORLEVEL%
