#!/bin/bash
# Beatix session launcher (USB/ADB). Starts the MIDI bridge and keeps the USB
# socket forward alive — auto-reapplying it whenever the phone reconnects.
# Double-click to run. Leave the window open while you DJ. Ctrl+C to stop.
cd "$(dirname "$0")"
PORT=5557
BRIDGE="./mac/beatix-bridge/beatix-bridge"

if [ ! -x "$BRIDGE" ]; then
  echo "Building bridge..."
  ( cd mac/beatix-bridge && go build -o beatix-bridge . ) || { echo "Build failed (need Go)."; exit 1; }
fi

# Start the MIDI bridge in the background.
"$BRIDGE" -v &
BPID=$!
trap 'kill $BPID 2>/dev/null; echo; echo "Beatix bridge stopped."' EXIT
echo "Bridge started (pid $BPID)."
echo "Open Rekordbox AFTER this line, enable the 'Beatix' MIDI device, then MIDI-LEARN."
echo "Watching USB for the phone (auto-reconnects)... Ctrl+C to quit."

LAST=""
while true; do
  STATE="$(adb get-state 2>/dev/null)"
  if [ "$STATE" = "device" ]; then
    if [ "$LAST" != "up" ]; then
      adb reverse tcp:$PORT tcp:$PORT >/dev/null 2>&1 \
        && echo "$(date '+%H:%M:%S')  phone connected  ->  USB socket forwarded :$PORT"
      LAST="up"
    fi
  else
    if [ "$LAST" != "down" ]; then
      echo "$(date '+%H:%M:%S')  phone offline - check cable / unlock screen..."
      LAST="down"
    fi
  fi
  sleep 2
done
