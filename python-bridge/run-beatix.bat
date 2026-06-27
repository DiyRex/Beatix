@echo off
REM Beatix launcher for Windows. Double-click to run. Leave window open while DJing.
cd /d "%~dp0"

echo Checking phone (USB debugging must be ON)...
adb get-state >nul 2>&1
if errorlevel 1 (
  echo No phone detected. Plug in, enable USB debugging, tap Allow, then retry.
  pause
  exit /b 1
)

echo Forwarding USB socket...
adb reverse tcp:5557 tcp:5557

echo -------------------------------------------------------------
echo  Make sure loopMIDI is running with a port named "Beatix".
echo  Open your DJ app AFTER this line, then map the Beatix device.
echo  Press Ctrl+C to stop.
echo -------------------------------------------------------------
python beatix_bridge.py
pause
