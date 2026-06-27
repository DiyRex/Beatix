@echo off
REM Beatix Windows launcher (Go binary - no Python needed).
REM Requires: loopMIDI running with a port named "Beatix", and adb on PATH.
cd /d "%~dp0"

adb get-state >nul 2>&1
if errorlevel 1 (
  echo No phone detected. Enable USB debugging, tap Allow, then retry.
  pause
  exit /b 1
)

echo Forwarding USB socket...
adb reverse tcp:5557 tcp:5557

echo -------------------------------------------------------------
echo  Make sure loopMIDI has a port named "Beatix".
echo  Open Rekordbox / VirtualDJ AFTER this line. Ctrl+C to stop.
echo -------------------------------------------------------------
beatix-bridge.exe -v
pause
