#!/bin/bash
# Build the Beatix tray app and assemble Beatix.app (menu-bar/Launchpad app).
set -e
cd "$(dirname "$0")/beatix-tray"
go build -o beatix-tray .
APP="Beatix.app"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp beatix-tray "$APP/Contents/MacOS/beatix-tray"
cp ../beatix-bridge/beatix-bridge "$APP/Contents/MacOS/beatix-bridge"
cp AppIcon.icns "$APP/Contents/Resources/AppIcon.icns" 2>/dev/null || true
echo "Built $APP — copy to /Applications to install (appears in Launchpad)."
