# Beatix — Run & MIDI-LEARN

## Plug-and-play service (set up once, auto-starts at login)
A `launchd` agent keeps the MIDI bridge alive and re-applies the USB forward
automatically, so Rekordbox always finds "Beatix".
- Start/enable:  `launchctl load -w ~/Library/LaunchAgents/com.beatix.plist`
- **Stop/disable: `launchctl unload ~/Library/LaunchAgents/com.beatix.plist`**
- Check it's running: `launchctl list | grep beatix`
- Logs: `/tmp/beatix-bridge.log` and `/tmp/beatix-daemon.log`

With the daemon running you can skip the manual steps below — just plug in the
phone, open Rekordbox, open the app.

## Per-session startup (manual alternative, if not using the daemon)
1. Plug in the phone (USB mode = **File Transfer / MTP**, screen unlocked).
2. Double-click **`~/Projects/Beatix/run-beatix.command`**  — it runs
   `adb reverse tcp:5557` and starts the bridge. Leave the window open.
   (Manual equivalent: `adb reverse tcp:5557 tcp:5557` then
   `./mac/beatix-bridge/beatix-bridge -v`.)
3. **Open Rekordbox AFTER the bridge** (it scans MIDI at launch — so it sees "Beatix").
4. Open the **Beatix** app on the phone. It auto-connects.

## One-time MIDI-LEARN in Rekordbox
1. Preferences → Controller / MIDI → make sure **Beatix** is enabled.
2. Click the **MIDI** (learn) button, top-right of the deck area.
3. Click a control ON SCREEN, then **touch the matching control on the phone**:
   - Buttons (Play/Cue/Sync/Hot Cues/Loop/Load/FX): learn as **Trigger**.
   - Sliders (EQ Hi/Mid/Low, channel faders, crossfader, tempo, FX level):
     set control type **Absolute** — the touch slider then maps 1:1 to the knob.
   - Jog drag: learn the deck's jog/pitch-bend; type **Relative**.
4. Turn MIDI-LEARN off. Your mapping is saved automatically.

## What maps to what (phone → Rekordbox)
- Deck A (left) / Deck B (right): IN, OUT, 4BEAT, SYNC, 8 pads (hot cues),
  CUE, PLAY, jog, TEMPO fader.
- Center: LOAD A/B, browse ▲▼, EQ Hi/Mid/Low ×2, channel faders ×2,
  crossfader, BEAT FX ON / SEL / LEVEL / ◄ ►.

## Tips
- If the app says disconnected: re-run `adb reverse tcp:5557 tcp:5557`
  (it resets when the cable re-enumerates).
- EQ sliders start centered; channel faders start high; crossfader centered.
- Keep the cable stable (data cable, MTP mode) — drops break the socket.
