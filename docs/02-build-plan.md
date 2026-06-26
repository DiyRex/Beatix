# Beatix — Build Plan (Android controller for Rekordbox)

## 1. Architecture
```
[Beatix app: buttons + sliders + jogs]   (Kotlin / Jetpack Compose)
        │  ASCII messages over TCP -> 127.0.0.1:5557
        │  adb reverse tcp:5557 tcp:5557      (USB cable)
        ▼
[Mac bridge: beatix_bridge.py]   (reuse mido + python-rtmidi venv)
        │  emits to virtual MIDI port "Beatix"
        ▼
[Rekordbox]  -> MIDI-LEARN once (buttons=Trigger, sliders=Absolute CC)
```
Rekordbox only speaks MIDI; ADB just moves bytes over USB. The bridge converts
bytes → a virtual MIDI device. ~80 lines, mostly reused from the earlier
keyboard bridge (swap key listener for a socket reader).

## 2. Target device — Redmi Note 9 Pro
- 6.67" IPS LCD, 1080 × 2400 px, ~440 dpi (scale ≈ 2.75x), 20:9.
- **Landscape design canvas (immersive fullscreen): ≈ 873 × 393 dp.**
- Confirm on device: `adb shell wm size` (2400x1080), `adb shell wm density` (~440).
- Lock landscape; hide status + nav bars (immersive sticky) to reclaim full width.

## 3. UI layout (Cross DJ / WeDJ inspired, fitted to 873 × 393 dp)
NOTE: controller only — NO waveform/art/BPM (MIDI sends none). Jogs are plain
control surfaces. Visual feedback stays on the Mac.

```
 ┌─────────────────────────── 873 dp ───────────────────────────┐
 │ top bar  ~34dp: [browse ▲▼]   DeckA ● REC   ⚙        DeckB     │
 ├───────────────────────────────────────────────────────────────┤
 │ T  H M L  F |          |  center  |          | F  H M L  T     │ ~280dp
 │ E  q q q  d |   JOG A  |  FX LOOP |   JOG B  | d  q q q  E     │
 │ M  i i i  r |  (Ø165)  |  ▲ ▼ grid|  (Ø165)  | r  i i i  M     │
 │ P  EQ A   A |  [hc1-3] | LoadA/B  | [hc1-3]  | B  EQ B   P     │
 ├───────────────────────────────────────────────────────────────┤
 │ [CUE A][PLAY A][SYNC A]  ◀═ crossfader ═▶  [SYNC B][PLAY B][CUE B] │ ~72dp
 └───────────────────────────────────────────────────────────────┘
```
Width budget (≈): tempo 44 + EQ(3×30)=90 + chFader 34 + jog 165 + center 110
+ jog 165 + chFader 34 + EQ 90 + tempo 44 = 776 dp (+gaps) → fits in 873.
Height: top 34 + middle 280 + transport 72 = ~386 dp → fits in 393.

### Controls per zone
- Outer edges: vertical **Tempo** slider per deck (like the references).
- Inner of each deck: **EQ Hi/Mid/Low** vertical sliders + **Channel Fader**.
- **Jog** (Ø~165dp): drag = pitch-bend/nudge (relative CC). Not for scratching.
- Under each jog: 3 small **Hot Cue** pads.
- Center column: **FX** on/off + param, **Loop** (4-beat), **browse ▲▼**,
  **Load A / Load B**, library/grid toggle.
- Bottom: **CUE / PLAY / SYNC** per deck + horizontal **Crossfader** (center).

### Touch rules
- Min hit target 48dp (slider tracks can look thin but expand touch slop).
- **Multi-touch mandatory**: Compose `pointerInput` + `awaitEachGesture` so you
  can hold a cue while riding an EQ. Track pointers by id.
- Double-tap EQ slider = snap to center (reset). Long-press fader = kill to 0.
- Visual press feedback (color/scale) since there's no audio feedback on phone.

## 4. Wire protocol (line-based ASCII, low latency)
- Button:  `N <note> 1`  /  `N <note> 0`
- Slider:  `C <cc> <0-127>`   (absolute; coalesced to ≤60 Hz)
- Jog:     `J <cc> <delta>`   (relative; +1 / 127 style)
- Heartbeat: `P` every 2 s.

## 5. MIDI map (channel 1)
Buttons→Notes: DeckA Play36 Cue37 Sync38 HC39/40/41 Loop42 LoopExit43 FX44;
DeckB Play60 Cue61 Sync62 HC63/64/65 Loop66 LoopExit67 FX68;
Browse Up50 Down51 LoadA52 LoadB53.
Sliders→Abs CC: DeckA EQhi1 mid2 low3 fader4 fxparam5;
DeckB EQhi11 mid12 low13 fader14 fxparam15; Crossfader20.
Jog→rel CC: DeckA 30, DeckB 31, Tempo A 21 / B 22.

## 6. Tech stack
Kotlin · Jetpack Compose · Material 3 · minSdk 24 · targetSdk latest ·
java.net.Socket on Dispatchers.IO coroutine · no extra libs.

## 7. Project structure
```
Beatix/                         (~/Projects/Beatix)
  docs/                         (this folder)
  app/                          (Android module - to scaffold)
    src/main/java/com/beatix/
      MainActivity.kt           # landscape lock + immersive + Compose root
      ui/ConsoleScreen.kt       # the layout above
      ui/Pad.kt ui/VFader.kt ui/Jog.kt ui/Crossfader.kt
      net/MidiClient.kt         # socket connect/send/reconnect
      Mapping.kt                # note/CC constants (section 5)
    build.gradle.kts
  mac/beatix_bridge.py          # socket -> virtual MIDI (reuse venv)
  run-beatix.command            # adb reverse + start bridge
```

## 8. Toolchain → produce the APK
1. Android Studio (free) OR SDK + `brew install gradle`. (`adb` already installed.)
2. Build debug APK (personal use, no signing/Play Store):
   `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
3. Phone: enable Developer Options + USB debugging.
4. `adb install app-debug.apk`

## 9. Per-session startup (wrapped in run-beatix.command)
1. Plug phone (USB debugging on).  2. `adb reverse tcp:5557 tcp:5557`
3. Start `beatix_bridge.py`.        4. Open Rekordbox AFTER bridge.
5. Open Beatix app → connects → MIDI-LEARN controls (one time) → play.

## 10. Milestones (smallest-provable-first)
- M1  Mac bridge + prove ONE slider moves a Rekordbox EQ knob (de-risks EQ).
- M2  App: socket connect + Play A / Play B end-to-end.
- M3  Full button set (cues, sync, loops, FX, browse/load).
- M4  Sliders: EQ×3 + fader + crossfader (absolute CC).
- M5  Jogs (nudge), layout polish, multitouch, double-tap reset, landscape.
- M6  Build APK + session launcher + on-device MIDI-LEARN cheat sheet.

## 11. Risks / honest caveats
- No waveform/art/BPM/time on phone (controller, not player).
- Library can't be mirrored → browse/load buttons; watch the Mac screen.
- Jogs are nudge-only; true scratch needs sub-10ms round-trip and audio on-device.
- Phone is tight: EQ sliders ~150dp tall — usable, a tablet would be roomier.
- USB/ADB latency fine (~ms); avoid WiFi. ADB reverse re-run on each replug.

## 12. Open decisions before coding
- Package id (default `com.beatix`), port (default 5557).
- Android Studio (GUI) vs pure Gradle CLI for builds.
- Jog wheels in v1, or defer to v2 and ship EQ/transport first?
