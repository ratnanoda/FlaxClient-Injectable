@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "LAUNCHER_DIR=%ROOT_DIR%FlaxClientLauncher"
set "LAUNCHER_EXE=%LAUNCHER_DIR%\FlaxClientLauncher.exe"
set "CLIENT_JAR=%ROOT_DIR%build\libs\FlaxClient-Release.jar"
set "LAUNCHER_JAR=%LAUNCHER_DIR%\FlaxClient-Release.jar"

if not exist "%LAUNCHER_EXE%" (
    echo [FlaxClient] FlaxClientLauncher.exe was not found:
    echo %LAUNCHER_EXE%
    echo.
    echo Build or place FlaxClientLauncher.exe in the FlaxClientLauncher folder.
    exit /b 1
)

if exist "%CLIENT_JAR%" (
    copy /Y "%CLIENT_JAR%" "%LAUNCHER_JAR%" >nul
    if errorlevel 1 (
        echo [FlaxClient] Warning: failed to copy legacy 1.8.9 jar to launcher directory.
        echo [FlaxClient] The launcher will still start and can build/version-select on its own.
    )
)

if not exist "%CLIENT_JAR%" (
    echo [FlaxClient] Legacy 1.8.9 jar not found. Skipping copy step.
    echo [FlaxClient] This does not block 1.21.11 launch.
)

set "FLAX_SKIP_LOCAL_BUILD=1"
start "" /D "%LAUNCHER_DIR%" "%LAUNCHER_EXE%"
if errorlevel 1 (
    echo [FlaxClient] Failed to start launcher executable.
    exit /b 1
)

exit /b 0
