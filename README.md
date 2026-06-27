# Beatix 🎛️

Turn an **Android phone into a touch DJ controller for Rekordbox** on macOS — a
$0, DIY alternative to a hardware controller (DDJ-FLX4-inspired layout). Buttons,
hot cues, **real EQ**, faders, crossfader, tempo and a rotatable jog, sent over
**USB** to Rekordbox via MIDI.

Built for a Redmi Note 9 Pro + Rekordbox 7 on macOS (Apple Silicon).

---

## How it works

```
┌─────────────────────────┐   TCP over USB (adb reverse)   ┌──────────────────┐
│  Beatix app (Android)   │ ─────────────────────────────▶ │  beatix-bridge   │
│  Kotlin + Compose UI    │   127.0.0.1:5557  "N/C/J" msgs  │  (Go + CoreMIDI) │
└─────────────────────────┘                                 └────────┬─────────┘
                                                  virtual MIDI port  │ "Beatix"
                                                                      ▼
                                                              ┌──────────────┐
                                                              │  Rekordbox 7 │
                                                              │  (MIDI map)  │
                                                              └──────────────┘
```

- The **app** draws the console and sends compact ASCII messages per touch.
- `adb reverse` forwards the phone's `127.0.0.1:5557` to the Mac over the USB cable.
- The **Go bridge** turns those messages into MIDI on a virtual CoreMIDI source
  named **Beatix**.
- **Rekordbox** sees "Beatix" as a MIDI controller; a generated mapping file binds
  every control. We mirror the FLX4 layout but stay a generic MIDI device (we do
  not emulate FLX4 hardware identity).

Why a bridge at all: Rekordbox only accepts MIDI, and Android phones can't reliably
act as USB-MIDI peripherals — so the bridge converts socket bytes → MIDI.

---

## Repository layout

```
Beatix/
├── README.md                     # this file
├── app/                          # Android app (Kotlin / Jetpack Compose)
│   └── src/main/java/com/beatix/
│       ├── MainActivity.kt       # landscape, immersive, display-cutout, MIDI client lifecycle
│       ├── Console.kt            # the FLX4-style console UI (decks, mixer, jog, faders)
│       ├── Midi.kt               # socket client + note/CC id map (DeckA/DeckB/Center)
│       └── Theme.kt              # dark + Pioneer-amber palette
├── mac/
│   ├── beatix-bridge/            # Go bridge (socket -> virtual CoreMIDI "Beatix")
│   │   ├── main.go
│   │   └── beatix-bridge         # built binary
│   └── beatix-daemon.sh          # plug-and-play loop (keeps bridge up + auto adb reverse)
├── python-bridge/                # cross-platform bridge (Windows/Linux/mac)
│   ├── beatix_bridge.py          # socket -> MIDI (+ keyboard synth); uses loopMIDI on Windows
│   └── run-beatix.bat            # Windows launcher
├── rekordbox/Beatix.midi.csv     # importable Rekordbox mapping (56 controls)
├── vdj/Beatix.xml                # importable VirtualDJ definition
├── vdj/Beatix-mapper.xml         # importable VirtualDJ mapper
├── run-beatix.command            # macOS double-click launcher (manual alt to daemon)
└── docs/                         # design + findings + guides (see docs/00-overview.md)
```

**Importable mappings:** `rekordbox/Beatix.midi.csv` and `vdj/Beatix.xml` +
`vdj/Beatix-mapper.xml` — see `docs/06-importable-mappings.md` for where to copy
them. Windows + VirtualDJ setup: `docs/05-windows-and-virtualdj.md`.

Plus, outside the repo:
- `~/Library/LaunchAgents/com.beatix.plist` — the plug-and-play service.
- `~/Library/Application Support/Pioneer/rekordbox6/MidiMappings/Beatix.midi.csv` —
  the active Rekordbox mapping (also kept on the Desktop for IMPORT).

---

## Build from source

Prereqs (all already present on the target Mac via Homebrew):
- Go 1.2x (cgo + Xcode Command Line Tools for CoreMIDI)
- JDK 17 (`openjdk@17`) — Gradle/AGP won't run on JDK 25
- Android SDK (`sdkmanager`, platform 35, build-tools 35), `adb`, `gradle`

**Bridge:**
```bash
go -C mac/beatix-bridge build -o beatix-bridge .
./mac/beatix-bridge/beatix-bridge -selftest      # prints SELFTEST OK
```

**APK (debug):**
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

---

## Install on the phone

1. Enable **Developer options** (Settings → About → tap MIUI version ×7) and turn on
   **USB debugging** + **Install via USB**.
2. Set the USB connection to **File Transfer (MTP)** with a **data** cable.
3. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
   - MIUI may block direct install (`INSTALL_FAILED_USER_RESTRICTED`) — enable
     "Install via USB", or `adb push <apk> /sdcard/Download/` and tap it on the phone.

---

## Run (plug-and-play)

The `launchd` agent keeps the bridge alive and re-applies the USB forward
automatically, so Rekordbox always finds "Beatix":

```bash
launchctl load -w ~/Library/LaunchAgents/com.beatix.plist   # once; auto-starts at login
launchctl list | grep beatix                                     # verify
```

Then just:
1. Plug in the phone (MTP, unlocked).
2. Open **Rekordbox** (after the daemon — it scans MIDI at launch).
3. Open the **Beatix** app → it connects automatically.

Manual alternative (no daemon): double-click `run-beatix.command` and leave it open.

To stop/remove the service:
```bash
launchctl unload ~/Library/LaunchAgents/com.beatix.plist
```

---

## Wire protocol (app → bridge)

One ASCII message per line, MIDI channel 1:
| Msg | Meaning | Example |
|-----|---------|---------|
| `N <note> <0\|1>` | button note off/on (vel 127) | `N 36 1` |
| `C <cc> <0-127>` | slider absolute CC | `C 22 96` |
| `J <cc> <±delta>` | jog/encoder relative (1=+, 127=−) | `J 16 -1` |
| `P` | heartbeat | `P` |

---

## Rekordbox mapping

Generated as `Beatix.midi.csv` (Rekordbox's own format, mirrored from its bundled
`DDJ-FLX4.midi.csv`). Key rules learned:
- **Per-deck functions** (Play/Cue/Sync/Loops/Hot Cues/EQ/Channel Fader/Tempo/Jog)
  put their MIDI codes in the **deck1 / deck2** columns (blank input column).
- **Global functions** (Crossfader, Browse, Beat FX) put their code in the
  **standalone input column**.
- Buttons → `Button`, pads → `Pad`, sliders → `KnobSlider` (7-bit absolute),
  jog → `JogRotate` + `JogTouch`, browse → `Rotary`.

Regenerate + apply:
```bash
python3 /tmp/gen_beatix_csv.py   # writes ~/Desktop/Beatix.midi.csv
# quit Rekordbox, copy to .../rekordbox6/MidiMappings/Beatix.midi.csv, reopen
# (or use the MIDI setting window's IMPORT button — no restart)
```

### Control → MIDI map (channel 1)
| Control | Deck A | Deck B | Type |
|---|---|---|---|
| Play/Pause | note 0x24 | 0x44 | Button |
| Cue | 0x25 | 0x45 | Button |
| Sync | 0x26 | 0x46 | Button |
| Loop In / Out | 0x27 / 0x28 | 0x47 / 0x48 | Button |
| 4-Beat Loop | 0x29 | 0x49 | Button |
| Hot Cue 1–8 | 0x2C–0x33 | 0x4C–0x53 | Pad |
| Jog rotate / touch | cc 0x10 / note 0x2A | cc 0x11 / note 0x4A | JogRotate / JogTouch |
| EQ Hi/Mid/Low | cc 0x16/0x18/0x1A | cc 0x17/0x19/0x1B | KnobSlider |
| Channel Fader | cc 0x1E | cc 0x1F | KnobSlider |
| Tempo | cc 0x00 | cc 0x01 | KnobSlider |
| Load to deck | note 0x64 | 0x65 | Button |
| Crossfader | cc 0x20 (global) | — | KnobSlider |
| Browse ▲/▼ | cc 0x21 relative (global) | — | Rotary |
| Beat FX On / Level / ◄ ► | notes 0x69/0x6A/0x6B, cc 0x25 (global) | — | Button / KnobSlider |

---

## Status

**Working:** Play, Cue, Sync, Loop In/Out, 4-Beat Loop, Hot Cues 1–8, EQ Hi/Mid/Low,
Channel faders, Tempo, Load A/B, **Crossfader**, **pad-mode banks** (Hot Cue / Pad FX /
Beat Jump / Sampler via per-deck tabs), **SHIFT** modifier, plug-and-play daemon,
edge-to-edge cutout layout.

**Browse (track ▲/▼)** is done via **keyboard simulation** — the bridge synthesizes
Up/Down arrow keystrokes (Rekordbox track-list navigation), because the MIDI `Rotary`
browse function crashes Rekordbox. This needs the bridge binary granted **Accessibility**
permission (System Settings → Privacy & Security → Accessibility → add
`mac/beatix-bridge/beatix-bridge`) and Rekordbox focused.

**Notes / pending:**
- Jog scratch needs **Vinyl mode ON** for the deck in Rekordbox.
- Crossfader needs the Rekordbox channels assigned to it (A/B, not THRU).
- SHIFT is a momentary modifier (Rekordbox `Shift` function); shifted combos
  (delete hot cue, master) are a follow-up — they need channel-based shift-variant notes.
- No waveforms/BPM/art on the phone — it's a control surface, not a player.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| App "not responding", buttons dead | USB `reverse` dropped — the daemon re-applies it within 2s; or run `adb reverse tcp:5557 tcp:5557`. |
| Rekordbox doesn't see "Beatix" | Start the daemon before Rekordbox; confirm with `launchctl list \| grep beatix` and the MIDI setting "Connected device" dropdown. |
| A control does nothing | Check the bridge log `/tmp/beatix-bridge.log` to confirm the app sends it; if so, it's a mapping row (deck-column vs standalone-input). |
| Crossfader moves on-screen but no audio | Assign the mixer channels to the crossfader (A/B) in Rekordbox. |
| MIUI blocks install | Enable "Install via USB", or push the APK and tap it on the phone. |
