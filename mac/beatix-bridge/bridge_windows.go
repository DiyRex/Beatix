//go:build windows

package main

import (
	"fmt"
	"strings"
	"syscall"
	"unsafe"
)

var (
	winmm                 = syscall.NewLazyDLL("winmm.dll")
	procMidiOutGetNumDevs = winmm.NewProc("midiOutGetNumDevs")
	procMidiOutGetDevCaps = winmm.NewProc("midiOutGetDevCapsW")
	procMidiOutOpen       = winmm.NewProc("midiOutOpen")
	procMidiOutShortMsg   = winmm.NewProc("midiOutShortMsg")

	user32         = syscall.NewLazyDLL("user32.dll")
	procKeybdEvent = user32.NewProc("keybd_event")
)

type midiOutCaps struct {
	wMid           uint16
	wPid           uint16
	vDriverVersion uint32
	szPname        [32]uint16
	wTechnology    uint16
	wVoices        uint16
	wNotes         uint16
	wChannelMask   uint16
	dwSupport      uint32
}

var hMidiOut uintptr

// midiInit opens the loopMIDI output port named "Beatix" (Windows has no virtual
// MIDI API, so the user creates that port in loopMIDI).
func midiInit() error {
	n, _, _ := procMidiOutGetNumDevs.Call()
	for id := uintptr(0); id < n; id++ {
		var caps midiOutCaps
		r, _, _ := procMidiOutGetDevCaps.Call(id, uintptr(unsafe.Pointer(&caps)), unsafe.Sizeof(caps))
		if r != 0 {
			continue
		}
		if strings.Contains(syscall.UTF16ToString(caps.szPname[:]), "Beatix") {
			if r2, _, _ := procMidiOutOpen.Call(uintptr(unsafe.Pointer(&hMidiOut)), id, 0, 0, 0); r2 != 0 {
				return fmt.Errorf("midiOutOpen failed (%d)", r2)
			}
			return nil
		}
	}
	return fmt.Errorf("no MIDI output named 'Beatix' — create a loopMIDI port called 'Beatix' and retry")
}

func midiSend(a, b, c byte) {
	if hMidiOut == 0 {
		return
	}
	msg := uint32(a) | uint32(b)<<8 | uint32(c)<<16
	procMidiOutShortMsg.Call(hMidiOut, uintptr(msg))
}

// macOS virtual keycodes (sent by the app) -> Windows virtual-key codes.
var winVK = map[int]uintptr{126: 0x26, 125: 0x28, 123: 0x25, 124: 0x27} // up,down,left,right

func keySynth(code int, shift bool) {
	vk, ok := winVK[code]
	if !ok {
		return
	}
	const keyUp = 0x0002
	const vkShift = 0x10
	if shift {
		procKeybdEvent.Call(vkShift, 0, 0, 0)
	}
	procKeybdEvent.Call(vk, 0, 0, 0)
	procKeybdEvent.Call(vk, 0, keyUp, 0)
	if shift {
		procKeybdEvent.Call(vkShift, 0, keyUp, 0)
	}
}
