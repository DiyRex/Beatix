# Beatix — Windows & VirtualDJ

## A. VirtualDJ (macOS or Windows) — no Beatix changes
VirtualDJ reads the same "Beatix" MIDI port; nothing in the bridge/app changes, and
your Rekordbox mapping is untouched (separate files).

Mapping files are in `vdj/`:
- `vdj/Beatix.xml`         → definition (names every MIDI control)
- `vdj/Beatix-mapper.xml`  → mapper (names → VirtualDJ actions)

Install them:
- macOS: copy to `~/Library/Application Support/VirtualDJ/Devices/Beatix.xml`
  and `.../VirtualDJ/Mappers/Beatix.xml`  (already placed for you on this Mac).
- Windows: copy to `Documents\VirtualDJ\Devices\Beatix.xml`
  and `Documents\VirtualDJ\Mappers\Beatix.xml`.

Then open VirtualDJ → Settings → Controllers; it should show **Beatix**. If a control
is off, right-click it on screen → **learn** (VirtualDJ's mapper is very forgiving).
VirtualDJ also supports **jog scratch** and **LED feedback** for custom MIDI, which
Rekordbox does not — so the platter and Beat-FX-blink can be made to work there.

Run **one DJ app at a time** (don't open Rekordbox and VirtualDJ together).

## B. Running the whole thing on Windows

Two bridge options — pick one. Both need **loopMIDI** + **adb**.

### Option 1 — prebuilt Go .exe (no Python needed) — recommended
`windows/beatix-bridge.exe` is a standalone binary (cross-compiled; uses winmm for
MIDI + SendInput for browse keys).
1. Install **loopMIDI** (free) and create a port named exactly **`Beatix`**.
2. Install Android **platform-tools** (adb) and add to PATH.
3. Phone: USB debugging + Install via USB.
Per session: start loopMIDI → plug phone → double-click **`windows/run-beatix.bat`**
→ open Rekordbox/VirtualDJ → open the Beatix app.
(Note: cross-compiled from macOS; if Windows SmartScreen warns, choose "Run anyway".)

### Option 2 — Python bridge (`python-bridge/`)
Same behavior, if you prefer Python. One-time setup:

1. **Python 3**: install from python.org (tick "Add to PATH").
   Then: `pip install mido python-rtmidi pynput`
2. **loopMIDI** (Tobias Erichsen, free): Windows has no built-in virtual MIDI, so
   install loopMIDI and create a port named exactly **`Beatix`**. (This is the port
   Rekordbox/VirtualDJ will see.)
3. **platform-tools** (adb): download Android SDK platform-tools, add to PATH.
4. Phone: enable USB debugging + Install via USB (same as macOS).

Per session:
1. Start **loopMIDI** (port "Beatix" present).
2. Plug in the phone.
3. Double-click **`python-bridge/run-beatix.bat`** (runs `adb reverse` + the bridge).
4. Open Rekordbox or VirtualDJ AFTER the bridge; import the Beatix mapping.
5. Open the Beatix app on the phone.

Auto-start (optional, like the macOS launchd daemon): create a Task Scheduler task
that runs `run-beatix.bat` at logon (set "Run only when user is logged on").

Notes:
- Browse arrows use keyboard synthesis (pynput) — works on Windows without extra
  permission (macOS needs Accessibility; Windows does not).
- The Beatix APK is identical on any OS — only the Mac-side bridge differs.

## C. Tray app (menu-bar / system-tray controller)
A one-click controller that runs + monitors the bridge, auto-reapplies `adb reverse`,
and shows status (Bridge / Phone). Replaces the manual launcher/daemon.

- macOS: `mac/beatix-tray/Beatix.app` (build with `mac/build-tray.sh`). Runs in the
  menu bar (amber jog icon). Add to System Settings → Login Items to auto-start.
- Windows: `windows/beatix-tray.exe` — double-click; it sits in the system tray and
  spawns `beatix-bridge.exe` (keep both in the same folder). Still needs loopMIDI
  (port "Beatix") + adb on PATH. For auto-start, drop a shortcut in
  `shell:startup`.

Menu: status line, Reconnect USB, Restart bridge, Stop/Start bridge, Quit.
