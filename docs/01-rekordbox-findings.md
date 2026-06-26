# Rekordbox 7 — Findings (the "why" behind Beatix)

All verified against rekordbox 7 on this Mac (June 2026).

## 1. Two ways to control Rekordbox
- **Keyboard shortcuts** — Rekordbox 7 supports custom presets with Import/Export
  (Preferences → Keyboard). Format below. BUT limited command set (no EQ).
- **MIDI** — full control via MIDI-LEARN from any MIDI device. This is the only
  path that reaches EQ/knobs. Beatix uses this.

## 2. Keyboard mapping file format (importable)
- Location of presets on disk:
  `~/Library/Application Support/Pioneer/rekordbox6/KeyMappings/*.mappings`
- XML, CRLF line endings, wrapper:
```
<?xml version="1.0" encoding="UTF-8"?>
<PROPERTIES>
  <VALUE name="keyMappingName" val="My Preset"/>
  <VALUE name="keyMappingXml">
    <KEYMAPPINGS basedOnDefaults="0">
      <MAPPING commandId="3006" description="Play/Pause" key="Z"/>
      ...
    </KEYMAPPINGS>
  </VALUE>
</PROPERTIES>
```
- Import: Preferences → Keyboard → preset dropdown → (Import).
- Deleting a preset: quit Rekordbox first (it rewrites on quit), then remove the
  matching `.mappings` file; reopen.

## 3. Command-ID patterns (decoded)
- Deck 1 = `30xx`, Deck 2 = `31xx` (offset +0x100).
- Mixer CH1 = `d0xx`, CH2 = `d1xx`.
- Browser load: `b129` = load to Deck 1, `b12a` = load to Deck 2.
- Verified deck/mixer IDs:
  Play 3006/3106 · Cue 3007/3107 · BEAT SYNC 304b/314b ·
  Hot Cue A/B/C 301e,301f,3020 / 311e,311f,3120 ·
  4-Beat Loop 3014/3114 · Exit/Reloop 300c/310c ·
  Pitch Bend +/- 304f,3050 / 314f,3150 ·
  CH Fader raise/lower (large) d017/d01a (CH1), d117/d11a (CH2).
- Key tokens: letters uppercase ("Z"), arrows = "cursor up/down/left/right",
  modifiers "shift + ", "command + ", "ctrl + ", "option + ", backtick = "&#96;".

## 4. THE EQ LIMITATION (key finding)
Rekordbox's **keyboard** cannot control EQ — at all. The Keyboard editor's
**Mixer** category only exposes: Trim, Auto Gain, Headphones Cue, and Ch Fader
(small/medium/large). There is **no EQ Hi/Mid/Low** command for the keyboard,
gradual or kill. Pressing an unmapped key just makes macOS beep.
→ EQ is only reachable via **MIDI** (a controller) or the mouse.
→ This is the core reason Beatix exists: a touch slider → absolute MIDI CC →
   Rekordbox EQ knob = real EQ control.

## 5. What IS keyboard-mappable (for the interim keyboard preset)
- Deck: Play, Cue, Sync, Hot Cues, Loops (in/out/exit/auto/N-beat), Pitch Bend,
  Tempo +/- (small/med/large), Memory Cues, Reverse, Slip, Key, Stems, etc.
- Mixer: Trim, Auto Gain, Headphones Cue, Ch Fader.
- FX1: Assign Deck 1-4, Select FX 1-3, **FX1 On/Off**, FX2/FX3 On/Off,
  **Increase/Decrease Parameter 1/2/3** (so FX param DOES work on keyboard),
  MULTI FX double/halve beats.
- Browser: load to deck, search, playlist nav, etc.
- Assign in the editor by clicking the **(+)** on a row, then pressing the key.

## 6. MIDI specifics for Beatix
- Buttons → MIDI Notes (learn as Trigger).
- Sliders → absolute MIDI CC (learn the knob/fader, set type **Absolute**).
- A virtual MIDI port created by a Mac helper is seen by Rekordbox if the port
  exists **before** Rekordbox launches (it scans MIDI at startup).
