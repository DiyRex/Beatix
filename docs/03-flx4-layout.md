# Beatix — DDJ-FLX4 Layout & Control Map

Beatix mirrors the **Pioneer DDJ-FLX4** layout (Rekordbox's entry controller).
We copy its ergonomics/positions; Beatix remains a generic MIDI surface that you
MIDI-LEARN in Rekordbox (we do NOT emulate FLX4 hardware identity / unlock).

Jogs are shrunk (Ø ~110 dp vs the FLX4's huge platters) to free space for pads,
EQ, and FX — per the design goal.

## Full FLX4 control inventory (what we replicate)

### Per deck (×2: Deck A left, Deck B right)
Buttons:
- SHIFT (app-local modifier → alt layer)
- LOOP IN, LOOP OUT, 4 BEAT/EXIT (auto loop)
- CUE/LOOP CALL ◄ , ► (with shift: 1/2X DEL, 2X MEMORY)
- BEAT SYNC (shift: TEMPO RANGE)
- CUE, PLAY/PAUSE
- 8 performance pads (4×2)
- Pad-mode tabs: HOT CUE, PAD FX1, BEAT JUMP, SAMPLER
  (shift tabs: KEYBOARD, PAD FX2, BEAT LOOP, KEY SHIFT)
Continuous:
- JOG (touch = note; rotate = relative CC; shift+jog = SEARCH)
- TEMPO fader (absolute CC)

### Center / mixer
Buttons: LOAD A, LOAD B, browse-knob push (BACK), MASTER CUE,
CH1 CUE, CH2 CUE, BEAT FX ON/OFF, BEAT ◄, BEAT ►, FX SELECT
Switch: BEAT FX assign 1 / 2 / 1&2 (3-state)
Continuous (knobs on FLX4 → compact vertical sliders on phone):
- TRIM 1/2
- EQ HI 1/2, MID 1/2, LOW 1/2   ← priority (the reason for Beatix)
- CFX 1/2 (Color FX / Smart CFX)
- CH FADER 1/2 (vertical)
- CROSSFADER (horizontal)
- MASTER LEVEL, MIC LEVEL, HP MIX, HP LEVEL  ← "utility" (slide-out panel)
- BEAT FX LEVEL/DEPTH
- browse-knob rotate (relative CC)

## Beatix MIDI map (channel 1; unique numbers, learned in Rekordbox)
Notes (buttons):
```
Deck A: Play36 Cue37 Sync38 LoopIn39 LoopOut40 4Beat41 Call◄42 Call►43
        Pads44-51  ModeHotCue52 PadFX53 BeatJump54 Sampler55
Deck B: same +32  → Play68 Cue69 Sync70 ... Sampler87
Center: LoadA100 LoadB101 MasterCue102 BrowseUp103 BrowseDown104
        BeatFxOnOff105 Beat◄106 Beat►107
        FxAssign1=109 FxAssign2=110
        Ch1Cue(headphone)=111 Ch2Cue(headphone)=112
```
FX SELECT is a RELATIVE CC encoder (not a note): FxSelect=CC38 (◄ sends -1, ► sends +1).
Requires rekordbox single-FX mode + MIDI-learn.

Control Change (continuous; sliders=Absolute, jog/browse=Relative):
```
Deck A: Tempo0  Jog16
Deck B: Tempo1  Jog17
Center: Trim1=20 Trim2=21  Hi1=22 Hi2=23  Mid1=24 Mid2=25  Low1=26 Low2=27
        CFX1=28 CFX2=29  ChFader1=30 ChFader2=31  Xfader=32   (Trim/CFX now in-app)
        MasterLevel=33 MicLevel=34 HpMix=35 HpLevel=36
        BeatFxLevel=37  FxSelect(rel)=38  BrowseRotate=33
```

## Phone-fitted layout (Redmi, 873 × 393 dp, smaller jogs)
```
 ┌──────────────────────────── 873 dp ────────────────────────────┐
 │ DECK A top: IN OUT 4BEAT  CALL◄►  SYNC │ LOAD A  ⟳browse  LOAD B │ DECK B top ⤳
 ├─────────────────────────────────────────────────────────────────┤
 │ S          ╭─────╮  T │ TRIM TRIM │BEAT│ T  ╭─────╮          S   │
 │ H  pads    │ JOG │  E │ HI   HI   │ FX │ E  │ JOG │   pads   H   │ ~250dp
 │ I  (4×2)   │Ø110 │  M │ MID  MID  │ ◄► │ M  │Ø110 │   (4×2)  I   │
 │ F  CUE     ╰─────╯  P │ LOW  LOW  │SEL │ P  ╰─────╯   CUE     F   │
 │ T  PLAY  [mode tabs]  │ CFX  CFX  │ON  │    [mode tabs]  PLAY T   │
 ├─────────────────────────────────────────────────────────────────┤
 │      CH-CUE1 CH-CUE2   │ fdr1 ◀═ XFADER ═▶ fdr2 │   (utility ⚙)   │ ~70dp
 └─────────────────────────────────────────────────────────────────┘
 EQ HI/MID/LOW = vertical sliders (priority). TRIM/CFX = short sliders.
 MASTER/MIC/HP knobs live behind the ⚙ utility slide-out panel.
```

## Scope phasing (keeps v1 shippable on a phone)
- **v1 (core mix):** Play/Cue/Sync, 8 hot-cue pads, 4-beat loop + loop in/out/exit,
  EQ Hi/Mid/Low, channel faders, crossfader, tempo fader, LOAD A/B + browse,
  BEAT FX on/off + select + level. (Smaller jogs as nudge.)
- **v2:** pad modes (PAD FX/Beat Jump/Sampler/Keyboard), full BEAT FX (assign,
  beat ◄►, tap), TRIM/CFX, utility panel (master/mic/HP), jog SEARCH.
- **v3 (stretch, optional):** lower-latency jog for light scratch.

## Open decisions
- v1 scope confirm (above) — or must pads have all 4 modes from day one?
- Jog: nudge-only in v1 (recommended) vs attempt scratch later.
- Knob-style touch (rotary drag) vs vertical mini-sliders for TRIM/CFX
  (sliders are easier/precise on a phone — recommended).
```
