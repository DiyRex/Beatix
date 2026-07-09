========================================
  BEATIX + MIXXX  (100% FREE — no paywall)
========================================

Why Mixxx: rekordbox's FREE plan blocks MIDI controllers (needs a paid Core plan
or real Pioneer hardware). Mixxx is free and open-source and maps any MIDI
controller with no unlock. Beatix already works — Mixxx just reads it.

--------------------------------------------------
ONE-TIME SETUP (Windows PC)
--------------------------------------------------
1. Make sure the Beatix bridge + loopMIDI ("Beatix" port) are running
   (same as before — the bridge window should say "MIDI port 'Beatix' is ready").

2. Install Mixxx (free):  https://mixxx.org/download/

3. Copy BOTH files from this folder:
      Beatix.midi.xml
      Beatix-scripts.js
   into Mixxx's user controllers folder:
      %LOCALAPPDATA%\Mixxx\controllers\
   (paste that path into Explorer's address bar; create the "controllers"
    folder if it isn't there.)

4. Open Mixxx -> Preferences -> Controllers.
   - Click the "Beatix" device in the left list (that's the loopMIDI port).
   - Load Preset -> choose "Beatix".
   - Tick "Enabled".  Click OK.

5. Open the Beatix app on the phone (Wi-Fi or USB, same as before).
   Move a fader -> it moves in Mixxx. Done.

--------------------------------------------------
WHAT'S MAPPED
--------------------------------------------------
Per deck: Play, Cue, Sync, Loop In/Out, 4-Beat loop, Reloop, Shift=Start,
          8 hot cues, EQ Hi/Mid/Low (knobs, centered=unity), TRIM, CFX filter,
          Volume fader, Tempo, Headphone CUE.
Center:   Load A/B, Crossfader, Beat FX (ON, →1/→2 assign, DEPTH/mix),
          BEAT ◄/► = previous/next effect (yes — effect select works here!),
          FX PARAM ◄/► = nudge the effect amount.

Tip: in Mixxx, right-click any on-screen control -> you can also re-learn it to a
Beatix button/fader if you want to change something.
