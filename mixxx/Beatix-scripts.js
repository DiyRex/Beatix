// Beatix — Mixxx controller script
// Handles center-detented knobs (EQ/TRIM) and FX select/param buttons.
// Paired with Beatix.midi.xml. Mixxx sends channel-1 MIDI from the loopMIDI
// "Beatix" port (fed by the Beatix bridge).

var Beatix = {};

Beatix.init = function (id, debug) {};
Beatix.shutdown = function () {};

// 0..127 (center 64)  ->  0..4 (center 1.0)  for EQ/gain, so a centered
// knob rests at unity gain instead of boosted.
Beatix._center4 = function (v) {
    return v <= 64 ? (v / 64.0) : (1.0 + (v - 64) / 63.0 * 3.0);
};

Beatix.eqLow  = function (ch, ctrl, val, status, group) { engine.setValue(group, "parameter1", Beatix._center4(val)); };
Beatix.eqMid  = function (ch, ctrl, val, status, group) { engine.setValue(group, "parameter2", Beatix._center4(val)); };
Beatix.eqHigh = function (ch, ctrl, val, status, group) { engine.setValue(group, "parameter3", Beatix._center4(val)); };
Beatix.gain   = function (ch, ctrl, val, status, group) { engine.setValue(group, "pregain",    Beatix._center4(val)); };

// --- Beat FX (EffectUnit1) ---
var FXUNIT = "[EffectRack1_EffectUnit1]";
var FXEFF1 = "[EffectRack1_EffectUnit1_Effect1]";

// FX PARAM ◄ / ► : nudge the meta (super) knob down/up
Beatix.fxParamDn = function (ch, ctrl, val) { if (val) engine.setValue(FXUNIT, "super1", Math.max(0, engine.getValue(FXUNIT, "super1") - 0.05)); };
Beatix.fxParamUp = function (ch, ctrl, val) { if (val) engine.setValue(FXUNIT, "super1", Math.min(1, engine.getValue(FXUNIT, "super1") + 0.05)); };

// BEAT ◄ / ► : previous / next effect  (this is the effect-select rekordbox wouldn't do!)
Beatix.fxPrev = function (ch, ctrl, val) { if (val) engine.setValue(FXEFF1, "effect_selector", -1); };
Beatix.fxNext = function (ch, ctrl, val) { if (val) engine.setValue(FXEFF1, "effect_selector",  1); };
