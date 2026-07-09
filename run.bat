@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out

echo [1/2] Compiling all sources...
setlocal enabledelayedexpansion
set "SRCS="
for /r "src\main\java" %%f in (*.java) do set "SRCS=!SRCS! %%f"
javac -encoding UTF-8 -d out -cp "lib\*" %SRCS%
if errorlevel 1 (
    echo.
    echo Compile FAILED - see errors above.
    pause
    exit /b 1
)

echo [2/2] Starting game (close the window to exit)...
java -Dfile.encoding=UTF-8 -cp "out;lib\*;src\main\resources" com.game.GameApp
pause
