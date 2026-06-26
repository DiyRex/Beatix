package com.beatix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun ConsoleScreen(midi: MidiClient) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Deck(DeckA, side = Side.LEFT, midi = midi, modifier = Modifier.weight(1f))
        Mixer(midi = midi, modifier = Modifier.weight(1.05f))
        Deck(DeckB, side = Side.RIGHT, midi = midi, modifier = Modifier.weight(1f))
    }
}

enum class Side { LEFT, RIGHT }

enum class PadMode { HOTCUE, PADFX, BEATJUMP, SAMPLER }

@Composable
private fun Deck(ids: DeckIds, side: Side, midi: MidiClient, modifier: Modifier) {
    var mode by remember { mutableStateOf(PadMode.HOTCUE) }
    var shift by remember { mutableStateOf(false) }
    val bank = when (mode) {
        PadMode.HOTCUE -> ids.pads
        PadMode.PADFX -> ids.padFx
        PadMode.BEATJUMP -> ids.beatJump
        PadMode.SAMPLER -> ids.sampler
    }
    val deckBody: @Composable (Modifier) -> Unit = { m ->
        Column(
            modifier = m
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Panel)
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // top: SHIFT + loop/sync.  SHIFT turns SYNC->MASTER and 4BEAT->EXIT.
            Row(
                Modifier.fillMaxWidth().weight(0.13f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ShiftButton(shift, { shift = it }, Modifier.weight(0.85f).fillMaxHeight())
                Pad("IN", ids.loopIn, midi, Modifier.weight(0.9f).fillMaxHeight())
                Pad("OUT", ids.loopOut, midi, Modifier.weight(0.9f).fillMaxHeight())
                Pad("4BEAT", ids.fourBeat, midi, Modifier.weight(1f).fillMaxHeight())
                Pad("SYNC", ids.sync, midi, Modifier.weight(1f).fillMaxHeight(), accent = Amber)
            }
            Box(
                Modifier.fillMaxWidth().weight(0.26f),
                contentAlignment = Alignment.Center,
            ) { Jog(ids.jogFwd, ids.jogBack, midi) }
            // pad-mode tabs
            Row(
                Modifier.fillMaxWidth().weight(0.09f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ModeTab("HOT CUE", PadMode.HOTCUE, mode, ids.modeHotcue, midi, { mode = it }, Modifier.weight(1f))
                ModeTab("PAD FX", PadMode.PADFX, mode, ids.modePadfx, midi, { mode = it }, Modifier.weight(1f))
                ModeTab("JUMP", PadMode.BEATJUMP, mode, ids.modeBeatjump, midi, { mode = it }, Modifier.weight(1f))
                ModeTab("SMPLR", PadMode.SAMPLER, mode, ids.modeSampler, midi, { mode = it }, Modifier.weight(1f))
            }
            // pads — current mode's bank
            Column(
                Modifier.fillMaxWidth().weight(0.28f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    bank.take(4).forEachIndexed { i, p ->
                        Pad("${i + 1}", p, midi, Modifier.weight(1f).fillMaxHeight(), accent = Amber)
                    }
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    bank.drop(4).forEachIndexed { i, p ->
                        Pad("${i + 5}", p, midi, Modifier.weight(1f).fillMaxHeight(), accent = Amber)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().weight(0.24f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Pad(if (shift) "↪ START" else "CUE", if (shift) ids.shiftCue else ids.cue, midi, Modifier.weight(1f).fillMaxHeight(), accent = GrayBtn)
                Pad("PLAY", ids.play, midi, Modifier.weight(1f).fillMaxHeight(), accent = Amber)
            }
        }
    }

    val tempo: @Composable (Modifier) -> Unit = { m ->
        LabeledFader("TEMPO", ids.tempo, 64, midi, m, centerTick = true)
    }

    Row(modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (side == Side.LEFT) {
            tempo(Modifier.weight(0.11f))
            deckBody(Modifier.weight(1f))
        } else {
            deckBody(Modifier.weight(1f))
            tempo(Modifier.weight(0.11f))
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    mode: PadMode,
    current: PadMode,
    note: Int,
    midi: MidiClient,
    onSelect: (PadMode) -> Unit,
    modifier: Modifier,
) {
    val active = mode == current
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Amber else PadIdle)
            .border(1.dp, Amber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .pointerInput(note) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onSelect(mode)
                    midi.note(note, true)
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.changes.none { it.pressed }) break
                    }
                    midi.note(note, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Color.Black else TextCol, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShiftButton(active: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Amber else PadIdle)
            .border(1.dp, Amber.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .pointerInput(active) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onToggle(!active)
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.changes.none { it.pressed }) break
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("SHIFT", color = if (active) Color.Black else TextCol, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/** Button that synthesizes a Mac keystroke via the bridge (used for browse). */
@Composable
private fun KeyButton(label: String, keycode: Int, shift: Boolean, midi: MidiClient, modifier: Modifier) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) Amber else PadIdle)
            .border(1.dp, Amber.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .pointerInput(keycode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    midi.key(keycode, shift)
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.changes.none { it.pressed }) break
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (pressed) Color.Black else TextCol, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Mixer(midi: MidiClient, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // top: LOAD A | browse | LOAD B
        Row(
            Modifier.fillMaxWidth().weight(0.13f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Pad("LOAD A", Center.LOAD_A, midi, Modifier.weight(1.2f).fillMaxHeight())
            KeyButton("▲", Center.KEY_ARROW_UP, false, midi, Modifier.weight(0.7f).fillMaxHeight())
            KeyButton("▼", Center.KEY_ARROW_DOWN, false, midi, Modifier.weight(0.7f).fillMaxHeight())
            Pad("LOAD B", Center.LOAD_B, midi, Modifier.weight(1.2f).fillMaxHeight())
        }
        // EQ row (CH1 | BEAT FX | CH2) — EQ only, no channel faders here
        Row(
            Modifier.fillMaxWidth().weight(0.46f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            EqStrip("CH 1", DeckA.hi, DeckA.mid, DeckA.low, midi, Modifier.weight(1f))
            FxColumn(midi, Modifier.weight(0.85f))
            EqStrip("CH 2", DeckB.hi, DeckB.mid, DeckB.low, midi, Modifier.weight(1f))
        }
        // VOLUME row — narrower faders, aligned under their EQ strips
        Row(
            Modifier.fillMaxWidth().weight(0.28f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                LabeledFader("VOL 1", DeckA.fader, 110, midi, Modifier.fillMaxWidth(0.5f).fillMaxHeight())
            }
            Spacer(Modifier.weight(0.85f))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                LabeledFader("VOL 2", DeckB.fader, 110, midi, Modifier.fillMaxWidth(0.5f).fillMaxHeight())
            }
        }
        // crossfader — smaller (rarely used)
        Column(Modifier.fillMaxWidth().weight(0.13f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CROSSFADER", color = Amber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                HFader(Center.XFADER, midi, initial = 64, modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.65f))
            }
        }
    }
}

@Composable
private fun EqStrip(label: String, hi: Int, mid: Int, low: Int, midi: MidiClient, modifier: Modifier) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EqFader("HI", hi, midi, Modifier.weight(1f))
            EqFader("MID", mid, midi, Modifier.weight(1f))
            EqFader("LOW", low, midi, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EqFader(label: String, cc: Int, midi: MidiClient, modifier: Modifier) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        VFader(cc, midi, initial = 64, modifier = Modifier.weight(1f).padding(horizontal = 3.dp), centerTick = true)
        Text(label, color = TextCol, fontSize = 8.sp)
    }
}

@Composable
private fun LabeledFader(label: String, cc: Int, initial: Int, midi: MidiClient, modifier: Modifier, centerTick: Boolean = false) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        VFader(cc, midi, initial = initial, modifier = Modifier.weight(1f).padding(horizontal = 3.dp), centerTick = centerTick)
        Text(label, color = TextCol, fontSize = 8.sp)
    }
}

@Composable
private fun FxColumn(midi: MidiClient, modifier: Modifier) {
    Column(
        modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("BEAT FX", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Pad("ON", Center.FX_ONOFF, midi, Modifier.fillMaxWidth().weight(0.9f), accent = Amber)
        Pad("SEL", Center.FX_SELECT, midi, Modifier.fillMaxWidth().weight(0.9f))
        VFader(Center.FX_LEVEL, midi, initial = 0, modifier = Modifier.fillMaxWidth().weight(1.3f))
        Row(Modifier.fillMaxWidth().weight(0.8f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StepButton("◄", Center.BEAT_L, -1, midi, Modifier.weight(1f).fillMaxHeight(), note = true)
            StepButton("►", Center.BEAT_R, +1, midi, Modifier.weight(1f).fillMaxHeight(), note = true)
        }
    }
}

@Composable
private fun Pad(
    label: String,
    note: Int,
    midi: MidiClient,
    modifier: Modifier,
    accent: Color = PadIdle,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) accent else PadIdle)
            .border(1.dp, (if (accent == PadIdle) Amber else accent).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .pointerInput(note) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    midi.note(note, true)
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.none { it.pressed }) break
                    }
                    pressed = false
                    midi.note(note, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) {
            Text(
                label,
                color = if (pressed) Color.Black else TextCol,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Momentary button that emits one relative step (rotary) or a note tick on press. */
@Composable
private fun StepButton(label: String, id: Int, delta: Int, midi: MidiClient, modifier: Modifier, note: Boolean = false) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) Amber else PadIdle)
            .border(1.dp, Amber.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .pointerInput(id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (note) midi.note(id, true) else midi.jog(id, delta)
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.none { it.pressed }) break
                    }
                    if (note) midi.note(id, false)
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (pressed) Color.Black else TextCol, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Jog(fwdNote: Int, backNote: Int, midi: MidiClient) {
    // Rekordbox ignores JogRotate from non-Pioneer gear, so each tick of rotation
    // fires a PitchBend Up/Down pulse (the documented third-party jog workaround).
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color(0xFF161619))
            .border(3.dp, Amber.copy(alpha = 0.55f), CircleShape)
            .pointerInput(fwdNote) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var lastAngle = atan2(down.position.y - cy, down.position.x - cx)
                    var acc = 0f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val chg = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!chg.pressed) break
                        val a = atan2(chg.position.y - cy, chg.position.x - cx)
                        val piF = PI.toFloat()
                        val twoPiF = (2 * PI).toFloat()
                        var d = a - lastAngle
                        if (d > piF) d -= twoPiF
                        if (d < -piF) d += twoPiF
                        lastAngle = a
                        acc += d
                        val step = 0.14f // radians per emitted pulse
                        while (acc > step) { midi.note(fwdNote, true); midi.note(fwdNote, false); acc -= step }
                        while (acc < -step) { midi.note(backNote, true); midi.note(backNote, false); acc += step }
                        chg.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize(0.42f)
                .clip(CircleShape)
                .background(Color(0xFF0B0B0D))
                .border(2.dp, Amber.copy(alpha = 0.4f), CircleShape),
        )
    }
}

@Composable
private fun VFader(cc: Int, midi: MidiClient, initial: Int, modifier: Modifier, centerTick: Boolean = false) {
    var frac by remember { mutableFloatStateOf(initial / 127f) }
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(PanelDark)
            .pointerInput(cc) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    frac = (1f - down.position.y / h).coerceIn(0f, 1f)
                    midi.cc(cc, (frac * 127).toInt())
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        frac = (1f - ch.position.y / h).coerceIn(0f, 1f)
                        midi.cc(cc, (frac * 127).toInt())
                        ch.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawLine(Color(0xFF3A3A40), Offset(w / 2, 5f), Offset(w / 2, h - 5f), strokeWidth = 3f)
            if (centerTick) drawLine(Color(0xFF6A6A72), Offset(3f, h / 2), Offset(w - 3f, h / 2), strokeWidth = 2f)
            val thumbY = (1f - frac) * (h - 16f) + 8f
            drawRoundRect(
                color = Amber,
                topLeft = Offset(1f, thumbY - 8f),
                size = Size(w - 2f, 16f),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
    }
}

@Composable
private fun HFader(cc: Int, midi: MidiClient, initial: Int, modifier: Modifier) {
    var frac by remember { mutableFloatStateOf(initial / 127f) }
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(PanelDark)
            .pointerInput(cc) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    frac = (down.position.x / w).coerceIn(0f, 1f)
                    midi.cc(cc, (frac * 127).toInt())
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        frac = (ch.position.x / w).coerceIn(0f, 1f)
                        midi.cc(cc, (frac * 127).toInt())
                        ch.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawLine(Color(0xFF3A3A40), Offset(6f, h / 2), Offset(w - 6f, h / 2), strokeWidth = 3f)
            val thumbX = frac * (w - 20f) + 10f
            drawRoundRect(
                color = Amber,
                topLeft = Offset(thumbX - 10f, 2f),
                size = Size(20f, h - 4f),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
    }
}
