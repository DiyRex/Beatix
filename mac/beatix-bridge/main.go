// Beatix bridge — receives Beatix app messages over TCP (forwarded by `adb reverse`)
// and emits MIDI on a port named "Beatix", plus synthesizes browse keystrokes.
//
// Platform backends provide midiInit/midiSend/keySynth:
//   bridge_darwin.go   — CoreMIDI virtual source + CGEvent (macOS, cgo)
//   bridge_windows.go  — winmm (opens a loopMIDI "Beatix" port) + keybd_event (no cgo)
//
// Wire protocol (one message per line, ASCII):
//   N <note> <0|1>   button note off/on        C <cc> <0..127>  absolute CC
//   J <cc> <delta>   relative CC (1=+,127=-)    K <keycode> <shift>  keystroke
//   P                heartbeat (ignored)
// MIDI is sent on channel 1.
package main

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
	statusNoteOn = 0x90
	statusCC     = 0xB0
	velOn        = 127
	relUp        = 1
	relDown      = 127
)

func clamp7(v int) byte {
	if v < 0 {
		return 0
	}
	if v > 127 {
		return 127
	}
	return byte(v)
}

func handleLine(line string, verbose bool) {
	f := strings.Fields(line)
	if len(f) == 0 {
		return
	}
	switch f[0] {
	case "P":
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
		midiSend(statusNoteOn, clamp7(note), vel)
		if verbose {
			log.Printf("note %d on=%d", note, on)
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
		midiSend(statusCC, clamp7(cc), clamp7(val))
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
		midiSend(statusCC, clamp7(cc), v)
		if verbose {
			log.Printf("jog cc %d delta %d", cc, delta)
		}
	case "K":
		if len(f) != 3 {
			return
		}
		keycode, err1 := strconv.Atoi(f[1])
		shift, err2 := strconv.Atoi(f[2])
		if err1 != nil || err2 != nil {
			return
		}
		keySynth(keycode, shift != 0)
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

	if err := midiInit(); err != nil {
		log.Fatalf("MIDI init failed: %v", err)
	}
	log.Printf("MIDI port 'Beatix' is ready")

	if *selftest {
		midiSend(statusNoteOn, 36, velOn)
		midiSend(statusNoteOn, 36, 0)
		fmt.Println("SELFTEST OK")
		return
	}

	ln, err := net.Listen("tcp", *addr)
	if err != nil {
		log.Fatalf("listen %s: %v", *addr, err)
	}
	log.Printf("listening on %s  (run: adb reverse tcp:%s tcp:%s)", *addr, portOf(*addr), portOf(*addr))
	log.Printf("open your DJ app AFTER this, then map/learn the 'Beatix' device")

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
