#!/usr/bin/env python3
"""
Beatix cross-platform bridge (macOS + Windows + Linux).

Receives Beatix app messages over TCP (forwarded by `adb reverse`) and emits MIDI
on a port named "Beatix", plus synthesizes keystrokes for browse.

MIDI port:
  - macOS / Linux: creates a *virtual* port "Beatix" (nothing else needed).
  - Windows: Windows has no virtual-MIDI API, so install **loopMIDI** and create a
    port named "Beatix"; this bridge opens it.

Deps:  pip install mido python-rtmidi pynput

Protocol (one per line):  N <note> <0|1> | C <cc> <0-127> | J <cc> <delta> | K <keycode> <shift> | P
"""
import platform
import socket
import sys

import mido
from pynput.keyboard import Controller, Key

PORT_NAME = "Beatix"
ADDR = ("127.0.0.1", 5557)
IS_WIN = platform.system() == "Windows"


def open_midi():
    if IS_WIN:
        name = next((n for n in mido.get_output_names() if PORT_NAME in n), None)
        if not name:
            print("ERROR: no 'Beatix' MIDI port found.")
            print("Open loopMIDI and create a port named exactly 'Beatix', then re-run.")
            sys.exit(1)
        print(f"[Beatix] opened MIDI port: {name}")
        return mido.open_output(name)
    out = mido.open_output(PORT_NAME, virtual=True)
    print(f"[Beatix] virtual MIDI port '{PORT_NAME}' is live")
    return out


kb = Controller()
# our app sends macOS virtual keycodes; map to pynput keys (works on all platforms)
ARROWS = {126: Key.up, 125: Key.down, 123: Key.left, 124: Key.right}


def handle(line, out):
    f = line.split()
    if not f:
        return
    cmd = f[0]
    try:
        if cmd == "N":
            note, on = int(f[1]), int(f[2])
            out.send(mido.Message("note_on", channel=0, note=note & 127, velocity=127 if on else 0))
        elif cmd == "C":
            cc, v = int(f[1]), int(f[2])
            out.send(mido.Message("control_change", channel=0, control=cc & 127, value=max(0, min(127, v))))
        elif cmd == "J":
            cc, d = int(f[1]), int(f[2])
            out.send(mido.Message("control_change", channel=0, control=cc & 127, value=1 if d > 0 else 127))
        elif cmd == "K":
            code, shift = int(f[1]), int(f[2])
            key = ARROWS.get(code)
            if key:
                if shift:
                    kb.press(Key.shift)
                kb.press(key)
                kb.release(key)
                if shift:
                    kb.release(Key.shift)
        # "P" heartbeat -> ignore
    except Exception:
        pass


def main():
    out = open_midi()
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(ADDR)
    srv.listen(1)
    print(f"[Beatix] listening on {ADDR[0]}:{ADDR[1]}  (run: adb reverse tcp:5557 tcp:5557)")
    print("[Beatix] open your DJ app AFTER this, then map the 'Beatix' device. Ctrl+C to quit.")
    while True:
        conn, _ = srv.accept()
        print("[Beatix] app connected")
        buf = b""
        with conn:
            while True:
                data = conn.recv(4096)
                if not data:
                    break
                buf += data
                while b"\n" in buf:
                    line, buf = buf.split(b"\n", 1)
                    handle(line.decode("ascii", "ignore").strip(), out)
        print("[Beatix] app disconnected")


if __name__ == "__main__":
    main()
