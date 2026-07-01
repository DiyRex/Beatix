# macOS Accessibility — why track-nav arrows kept breaking (and the permanent fix)

## Symptom
Track-navigation arrow keys (and any keyboard-synthesized input) would work right
after granting Accessibility, then silently stop — usually after a rebuild, a tray
restart, or a reboot. The bridge log showed the keystrokes arriving
(`key 125 shift 0`, `key 126 shift 0`), so phone → bridge was fine; macOS was
**silently dropping the injected `CGEvent`s**.

## Root cause: TCC "responsible process" attribution
macOS grants Accessibility **per executable**, but when a process is spawned as a
**child**, TCC evaluates the permission against the child's *responsible process* —
its parent. The tray (`beatix-tray`) was spawning the bridge (`beatix-bridge`) as a
child, so:

- The grant landed on / was evaluated against **`beatix-tray`**, not the bridge.
- Even after explicitly adding **`beatix-bridge`** to the Accessibility list and
  enabling it, injection stayed blocked — the running child was still attributed
  to the tray as its responsible parent.

This is why it "worked in the morning": back then the bridge ran under **launchd**
(its own responsible process), so its own grant applied. Once the tray took over
spawning it as a child, the grant no longer stuck.

## The fix: run the bridge as its own launchd agent
The bridge now runs as an independent LaunchAgent (`com.beatix.bridge`) instead of
as a child of the tray. A launchd-spawned process is its **own** responsible
process, so the (persistent, cert-signed) Accessibility grant applies and survives
restarts, rebuilds, and reboots.

- Agent plist: `~/Library/LaunchAgents/com.beatix.bridge.plist`
  - `ProgramArguments`: the cert-signed `beatix-bridge -v`
  - `RunAtLoad` + `KeepAlive` → starts at login, auto-restarts if it dies
  - logs to `/tmp/beatix-bridge.log`
- The bridge binary is code-signed with the self-signed **"Beatix Code Signing"**
  identity (`com.beatix.bridge`). Because the grant is keyed to a *stable signing
  identity* (not an ad-hoc cdhash), re-signing on every rebuild keeps the grant.

### The tray now *manages* the agent instead of spawning a child (macOS only)
`cmd/beatix-tray/main.go`:
- `installAgent()` writes/loads the plist (idempotent — never disturbs a live bridge).
- `startBridge()` → `launchctl kickstart` (no-op if already running).
- `restartBridge()` → `launchctl kickstart -k`.
- `stopBridge()` → `launchctl bootout` (so `KeepAlive` won't respawn).
- `bridgeUp()` → `pgrep -f "beatix-bridge -v"`.
- Quitting the tray does **not** kill the bridge on macOS (it lives independently).
- **Windows** keeps the original direct-child-spawn model — it has no equivalent
  permission, so none of this applies.

## One-time grant (only needed once per machine)
1. System Settings → Privacy & Security → **Accessibility**.
2. Add / enable **`beatix-bridge`** (the binary at
   `/Applications/Beatix.app/Contents/MacOS/beatix-bridge`). Drag it in from Finder
   to avoid macOS collapsing the path to the `.app`.
3. If it was already running when you enabled it, restart it once
   (`launchctl kickstart -k gui/$(id -u)/com.beatix.bridge`) so the fresh process
   reads the now-enabled entry.

After that it persists — you should never touch this pane again.
