// Beatix tray — a menu-bar controller that runs and monitors the MIDI bridge,
// auto-reapplies the USB socket forward (adb reverse), and shows live status.
// Cross-platform via fyne.io/systray (macOS/Windows/Linux).
package main

import (
	"bytes"
	"image"
	"image/color"
	"image/png"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"fyne.io/systray"
)

const port = "5557"

var (
	mu          sync.Mutex
	cmd         *exec.Cmd
	wantRunning bool
)

func bridgePath() string {
	if v := os.Getenv("BEATIX_BRIDGE"); v != "" {
		return v
	}
	name := "beatix-bridge"
	if runtime.GOOS == "windows" {
		name += ".exe"
	}
	// look next to this executable first
	if exe, err := os.Executable(); err == nil {
		cand := filepath.Join(filepath.Dir(exe), name)
		if _, err := os.Stat(cand); err == nil {
			return cand
		}
		cand = filepath.Join(filepath.Dir(exe), "beatix-bridge", name)
		if _, err := os.Stat(cand); err == nil {
			return cand
		}
	}
	return "/Users/devin/Projects/Beatix/mac/beatix-bridge/beatix-bridge"
}

func adbBin() string {
	if p, err := exec.LookPath("adb"); err == nil {
		return p
	}
	return "/opt/homebrew/bin/adb"
}

func startBridge() {
	mu.Lock()
	defer mu.Unlock()
	wantRunning = true
	if cmd != nil {
		return
	}
	c := exec.Command(bridgePath(), "-v")
	if f, err := os.Create("/tmp/beatix-bridge.log"); err == nil {
		c.Stdout, c.Stderr = f, f
	}
	if err := c.Start(); err != nil {
		return
	}
	cmd = c
	go func() {
		c.Wait()
		mu.Lock()
		if cmd == c {
			cmd = nil
		}
		mu.Unlock()
	}()
}

func stopBridge() {
	mu.Lock()
	defer mu.Unlock()
	wantRunning = false
	if cmd != nil && cmd.Process != nil {
		_ = cmd.Process.Kill()
	}
	cmd = nil
}

func bridgeUp() bool {
	mu.Lock()
	defer mu.Unlock()
	return cmd != nil
}

func phoneUp() bool {
	out, _ := exec.Command(adbBin(), "get-state").Output()
	return strings.TrimSpace(string(out)) == "device"
}

func reverse() {
	_ = exec.Command(adbBin(), "reverse", "tcp:"+port, "tcp:"+port).Run()
}

func iconPNG() []byte {
	const s = 32
	img := image.NewRGBA(image.Rect(0, 0, s, s))
	amber := color.RGBA{0xF5, 0xA6, 0x23, 0xFF}
	hub := color.RGBA{0x10, 0x10, 0x12, 0xFF}
	cx, cy := 15.5, 15.5
	for y := 0; y < s; y++ {
		for x := 0; x < s; x++ {
			dx, dy := float64(x)-cx, float64(y)-cy
			d := dx*dx + dy*dy
			switch {
			case d <= 13.5*13.5 && d >= 9.5*9.5:
				img.SetRGBA(x, y, amber) // outer ring
			case d <= 4.5*4.5:
				img.SetRGBA(x, y, amber) // center dot
			case d <= 13.5*13.5:
				img.SetRGBA(x, y, hub)
			}
		}
	}
	var b bytes.Buffer
	_ = png.Encode(&b, img)
	return b.Bytes()
}

// appConn reports whether the phone app has an established socket to the bridge,
// and which method it's using (inferred from the peer address).
func appConn() (bool, string) {
	var out []byte
	if runtime.GOOS == "windows" {
		out, _ = exec.Command("netstat", "-n", "-p", "tcp").Output()
	} else {
		out, _ = exec.Command("lsof", "-nP", "-iTCP:"+port, "-sTCP:ESTABLISHED").Output()
	}
	for _, line := range strings.Split(string(out), "\n") {
		if runtime.GOOS == "windows" {
			if !strings.Contains(line, ":"+port) || !strings.Contains(line, "ESTABLISHED") {
				continue
			}
			f := strings.Fields(line)
			if len(f) >= 3 {
				return true, method(f[2])
			}
		} else {
			i := strings.Index(line, "->")
			if i < 0 {
				continue
			}
			peer := strings.TrimSpace(line[i+2:])
			if j := strings.Index(peer, " "); j >= 0 {
				peer = peer[:j]
			}
			return true, method(peer)
		}
	}
	return false, ""
}

func method(peer string) string {
	if strings.Contains(peer, "127.0.0.1") || strings.Contains(peer, "::1") {
		return "ADB/USB"
	}
	return "Wi-Fi"
}

func onReady() {
	systray.SetIcon(iconPNG())
	systray.SetTooltip("Beatix DJ bridge")

	mStatus := systray.AddMenuItem("starting…", "")
	mStatus.Disable()
	systray.AddSeparator()
	mReconnect := systray.AddMenuItem("Reconnect USB", "Re-apply adb reverse")
	mRestart := systray.AddMenuItem("Restart bridge", "")
	mToggle := systray.AddMenuItem("Stop bridge", "")
	systray.AddSeparator()
	mQuit := systray.AddMenuItem("Quit Beatix", "")

	startBridge()
	reverse()

	go func() {
		for {
			select {
			case <-mReconnect.ClickedCh:
				reverse()
			case <-mRestart.ClickedCh:
				stopBridge()
				time.Sleep(300 * time.Millisecond)
				startBridge()
				reverse()
				mToggle.SetTitle("Stop bridge")
			case <-mToggle.ClickedCh:
				if bridgeUp() {
					stopBridge()
					mToggle.SetTitle("Start bridge")
				} else {
					startBridge()
					reverse()
					mToggle.SetTitle("Stop bridge")
				}
			case <-mQuit.ClickedCh:
				systray.Quit()
			}
		}
	}()

	go func() {
		t := time.NewTicker(2 * time.Second)
		defer t.Stop()
		for range t.C {
			mu.Lock()
			want := wantRunning
			alive := cmd != nil
			mu.Unlock()
			if want && !alive { // auto-respawn if it died unexpectedly
				startBridge()
			}
			if phoneUp() {
				reverse() // ADB mode: auto-heal the USB forward
			}
			b := "stopped"
			if bridgeUp() {
				b = "running"
			}
			connected, via := appConn()
			st := "waiting for app…"
			if connected {
				st = "app connected · " + via
			}
			mStatus.SetTitle("Bridge " + b + "  ·  " + st)
		}
	}()
}

func onExit() { stopBridge() }

func main() { systray.Run(onReady, onExit) }
