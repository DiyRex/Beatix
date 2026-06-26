// Beatix bridge: receives compact control messages from the Beatix Android app
// over a TCP socket (forwarded via `adb reverse`) and emits them as MIDI on a
// virtual CoreMIDI source named "Beatix". Rekordbox sees "Beatix" as a MIDI
// controller; you MIDI-LEARN each control once.
//
// Wire protocol (one message per line, ASCII):
//
//	N <note> <0|1>     button: note on (vel 127) / note off       e.g. "N 36 1"
//	C <cc> <0..127>    slider: absolute control change            e.g. "C 22 96"
//	J <cc> <delta>     jog/encoder: relative CC (1=+, 127=-)      e.g. "J 16 -3"
//	P                  heartbeat (ignored)
//
// All MIDI is sent on channel 1.
package main

/*
#cgo LDFLAGS: -framework CoreMIDI -framework CoreFoundation -framework CoreGraphics
#include <CoreMIDI/CoreMIDI.h>
#include <CoreGraphics/CoreGraphics.h>

static MIDIClientRef   gClient;
static MIDIEndpointRef gSource;

// Synthesize a keystroke to the focused app (e.g. Rekordbox track-list nav).
// Requires the bridge binary to have Accessibility permission.
void beatix_key(int keycode, int shift) {
    CGEventSourceRef src = CGEventSourceCreate(kCGEventSourceStateHIDSystemState);
    CGEventRef down = CGEventCreateKeyboardEvent(src, (CGKeyCode)keycode, true);
    CGEventRef up   = CGEventCreateKeyboardEvent(src, (CGKeyCode)keycode, false);
    if (shift) {
        CGEventSetFlags(down, kCGEventFlagMaskShift);
        CGEventSetFlags(up, kCGEventFlagMaskShift);
    }
    CGEventPost(kCGHIDEventTap, down);
    CGEventPost(kCGHIDEventTap, up);
    CFRelease(down);
    CFRelease(up);
    CFRelease(src);
}

// Returns 0 on success, otherwise the OSStatus error code.
int beatix_midi_init() {
    CFStringRef name = CFStringCreateWithCString(NULL, "Beatix", kCFStringEncodingUTF8);
    OSStatus s = MIDIClientCreate(name, NULL, NULL, &gClient);
    if (s != noErr) { CFRelease(name); return (int)s; }
    s = MIDISourceCreate(gClient, name, &gSource);
    CFRelease(name);
    return (int)s;
}

void beatix_midi_send(unsigned char a, unsigned char b, unsigned char c) {
    Byte buffer[64];
    MIDIPacketList *pktlist = (MIDIPacketList *)buffer;
    MIDIPacket *cur = MIDIPacketListInit(pktlist);
    Byte data[3] = { a, b, c };
    cur = MIDIPacketListAdd(pktlist, sizeof(buffer), cur, (MIDITimeStamp)0, 3, data);
    if (cur != NULL) {
        MIDIReceived(gSource, pktlist);
    }
}
*/
import "C"

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
)

const (
	statusNoteOn = 0x90 // channel 1
	statusCC     = 0xB0 // channel 1
	velOn        = 127
	relUp        = 1
	relDown      = 127
)

func sendMIDI(a, b, c byte) {
	C.beatix_midi_send(C.uchar(a), C.uchar(b), C.uchar(c))
}

func clamp7(v int) byte {
	if v < 0 {
		return 0
	}
	if v > 127 {
		return 127
	}
	return byte(v)
}

// handleLine parses one protocol line and emits MIDI. verbose logs each message.
func handleLine(line string, verbose bool) {
	f := strings.Fields(line)
	if len(f) == 0 {
		return
	}
	switch f[0] {
	case "P": // heartbeat
		return
	case "N":
		if len(f) != 3 {
			return
		}
		note, err1 := strconv.Atoi(f[1])
		on, err2 := strconv.Atoi(f[2])
		if err1 != nil || err2 != nil {
			return
		}
		vel := byte(0)
		if on != 0 {
			vel = velOn
		}
		sendMIDI(statusNoteOn, clamp7(note), vel)
		if verbose {
			log.Printf("note %d %s", note, map[bool]string{true: "on", false: "off"}[on != 0])
		}
	case "C":
		if len(f) != 3 {
			return
		}
		cc, err1 := strconv.Atoi(f[1])
		val, err2 := strconv.Atoi(f[2])
		if err1 != nil || err2 != nil {
			return
		}
		sendMIDI(statusCC, clamp7(cc), clamp7(val))
		if verbose {
			log.Printf("cc %d = %d", cc, val)
		}
	case "J":
		if len(f) != 3 {
			return
		}
		cc, err1 := strconv.Atoi(f[1])
		delta, err2 := strconv.Atoi(f[2])
		if err1 != nil || err2 != nil {
			return
		}
		v := byte(relUp)
		if delta < 0 {
			v = relDown
		}
		sendMIDI(statusCC, clamp7(cc), v)
		if verbose {
			log.Printf("jog cc %d delta %d", cc, delta)
		}
	case "K": // synthesize a keystroke (e.g. browse arrows): "K <keycode> <shift>"
		if len(f) != 3 {
			return
		}
		keycode, err1 := strconv.Atoi(f[1])
		shift, err2 := strconv.Atoi(f[2])
		if err1 != nil || err2 != nil {
			return
		}
		C.beatix_key(C.int(keycode), C.int(shift))
		if verbose {
			log.Printf("key %d shift %d", keycode, shift)
		}
	}
}

func handleConn(conn net.Conn, verbose bool) {
	defer conn.Close()
	log.Printf("client connected: %s", conn.RemoteAddr())
	sc := bufio.NewScanner(conn)
	sc.Buffer(make([]byte, 0, 4096), 65536)
	for sc.Scan() {
		handleLine(strings.TrimSpace(sc.Text()), verbose)
	}
	log.Printf("client disconnected")
}

func main() {
	addr := flag.String("addr", "127.0.0.1:5557", "TCP listen address")
	verbose := flag.Bool("v", false, "log every MIDI message")
	selftest := flag.Bool("selftest", false, "init MIDI, send a test note, exit")
	flag.Parse()

	if rc := int(C.beatix_midi_init()); rc != 0 {
		log.Fatalf("CoreMIDI init failed (OSStatus %d)", rc)
	}
	log.Printf("virtual MIDI source 'Beatix' is live")

	if *selftest {
		sendMIDI(statusNoteOn, 36, velOn)
		sendMIDI(statusNoteOn, 36, 0)
		fmt.Println("SELFTEST OK")
		return
	}

	ln, err := net.Listen("tcp", *addr)
	if err != nil {
		log.Fatalf("listen %s: %v", *addr, err)
	}
	log.Printf("listening on %s  (run: adb reverse tcp:%s tcp:%s)", *addr,
		portOf(*addr), portOf(*addr))
	log.Printf("open Rekordbox AFTER this, then MIDI-LEARN the 'Beatix' device")

	go func() {
		for {
			conn, err := ln.Accept()
			if err != nil {
				log.Printf("accept: %v", err)
				return
			}
			go handleConn(conn, *verbose)
		}
	}()

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt, syscall.SIGTERM)
	<-sig
	log.Printf("shutting down")
	_ = ln.Close()
}

func portOf(addr string) string {
	if i := strings.LastIndex(addr, ":"); i >= 0 {
		return addr[i+1:]
	}
	return addr
}
