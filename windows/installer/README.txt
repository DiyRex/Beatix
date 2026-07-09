========================================
  BEATIX — WINDOWS INSTALL (SIMPLE)
========================================

You need: a Windows PC + your Android phone (USB cable, or same Wi-Fi).

Already installed an older Beatix? Just run the installer again — it stops the
old bridge, overwrites everything with this version, and starts the fixed bridge.
Nothing to uninstall first.

--------------------------------------------------
STEP 1  —  Install on the PC (once)
--------------------------------------------------
  1. Copy this whole "Beatix-Windows-Kit" folder onto the PC.
  2. Right-click  Install-Beatix.ps1  ->  "Run with PowerShell".
     (If it won't run: open PowerShell and paste:
        powershell -ExecutionPolicy Bypass -File Install-Beatix.ps1 )
  3. Say YES to the admin prompt. It installs everything automatically.
  4. When the loopMIDI window pops up: type   Beatix   in the box,
     click  [ + ] , and tick  "Autostart".   <-- the only manual step.

--------------------------------------------------
STEP 2  —  Install the app on the phone (once)
--------------------------------------------------
  1. Copy  beatix.apk  to the phone and tap it to install
     (allow "install from unknown apps" if asked).
  2. On the phone: Settings -> About -> tap "Build number" 7 times to
     unlock Developer options, then turn ON "USB debugging".

--------------------------------------------------
STEP 3  —  Install + set up rekordbox (once)
--------------------------------------------------
  1. Install rekordbox from rekordbox.com. Open it, switch to
     PERFORMANCE mode (top-left).
  2. Click the MIDI button (top bar) -> pick device "Beatix" -> IMPORT
     -> choose  rekordbox\Beatix.midi.csv  from this folder.
  3. Turn on the 2-deck view + Mixer panel + FX panel.
     Set the FX panel to single mode (click the 3-dots so it shows 1 dot).

--------------------------------------------------
EVERY TIME YOU PLAY
--------------------------------------------------
  1. Plug the phone into the PC with USB.
  2. Open rekordbox.
  3. Open the Beatix app on the phone.
       In the app: Settings -> "ADB / USB cable".
  4. Play. Move a fader on the phone -> it moves in rekordbox.

(loopMIDI and the Beatix bridge already auto-start with Windows.)

--------------------------------------------------
IF SOMETHING DOESN'T MOVE
--------------------------------------------------
  - In rekordbox: MIDI -> click the control on screen -> touch that
    control on the phone. That "learns" it. (Do this for TRIM/CFX/CUE
    if needed.)
  - Beat-FX EFFECT choice is picked with the MOUSE in rekordbox — that
    is a rekordbox limit, not the app. The phone still does ON / DEPTH /
    BEAT / FX PARAM / deck-assign (->1 ->2).
  - Only run ONE DJ app at a time.

Full details: see  SETUP-WINDOWS.txt  and the  docs\  folder.
