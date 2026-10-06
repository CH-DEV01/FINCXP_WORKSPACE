@echo off
REM Build script para CXP_DAV_CLIENT
REM Genera el dist del proyecto React con Vite

echo.
echo ========================================
echo   CXP_DAV_CLIENT - Build Process
echo ========================================
echo.

REM Cambiar al directorio del proyecto
cd /d "%~dp0"

REM Verificar si node_modules existe, si no, instalar dependencias
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    if errorlevel 1 (
        echo Error: Failed to install dependencies
        pause
        exit /b 1
    )
)

echo.
echo Building project...
call npm run build

if errorlevel 1 (
    echo Error: Build failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Build completed successfully!
echo   Output: %~dp0dist
echo ========================================
echo.
pause
