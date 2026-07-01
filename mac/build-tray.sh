#!/bin/bash
# Build + sign the macOS bridge & tray from ./cmd and assemble Beatix.app
# (menu-bar / Launchpad app). Binary paths are kept stable for the daemon.
set -e
R="$(cd "$(dirname "$0")/.." && pwd)"
go -C "$R" build -o mac/beatix-bridge/beatix-bridge ./cmd/beatix-bridge
codesign --force -s "Beatix Code Signing" -i com.beatix.bridge "$R/mac/beatix-bridge/beatix-bridge" 2>/dev/null || echo "(codesign identity missing — bridge left ad-hoc signed)"
go -C "$R" build -o mac/beatix-tray/beatix-tray ./cmd/beatix-tray
APP="$R/mac/beatix-tray/Beatix.app"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$R/mac/beatix-tray/beatix-tray" "$APP/Contents/MacOS/beatix-tray"
cp "$R/mac/beatix-bridge/beatix-bridge" "$APP/Contents/MacOS/beatix-bridge"
cp "$R/mac/beatix-tray/AppIcon.icns" "$APP/Contents/Resources/AppIcon.icns" 2>/dev/null || true
echo "Built $APP — copy to /Applications to install (appears in Launchpad)."
