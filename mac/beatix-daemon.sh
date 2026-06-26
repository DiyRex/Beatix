#!/bin/bash
# Beatix plug-and-play daemon (run by launchd).
# Keeps the MIDI bridge alive so the "Beatix" device is ALWAYS present for
# Rekordbox, and re-applies the USB socket forward whenever the phone appears.
BRIDGE="/Users/devin/Projects/Beatix/mac/beatix-bridge/beatix-bridge"
ADB="/opt/homebrew/bin/adb"
PORT=5557

while true; do
  # keep the virtual MIDI bridge running (port persists -> Rekordbox keeps device)
  if ! pgrep -f "beatix-bridge -v" >/dev/null 2>&1; then
    "$BRIDGE" -v >/tmp/beatix-bridge.log 2>&1 &
    sleep 1
  fi
  # whenever the phone is connected, make sure the reverse forward is set
  if [ "$("$ADB" get-state 2>/dev/null)" = "device" ]; then
    "$ADB" reverse --list 2>/dev/null | grep -q "tcp:$PORT" || "$ADB" reverse tcp:$PORT tcp:$PORT >/dev/null 2>&1
  fi
  sleep 2
done
