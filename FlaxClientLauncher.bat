@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "LAUNCHER_DIR=%ROOT_DIR%FlaxClientLauncher"
set "LAUNCHER_EXE=%LAUNCHER_DIR%\FlaxClientLauncher.exe"

if not exist "%LAUNCHER_EXE%" (
    echo [FlaxClient] FlaxClientLauncher.exe was not found:
    echo %LAUNCHER_EXE%
    echo.
    echo Build or place FlaxClientLauncher.exe in the FlaxClientLauncher folder.
    exit /b 1
)

set "FLAX_SKIP_LOCAL_BUILD=1"
start "" /D "%LAUNCHER_DIR%" "%LAUNCHER_EXE%"
if errorlevel 1 (
    echo [FlaxClient] Failed to start launcher executable.
    exit /b 1
)

exit /b 0
