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
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Sends Beatix wire-protocol lines to the Mac bridge over TCP.
 * The phone reaches the Mac's bridge at 127.0.0.1:5557 via `adb reverse`.
 * Auto-reconnects; messages are queued so the UI never blocks.
 */
class MidiClient(
    private val host: String = "127.0.0.1",
    private val port: Int = 5557,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<String>(Channel.UNLIMITED)
    private var job: Job? = null

    @Volatile
    var connected: Boolean = false
        private set

    fun start() {
        job = scope.launch {
            while (isActive) {
                try {
                    Socket().use { sock ->
                        sock.tcpNoDelay = true
                        sock.connect(InetSocketAddress(host, port), 2000)
                        connected = true
                        val out = sock.getOutputStream()
                        val hb = launch {
                            while (isActive) {
                                queue.trySend("P\n")
                                delay(2000)
                            }
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
                connected = false
                delay(800)
            }
        }
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
    val shift: Int,             // Rekordbox Shift modifier (momentary)
    val pads: List<Int>,        // HOT CUE bank
    val padFx: List<Int>,       // PAD FX bank
    val beatJump: List<Int>,    // BEAT JUMP bank
    val sampler: List<Int>,     // SAMPLER bank
    val modeHotcue: Int,
    val modePadfx: Int,
    val modeBeatjump: Int,
    val modeSampler: Int,
    val tempo: Int,
    val jog: Int,
    val jogTouch: Int,
    val hi: Int,
    val mid: Int,
    val low: Int,
    val fader: Int,
)

val DeckA = DeckIds(
    play = 36, cue = 37, sync = 38, loopIn = 39, loopOut = 40, fourBeat = 41,
    shift = 32,
    pads = (44..51).toList(), padFx = (0..7).toList(), beatJump = (8..15).toList(), sampler = (16..23).toList(),
    modeHotcue = 24, modePadfx = 25, modeBeatjump = 26, modeSampler = 27,
    tempo = 0, jog = 16, jogTouch = 42, hi = 22, mid = 24, low = 26, fader = 30,
)

val DeckB = DeckIds(
    play = 68, cue = 69, sync = 70, loopIn = 71, loopOut = 72, fourBeat = 73,
    shift = 34,
    pads = (76..83).toList(), padFx = (52..59).toList(), beatJump = (84..91).toList(), sampler = (92..99).toList(),
    modeHotcue = 28, modePadfx = 29, modeBeatjump = 30, modeSampler = 31,
    tempo = 1, jog = 17, jogTouch = 74, hi = 23, mid = 25, low = 27, fader = 31,
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
    const val FX_SELECT = 108
    const val FX_LEVEL = 37
    const val BEAT_L = 106
    const val BEAT_R = 107

    // macOS virtual keycodes for browse (synthesized as keystrokes by the bridge)
    const val KEY_ARROW_UP = 126
    const val KEY_ARROW_DOWN = 125
}
