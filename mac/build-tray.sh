#!/bin/bash
# Build the Beatix tray app and assemble Beatix.app (menu-bar controller).
set -e
cd "$(dirname "$0")/beatix-tray"
go build -o beatix-tray .
APP="Beatix.app"
mkdir -p "$APP/Contents/MacOS"
cp beatix-tray "$APP/Contents/MacOS/beatix-tray"
cp ../beatix-bridge/beatix-bridge "$APP/Contents/MacOS/beatix-bridge"
echo "Built $APP — double-click it (or 'open $APP') to run the menu-bar controller."
