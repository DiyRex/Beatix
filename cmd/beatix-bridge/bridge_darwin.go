//go:build darwin

package main

/*
#cgo LDFLAGS: -framework CoreMIDI -framework CoreFoundation -framework CoreGraphics
#include <CoreMIDI/CoreMIDI.h>
#include <CoreGraphics/CoreGraphics.h>

static MIDIClientRef   gClient;
static MIDIEndpointRef gSource;

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

import "fmt"

func midiInit() error {
	if rc := int(C.beatix_midi_init()); rc != 0 {
		return fmt.Errorf("CoreMIDI init OSStatus %d", rc)
	}
	return nil
}

func midiSend(a, b, c byte) {
	C.beatix_midi_send(C.uchar(a), C.uchar(b), C.uchar(c))
}

func keySynth(code int, shift bool) {
	s := C.int(0)
	if shift {
		s = 1
	}
	C.beatix_key(C.int(code), s)
}
