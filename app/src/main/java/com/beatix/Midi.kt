package com.beatix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Sends Beatix wire-protocol lines to the Mac bridge over TCP.
 * The phone reaches the Mac's bridge at 127.0.0.1:5557 via `adb reverse`.
 * Auto-reconnects; messages are queued so the UI never blocks.
 */
class MidiClient(
    initialHost: String = "127.0.0.1",
    private val port: Int = 5557,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<String>(Channel.UNLIMITED)
    private var job: Job? = null

    @Volatile
    private var sock: Socket? = null

    var host: String = initialHost
        private set

    /** Reactive connection state for Compose UI. */
    val connected = mutableStateOf(false)

    fun start() {
        job = scope.launch {
            while (isActive) {
                val target = host
                try {
                    Socket().use { s ->
                        sock = s
                        s.tcpNoDelay = true
                        s.connect(InetSocketAddress(target, port), 2000)
                        connected.value = true
                        val out = s.getOutputStream()
                        val hb = launch {
                            while (isActive) { queue.trySend("P\n"); delay(2000) }
                        }
                        for (msg in queue) {
                            out.write(msg.toByteArray(Charsets.US_ASCII))
                            out.flush()
                        }
                        hb.cancel()
                    }
                } catch (_: Exception) {
                    // fall through to reconnect
                }
                connected.value = false
                sock = null
                delay(600)
            }
        }
    }

    /** Switch target host (ADB=127.0.0.1, or the Mac's USB-tether / Wi-Fi IP). */
    fun setHost(h: String) {
        host = h
        try { sock?.close() } catch (_: Exception) {} // break current link -> reconnect to new host
    }

    private fun enqueue(line: String) {
        queue.trySend(if (line.endsWith("\n")) line else "$line\n")
    }

    fun note(n: Int, on: Boolean) = enqueue("N $n ${if (on) 1 else 0}")
    fun cc(c: Int, v: Int) = enqueue("C $c ${v.coerceIn(0, 127)}")
    fun jog(c: Int, delta: Int) = enqueue("J $c $delta")
    fun key(keycode: Int, shift: Boolean = false) = enqueue("K $keycode ${if (shift) 1 else 0}")

    fun stop() {
        job?.cancel()
        scope.cancel()
    }
}

/** Per-deck note + CC ids (must match docs/03-flx4-layout.md and the bridge). */
data class DeckIds(
    val play: Int,
    val cue: Int,
    val sync: Int,
    val loopIn: Int,
    val loopOut: Int,
    val fourBeat: Int,
    val loopExit: Int,          // exit / reloop
    val shiftCue: Int,          // shift + cue = jump to track start
    val pads: List<Int>,        // HOT CUE bank
    val padFx: List<Int>,       // PAD FX bank
    val beatJump: List<Int>,    // BEAT JUMP bank
    val sampler: List<Int>,     // SAMPLER bank
    val modeHotcue: Int,
    val modePadfx: Int,
    val modeBeatjump: Int,
    val modeSampler: Int,
    val tempo: Int,
    val jogFwd: Int,    // jog forward pulse -> PitchBendUp
    val jogBack: Int,   // jog backward pulse -> PitchBendDown
    val hi: Int,
    val mid: Int,
    val low: Int,
    val fader: Int,
    val trim: Int,              // channel gain (centered CC)
    val cfx: Int,               // color FX / filter (centered CC)
    val phoneCue: Int,          // headphone (pre-fader) cue (note)
)

val DeckA = DeckIds(
    play = 36, cue = 37, sync = 38, loopIn = 39, loopOut = 40, fourBeat = 41,
    loopExit = 42, shiftCue = 32,
    pads = (44..51).toList(), padFx = (0..7).toList(), beatJump = (8..15).toList(), sampler = (16..23).toList(),
    modeHotcue = 24, modePadfx = 25, modeBeatjump = 26, modeSampler = 27,
    tempo = 0, jogFwd = 33, jogBack = 35, hi = 22, mid = 24, low = 26, fader = 30,
    trim = 20, cfx = 28, phoneCue = 111,
)

val DeckB = DeckIds(
    play = 68, cue = 69, sync = 70, loopIn = 71, loopOut = 72, fourBeat = 73,
    loopExit = 74, shiftCue = 34,
    pads = (76..83).toList(), padFx = (52..59).toList(), beatJump = (84..91).toList(), sampler = (92..99).toList(),
    modeHotcue = 28, modePadfx = 29, modeBeatjump = 30, modeSampler = 31,
    tempo = 1, jogFwd = 60, jogBack = 61, hi = 23, mid = 25, low = 27, fader = 31,
    trim = 21, cfx = 29, phoneCue = 112,
)

/** Center / mixer ids. */
object Center {
    const val LOAD_A = 100
    const val LOAD_B = 101
    const val BROWSE_UP = 103
    const val BROWSE_DOWN = 104
    const val BROWSE_CC = 33  // relative rotary for track-list navigation
    const val MASTER_CUE = 102
    const val XFADER = 32
    const val FX_ONOFF = 105
    const val FX_PARAM_DN = 108   // beat-FX parameter step down (◄)
    const val FX_PARAM_UP = 113   // beat-FX parameter step up (►)
    const val FX_LEVEL = 37
    const val FX_ASSIGN1 = 109
    const val FX_ASSIGN2 = 110
    const val BEAT_L = 106
    const val BEAT_R = 107

    // macOS virtual keycodes for browse (synthesized as keystrokes by the bridge)
    const val KEY_ARROW_UP = 126
    const val KEY_ARROW_DOWN = 125
}
