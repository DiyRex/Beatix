#!/bin/bash
# Re-sign the bridge with the stable self-signed identity "Beatix Code Signing"
# so macOS Accessibility permission persists across rebuilds.
# Run this after every `go build` of the bridge.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
codesign --force -s "Beatix Code Signing" -i com.beatix.bridge "$DIR/beatix-bridge/beatix-bridge"
echo "signed:"
codesign -dvvv "$DIR/beatix-bridge/beatix-bridge" 2>&1 | grep -iE "Authority|Identifier="
