# Beatix — Importable Mapping Files

Ready-made mappings live in the repo so you can reuse them on any machine without
re-mapping by hand.

```
rekordbox/Beatix.midi.csv     # Rekordbox 7 MIDI mapping (56 controls)
vdj/Beatix.xml                # VirtualDJ device definition
vdj/Beatix-mapper.xml         # VirtualDJ mapper (names -> actions)
```

All assume the bridge exposes a MIDI device named **Beatix** (Go bridge on macOS,
or the Python bridge + loopMIDI port named "Beatix" on Windows).

## Rekordbox
Two ways:
1. **Drop-in (auto-load):** quit Rekordbox, copy `rekordbox/Beatix.midi.csv` to
   `~/Library/Application Support/Pioneer/rekordbox6/MidiMappings/Beatix.midi.csv`
   (Windows: `%APPDATA%\Pioneer\rekordbox6\MidiMappings\`), reopen Rekordbox.
2. **Import via UI:** top-bar **MIDI** button → device **Beatix** → **IMPORT** →
   pick `rekordbox/Beatix.midi.csv`.

Tips: sliders/EQ are 7-bit absolute; jog = Pitch Bend (works on a *playing* deck);
SHIFT+Cue = jump to start; track ▲/▼ use synthesized arrow keys (bridge needs
macOS Accessibility permission). Browse via the MIDI `Rotary` function crashes
Rekordbox — don't re-add it.

## VirtualDJ
Copy both files into VirtualDJ's config:
- macOS: `~/Library/Application Support/VirtualDJ/Devices/Beatix.xml` and
  `.../VirtualDJ/Mappers/Beatix.xml`
- Windows: `Documents\VirtualDJ\Devices\Beatix.xml` and `Documents\VirtualDJ\Mappers\Beatix.xml`

Open VirtualDJ → Settings → Controllers → it shows **Beatix**. Tweak any control by
right-clicking it on screen and using **learn**. VirtualDJ (unlike Rekordbox) also
supports custom-controller **jog scratch** and **LED feedback**.

Run only one DJ app at a time.
