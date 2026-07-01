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
	"strconv"
	"strings"
	"sync"
	"time"

	"fyne.io/systray"
)

// On macOS the bridge injects keystrokes (CGEvent), which needs an Accessibility
// grant. macOS ties that grant to the process's *responsible* process — so a
// bridge spawned as a child of the tray inherits the TRAY's identity and the
// grant never applies. Running the bridge as its own launchd agent makes it its
// own responsible process, so the (persistent, cert-signed) grant sticks across
// restarts and rebuilds. Windows has no such permission, so it keeps the simple
// direct-spawn model below.
const label = "com.beatix.bridge"

func svcTarget() string { return "gui/" + strconv.Itoa(os.Getuid()) + "/" + label }

func plistPath() string {
	home, _ := os.UserHomeDir()
	return filepath.Join(home, "Library", "LaunchAgents", label+".plist")
}

// installAgent writes/refreshes the LaunchAgent and loads it (idempotent — a
// no-op if already loaded/running, so it never disturbs a live bridge).
func installAgent() {
	p := plistPath()
	content := `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>` + label + `</string>
  <key>ProgramArguments</key>
  <array><string>` + bridgePath() + `</string><string>-v</string></array>
  <key>KeepAlive</key><true/>
  <key>RunAtLoad</key><true/>
  <key>StandardOutPath</key><string>/tmp/beatix-bridge.log</string>
  <key>StandardErrorPath</key><string>/tmp/beatix-bridge.log</string>
</dict>
</plist>
`
	_ = os.MkdirAll(filepath.Dir(p), 0755)
	_ = os.WriteFile(p, []byte(content), 0644)
	_ = exec.Command("launchctl", "bootstrap", "gui/"+strconv.Itoa(os.Getuid()), p).Run() // errors if already loaded — fine
}

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

// startBridge ensures the bridge is running. macOS: load + start the launchd
// agent (won't restart a live one). Other OSes: spawn as a child.
func startBridge() {
	if runtime.GOOS == "darwin" {
		installAgent()
		_ = exec.Command("launchctl", "kickstart", svcTarget()).Run()
		return
	}
	spawnBridge()
}

func restartBridge() {
	if runtime.GOOS == "darwin" {
		installAgent()
		_ = exec.Command("launchctl", "kickstart", "-k", svcTarget()).Run()
		return
	}
	stopBridge()
	time.Sleep(300 * time.Millisecond)
	spawnBridge()
}

func spawnBridge() {
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
	if runtime.GOOS == "darwin" {
		_ = exec.Command("launchctl", "bootout", svcTarget()).Run() // unload so KeepAlive won't respawn
		return
	}
	mu.Lock()
	defer mu.Unlock()
	wantRunning = false
	if cmd != nil && cmd.Process != nil {
		_ = cmd.Process.Kill()
	}
	cmd = nil
}

func bridgeUp() bool {
	if runtime.GOOS == "darwin" {
		out, _ := exec.Command("pgrep", "-f", "beatix-bridge -v").Output()
		return len(strings.TrimSpace(string(out))) > 0
	}
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
				restartBridge()
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
			if runtime.GOOS != "darwin" { // macOS: launchd KeepAlive respawns for us
				mu.Lock()
				want := wantRunning
				alive := cmd != nil
				mu.Unlock()
				if want && !alive {
					startBridge()
				}
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

func onExit() {
	// On macOS the bridge lives independently under launchd — quitting the tray
	// must NOT kill it (that's the whole point). Use "Stop bridge" to stop it.
	if runtime.GOOS != "darwin" {
		stopBridge()
	}
}

func main() { systray.Run(onReady, onExit) }
