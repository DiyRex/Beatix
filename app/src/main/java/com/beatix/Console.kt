package com.beatix

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2

enum class Side { LEFT, RIGHT }
enum class PadMode { HOTCUE, PADFX, BEATJUMP, SAMPLER }

private val panelBrush = Brush.verticalGradient(listOf(PanelHi, Panel))

@Composable
fun ConsoleScreen(midi: MidiClient) {
    Row(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF111116), Bg)))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Deck(DeckA, Side.LEFT, midi, Modifier.weight(1f))
        Mixer(midi, Modifier.weight(1.05f))
        Deck(DeckB, Side.RIGHT, midi, Modifier.weight(1f))
    }
}

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
            m.fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(panelBrush).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(Modifier.fillMaxWidth().weight(0.13f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pad("IN", ids.loopIn, midi, Modifier.weight(1f).fillMaxHeight())
                Pad("OUT", ids.loopOut, midi, Modifier.weight(1f).fillMaxHeight())
                Pad("4 BEAT", ids.fourBeat, midi, Modifier.weight(1.3f).fillMaxHeight())
                Pad("EXIT", ids.loopExit, midi, Modifier.weight(1f).fillMaxHeight())
            }
            Row(
                Modifier.fillMaxWidth().weight(0.31f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (side == Side.LEFT) {
                    ShiftButton(shift, { shift = it }, Modifier.weight(0.34f).fillMaxHeight(0.74f))
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Jog(ids.jogFwd, ids.jogBack, midi) }
                    Pad("SYNC", ids.sync, midi, Modifier.weight(0.34f).fillMaxHeight(0.74f), accent = Amber)
                } else {
                    Pad("SYNC", ids.sync, midi, Modifier.weight(0.34f).fillMaxHeight(0.74f), accent = Amber)
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Jog(ids.jogFwd, ids.jogBack, midi) }
                    ShiftButton(shift, { shift = it }, Modifier.weight(0.34f).fillMaxHeight(0.74f))
                }
            }
            Row(Modifier.fillMaxWidth().weight(0.09f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ModeTab("HOT CUE", PadMode.HOTCUE, mode, ids.modeHotcue, midi, { mode = it }, Modifier.weight(1f))
                ModeTab("PAD FX", PadMode.PADFX, mode, ids.modePadfx, midi, { mode = it }, Modifier.weight(1f))
                ModeTab("JUMP", PadMode.BEATJUMP, mode, ids.modeBeatjump, midi, { mode = it }, Modifier.weight(1f))
                ModeTab("SMPLR", PadMode.SAMPLER, mode, ids.modeSampler, midi, { mode = it }, Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().weight(0.28f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    bank.take(4).forEachIndexed { i, p -> Pad("${i + 1}", p, midi, Modifier.weight(1f).fillMaxHeight()) }
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    bank.drop(4).forEachIndexed { i, p -> Pad("${i + 5}", p, midi, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
            Row(Modifier.fillMaxWidth().weight(0.21f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Pad(if (shift) "↪ START" else "CUE", if (shift) ids.shiftCue else ids.cue, midi, Modifier.weight(1f).fillMaxHeight(), accent = GrayBtn)
                Pad("▶ ‖", ids.play, midi, Modifier.weight(1f).fillMaxHeight(), accent = Amber)
            }
        }
    }
    val tempo: @Composable (Modifier) -> Unit = { m ->
        Column(m.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            VFader(ids.tempo, midi, 64, Modifier.weight(1f).fillMaxWidth(0.6f), centerTick = true, centerFill = true)
            Text("TEMPO", color = TextDim, fontSize = 7.sp)
        }
    }
    Row(modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (side == Side.LEFT) {
            tempo(Modifier.weight(0.1f)); deckBody(Modifier.weight(1f))
        } else {
            deckBody(Modifier.weight(1f)); tempo(Modifier.weight(0.1f))
        }
    }
}

@Composable
private fun Mixer(midi: MidiClient, modifier: Modifier) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(panelBrush).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().weight(0.12f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Pad("LOAD\nA", Center.LOAD_A, midi, Modifier.weight(1.2f).fillMaxHeight())
            KeyButton("▲", Center.KEY_ARROW_UP, false, midi, Modifier.weight(0.7f).fillMaxHeight())
            KeyButton("▼", Center.KEY_ARROW_DOWN, false, midi, Modifier.weight(0.7f).fillMaxHeight())
            Pad("LOAD\nB", Center.LOAD_B, midi, Modifier.weight(1.2f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().weight(0.74f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChannelStrip("CH 1", DeckA, midi, Modifier.weight(1f))
            FxColumn(midi, Modifier.weight(0.95f))
            ChannelStrip("CH 2", DeckB, midi, Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().weight(0.13f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CROSSFADER", color = Amber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                HFader(Center.XFADER, midi, 64, Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.62f))
            }
        }
    }
}

@Composable
private fun ChannelStrip(label: String, ids: DeckIds, midi: MidiClient, modifier: Modifier) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            EqFader("HI", ids.hi, midi, Modifier.weight(1f))
            EqFader("MID", ids.mid, midi, Modifier.weight(1f))
            EqFader("LOW", ids.low, midi, Modifier.weight(1f))
        }
        Text("VOL", color = TextDim, fontSize = 7.sp)
        Box(Modifier.fillMaxWidth().weight(0.5f), contentAlignment = Alignment.Center) {
            VFader(ids.fader, midi, 110, Modifier.fillMaxWidth(0.42f).fillMaxHeight())
        }
    }
}

@Composable
private fun EqFader(label: String, cc: Int, midi: MidiClient, modifier: Modifier) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        VFader(cc, midi, 64, Modifier.weight(1f).fillMaxWidth(0.72f), centerTick = true, centerFill = true)
        Text(label, color = TextDim, fontSize = 7.sp)
    }
}

@Composable
private fun FxColumn(midi: MidiClient, modifier: Modifier) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1D1D24), PanelDark))).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("BEAT FX", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Pad("ON", Center.FX_ONOFF, midi, Modifier.fillMaxWidth().weight(1.2f), accent = Amber)
        Row(Modifier.fillMaxWidth().weight(0.9f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Pad("→ 1", Center.FX_ASSIGN1, midi, Modifier.weight(1f).fillMaxHeight())
            Pad("→ 2", Center.FX_ASSIGN2, midi, Modifier.weight(1f).fillMaxHeight())
        }
        Pad("SEL", Center.FX_SELECT, midi, Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth().weight(0.9f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            StepButton("◄", Center.BEAT_L, -1, midi, Modifier.weight(1f).fillMaxHeight(), note = true)
            StepButton("►", Center.BEAT_R, +1, midi, Modifier.weight(1f).fillMaxHeight(), note = true)
        }
        Text("DEPTH", color = TextDim, fontSize = 7.sp)
        VFader(Center.FX_LEVEL, midi, 0, Modifier.fillMaxWidth(0.5f).weight(1.5f))
    }
}

// ---- polished controls ----

@Composable
private fun Pad(label: String, note: Int, midi: MidiClient, modifier: Modifier, accent: Color = Amber) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(70), label = "pad")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(11.dp))
            .background(if (pressed) Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.25f), accent)) else Brush.verticalGradient(listOf(PadTop, PadBot)))
            .border(1.dp, if (pressed) lerp(accent, Color.White, 0.3f) else accent.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
            .pointerInput(note) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false); pressed = true; midi.note(note, true)
                    while (true) { val e = awaitPointerEvent(); if (e.changes.none { it.pressed }) break }
                    pressed = false; midi.note(note, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) Text(label, color = if (pressed) Color.Black else TextCol, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StepButton(label: String, id: Int, delta: Int, midi: MidiClient, modifier: Modifier, note: Boolean = false) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(70), label = "step")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(9.dp))
            .background(if (pressed) Brush.verticalGradient(listOf(AmberHi, Amber)) else Brush.verticalGradient(listOf(PadTop, PadBot)))
            .border(1.dp, if (pressed) AmberHi else Edge, RoundedCornerShape(9.dp))
            .pointerInput(id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false); pressed = true
                    if (note) midi.note(id, true) else midi.jog(id, delta)
                    while (true) { val e = awaitPointerEvent(); if (e.changes.none { it.pressed }) break }
                    if (note) midi.note(id, false); pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (pressed) Color.Black else TextCol, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun KeyButton(label: String, keycode: Int, shift: Boolean, midi: MidiClient, modifier: Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, tween(70), label = "key")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(9.dp))
            .background(if (pressed) Brush.verticalGradient(listOf(AmberHi, Amber)) else Brush.verticalGradient(listOf(PadTop, PadBot)))
            .border(1.dp, if (pressed) AmberHi else Edge, RoundedCornerShape(9.dp))
            .pointerInput(keycode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false); pressed = true; midi.key(keycode, shift)
                    while (true) { val e = awaitPointerEvent(); if (e.changes.none { it.pressed }) break }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (pressed) Color.Black else TextCol, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ModeTab(label: String, mode: PadMode, current: PadMode, note: Int, midi: MidiClient, onSelect: (PadMode) -> Unit, modifier: Modifier) {
    val active = mode == current
    Box(
        modifier.fillMaxHeight().clip(RoundedCornerShape(7.dp))
            .background(if (active) Brush.verticalGradient(listOf(AmberHi, Amber)) else Brush.verticalGradient(listOf(PadTop, PadBot)))
            .border(1.dp, if (active) AmberHi else Edge, RoundedCornerShape(7.dp))
            .pointerInput(note) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false); onSelect(mode); midi.note(note, true)
                    while (true) { val e = awaitPointerEvent(); if (e.changes.none { it.pressed }) break }
                    midi.note(note, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (active) Color.Black else TextCol, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ShiftButton(active: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(9.dp))
            .background(if (active) Brush.verticalGradient(listOf(AmberHi, Amber)) else Brush.verticalGradient(listOf(PadTop, PadBot)))
            .border(1.dp, if (active) AmberHi else Edge, RoundedCornerShape(9.dp))
            .pointerInput(active) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false); onToggle(!active)
                    while (true) { val e = awaitPointerEvent(); if (e.changes.none { it.pressed }) break }
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text("SHIFT", color = if (active) Color.Black else TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Jog(fwdNote: Int, backNote: Int, midi: MidiClient) {
    Box(
        Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape)
            .pointerInput(fwdNote) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width / 2f; val cy = size.height / 2f
                    var last = atan2(down.position.y - cy, down.position.x - cx)
                    var acc = 0f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        val a = atan2(ch.position.y - cy, ch.position.x - cx)
                        val piF = PI.toFloat(); val twoPiF = (2 * PI).toFloat()
                        var d = a - last
                        if (d > piF) d -= twoPiF
                        if (d < -piF) d += twoPiF
                        last = a; acc += d
                        val step = 0.14f
                        while (acc > step) { midi.note(fwdNote, true); midi.note(fwdNote, false); acc -= step }
                        while (acc < -step) { midi.note(backNote, true); midi.note(backNote, false); acc += step }
                        ch.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF2B2B33), Color(0xFF0E0E12)), center = c, radius = r), radius = r, center = c)
            drawCircle(Amber.copy(alpha = 0.5f), radius = r - 2f, center = c, style = Stroke(3f))
            drawCircle(Color.Black.copy(alpha = 0.45f), radius = r * 0.46f, center = c)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1C1C22), Color(0xFF09090B)), center = c, radius = r * 0.44f), radius = r * 0.44f, center = c)
            drawCircle(Amber.copy(alpha = 0.4f), radius = r * 0.44f, center = c, style = Stroke(2f))
        }
    }
}

@Composable
private fun VFader(cc: Int, midi: MidiClient, initial: Int, modifier: Modifier, centerTick: Boolean = false, centerFill: Boolean = false) {
    var frac by remember { mutableFloatStateOf(initial / 127f) }
    Box(
        modifier
            .pointerInput(cc) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    frac = (1f - down.position.y / h).coerceIn(0f, 1f); midi.cc(cc, (frac * 127).toInt())
                    while (true) {
                        val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        frac = (1f - ch.position.y / h).coerceIn(0f, 1f); midi.cc(cc, (frac * 127).toInt()); ch.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val cx = w / 2f
            val tw = (w * 0.26f).coerceIn(5f, 11f)
            val top = 9f; val bot = h - 9f; val usable = (bot - top).coerceAtLeast(1f)
            drawRoundRect(Color(0xFF050507), topLeft = Offset(cx - tw / 2, top), size = Size(tw, usable), cornerRadius = CornerRadius(tw / 2, tw / 2))
            val thumbY = top + (1f - frac) * usable
            if (centerFill) {
                val midY = top + usable / 2
                val a = minOf(midY, thumbY); val b = maxOf(midY, thumbY)
                drawRoundRect(Amber.copy(alpha = 0.85f), topLeft = Offset(cx - tw / 2, a), size = Size(tw, b - a))
            } else {
                drawRoundRect(Amber.copy(alpha = 0.85f), topLeft = Offset(cx - tw / 2, thumbY), size = Size(tw, bot - thumbY), cornerRadius = CornerRadius(tw / 2, tw / 2))
            }
            if (centerTick) drawLine(Color(0xFF6A6A72), Offset(3f, top + usable / 2), Offset(w - 3f, top + usable / 2), strokeWidth = 2f)
            val th = 18f; val thw = w * 0.9f
            drawRoundRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(cx - thw / 2 + 1, thumbY - th / 2 + 2), size = Size(thw, th), cornerRadius = CornerRadius(5f, 5f))
            drawRoundRect(Brush.verticalGradient(listOf(AmberHi, AmberLo)), topLeft = Offset(cx - thw / 2, thumbY - th / 2), size = Size(thw, th), cornerRadius = CornerRadius(5f, 5f))
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - thw / 2 + 3, thumbY), Offset(cx + thw / 2 - 3, thumbY), strokeWidth = 1.5f)
        }
    }
}

@Composable
private fun HFader(cc: Int, midi: MidiClient, initial: Int, modifier: Modifier) {
    var frac by remember { mutableFloatStateOf(initial / 127f) }
    Box(
        modifier
            .pointerInput(cc) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    frac = (down.position.x / w).coerceIn(0f, 1f); midi.cc(cc, (frac * 127).toInt())
                    while (true) {
                        val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        frac = (ch.position.x / w).coerceIn(0f, 1f); midi.cc(cc, (frac * 127).toInt()); ch.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val cy = h / 2f
            val th = (h * 0.32f).coerceIn(5f, 12f)
            val left = 10f; val right = w - 10f; val usable = (right - left).coerceAtLeast(1f)
            drawRoundRect(Color(0xFF050507), topLeft = Offset(left, cy - th / 2), size = Size(usable, th), cornerRadius = CornerRadius(th / 2, th / 2))
            drawLine(Color(0xFF6A6A72), Offset(left + usable / 2, 4f), Offset(left + usable / 2, h - 4f), strokeWidth = 2f)
            val thumbX = left + frac * usable
            val tw = 20f; val thh = h * 0.9f
            drawRoundRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(thumbX - tw / 2 + 1, cy - thh / 2 + 2), size = Size(tw, thh), cornerRadius = CornerRadius(5f, 5f))
            drawRoundRect(Brush.verticalGradient(listOf(AmberHi, AmberLo)), topLeft = Offset(thumbX - tw / 2, cy - thh / 2), size = Size(tw, thh), cornerRadius = CornerRadius(5f, 5f))
        }
    }
}
