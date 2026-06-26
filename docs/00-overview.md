# Beatix — Overview

**Beatix** turns an Android phone into a touch DJ control surface for
**Rekordbox 7** on macOS, connected over **USB via ADB**. It gives the things a
laptop keyboard can't: real touch faders, a crossfader, and — most importantly —
**real EQ control** (touch sliders send absolute MIDI, which Rekordbox EQ knobs
accept; the computer keyboard cannot move EQ at all).

## What Beatix is / is not
- IS: a MIDI **control surface** (buttons, sliders, jogs) → Rekordbox.
- IS NOT: a DJ player. Audio plays in Rekordbox on the Mac. The phone shows **no
  waveforms, album art, BPM, or time** — MIDI doesn't send that back. The phone
  is the hands; the Mac screen stays the eyes.

## Target hardware
- Phone: **Redmi Note 9 Pro** (6.67", 1080×2400, ~440 dpi).
- Mac running Rekordbox 7, USB cable, ADB.

## Pipeline (one line)
Android app → TCP socket over `adb reverse` (USB) → tiny Mac bridge → virtual
MIDI port "Beatix" → Rekordbox MIDI-LEARN.

## Docs in this folder
- `00-overview.md` — this file.
- `01-rekordbox-findings.md` — everything learned about Rekordbox keyboard/MIDI,
  command IDs, and the EQ limitation (the "why" behind Beatix).
- `02-build-plan.md` — the full Beatix app build plan (architecture, UI fitted to
  the Redmi screen, protocol, MIDI map, toolchain, milestones, risks).
- `03-flx4-layout.md` — the DDJ-FLX4-mirrored layout, full control inventory,
  MIDI map, and phone-fitted layout (the chosen visual/ergonomic target).
- `rekordbox-keyboard-cheatsheet.md` — the interim keyboard layout (works today
  while Beatix is built).

## Status
BUILT & running. See the top-level `../README.md` for the full build/run guide.
- Go bridge + virtual CoreMIDI "Beatix" port: done, tested.
- Kotlin/Compose APK: built, installed on the Redmi, FLX4-style dark/amber UI.
- Plug-and-play: `launchd` agent (`com.beatix`) keeps the bridge alive and
  auto-applies `adb reverse`, so Rekordbox always finds the device.
- Working: Play/Cue/Sync/Loops/4-Beat/Hot Cues/EQ/Channel faders/Tempo/Load.
- Fixed (pending on-device confirm): Crossfader, Browse, Beat FX, rotatable jog.
- Roadmap: pad-mode banks (Hot Cue / Pad FX / Beat Jump / Sampler) via a per-deck
  page-toggle on the 8-pad block.
