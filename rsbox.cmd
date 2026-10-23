@echo off
setlocal
cd /d "%~dp0"

rem Prefer the Java 11 toolchain provisioned by the original Gradle build.
if exist "%USERPROFILE%\.gradle\jdks\adoptium-11-x64-hotspot-windows\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\adoptium-11-x64-hotspot-windows"
)

set "RSBOX_TASK="
if /i "%~1"=="build" set "RSBOX_TASK=build"
if /i "%~1"=="setup" set "RSBOX_TASK=:server:setup server"
if /i "%~1"=="server" set "RSBOX_TASK=:server:run server"
if /i "%~1"=="client" set "RSBOX_TASK=:client:run client"

if not defined RSBOX_TASK (
    echo Usage: rsbox.cmd build ^| setup ^| server ^| client
    exit /b 1
)

if not defined JAVA_HOME (
    echo Set JAVA_HOME to a Java 11 JDK directory, then try again.
    exit /b 1
)

call "%~dp0gradlew.bat" "%RSBOX_TASK%" --console=plain
exit /b %ERRORLEVEL%
