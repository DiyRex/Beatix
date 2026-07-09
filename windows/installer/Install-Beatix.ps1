<#
  Install-Beatix.ps1  —  one-shot Windows setup for the Beatix bridge.

  Run it from the "Beatix-Windows-Kit" folder (right-click > Run with PowerShell,
  or:  powershell -ExecutionPolicy Bypass -File Install-Beatix.ps1).

  What it does:
    1. Installs the Beatix bridge + tray to %LOCALAPPDATA%\Beatix
    2. Downloads Android platform-tools (adb) into that folder  (no PATH edits)
    3. Downloads + silently installs loopMIDI (the virtual-MIDI driver Windows lacks)
    4. Creates a Start-Beatix launcher + auto-start entry (tray at logon)
    5. Opens loopMIDI for the ONE manual step: create a port named  Beatix

  NOTE: not yet tested on a live Windows box — best-effort. If a download URL is
  dead it will tell you and open the vendor page.
#>

$ErrorActionPreference = 'Stop'
$ProgressPreference     = 'SilentlyContinue'

# --- self-elevate (loopMIDI's driver install needs admin) ---
$id = [Security.Principal.WindowsIdentity]::GetCurrent()
$pr = New-Object Security.Principal.WindowsPrincipal($id)
if (-not $pr.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "Re-launching as administrator..." -ForegroundColor Yellow
    Start-Process powershell "-ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

$src  = $PSScriptRoot
$dest = Join-Path $env:LOCALAPPDATA 'Beatix'
$tools = Join-Path $dest 'platform-tools'
New-Object -ComObject Shell.Application | Out-Null

function Get-Src([string]$name) {
    foreach ($p in @((Join-Path $src "bridge\$name"), (Join-Path $src $name))) {
        if (Test-Path $p) { return $p }
    }
    throw "Missing $name — run this from the Beatix-Windows-Kit folder."
}

Write-Host "`n=== Beatix Windows installer ===`n" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $dest | Out-Null

# 0) stop any running Beatix so we can overwrite the old (crash-looping) exe.
#    Without this, Copy-Item fails because the .exe is locked/in-use.
Write-Host "[0/5] Stopping any running Beatix (clean replace)..."
Get-Process beatix-bridge,beatix-tray -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Milliseconds 600

# 1) bridge + tray + phone APK  (force-overwrite the previous install)
Write-Host "[1/5] Copying bridge + tray..."
Copy-Item (Get-Src 'beatix-bridge.exe') $dest -Force
Copy-Item (Get-Src 'beatix-tray.exe')   $dest -Force
$apk = Join-Path $src 'beatix.apk'
if (Test-Path $apk) { Copy-Item $apk $dest -Force }

# 2) adb (platform-tools)
if (-not (Test-Path (Join-Path $tools 'adb.exe'))) {
    Write-Host "[2/5] Downloading adb (platform-tools)..."
    $zip = Join-Path $env:TEMP 'platform-tools.zip'
    Invoke-WebRequest 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $zip
    Expand-Archive $zip $dest -Force        # yields $dest\platform-tools\adb.exe
    Remove-Item $zip -Force
} else { Write-Host "[2/5] adb already present — skipping." }

# 3) loopMIDI (virtual MIDI)
$loopExe = "C:\Program Files (x86)\Tobias Erichsen\loopMIDI\loopMIDI.exe"
if (-not (Test-Path $loopExe)) {
    Write-Host "[3/5] Installing loopMIDI (virtual MIDI driver)..."
    $loopUrl = 'https://www.tobias-erichsen.de/wp-content/uploads/2020/01/loopMIDISetup_1_0_16_27.zip'
    try {
        $lz = Join-Path $env:TEMP 'loopMIDI.zip'
        Invoke-WebRequest $loopUrl -OutFile $lz
        $lx = Join-Path $env:TEMP 'loopMIDI'
        Expand-Archive $lz $lx -Force
        $setup = Get-ChildItem $lx -Filter 'loopMIDISetup*.exe' | Select-Object -First 1
        Start-Process $setup.FullName '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART' -Wait
        Remove-Item $lz,$lx -Recurse -Force
    } catch {
        Write-Host "  Auto-download failed. Opening the loopMIDI page — install it, then re-run me." -ForegroundColor Yellow
        Start-Process 'https://www.tobias-erichsen.de/software/loopmidi.html'
        exit 1
    }
} else { Write-Host "[3/5] loopMIDI already installed — skipping." }

# 4) launcher + auto-start
Write-Host "[4/5] Creating launcher + auto-start..."
$cmd = @"
@echo off
rem Beatix session launcher
if exist "%LOCALAPPDATA%\Beatix\platform-tools\adb.exe" (
  "%LOCALAPPDATA%\Beatix\platform-tools\adb.exe" reverse tcp:5557 tcp:5557
)
start "" "%LOCALAPPDATA%\Beatix\beatix-tray.exe"
"@
$launcher = Join-Path $dest 'Start-Beatix.cmd'
Set-Content -Path $launcher -Value $cmd -Encoding ASCII

$startup = [Environment]::GetFolderPath('Startup')
$wsh = New-Object -ComObject WScript.Shell
$lnk = $wsh.CreateShortcut((Join-Path $startup 'Beatix.lnk'))
$lnk.TargetPath = $launcher
$lnk.WorkingDirectory = $dest
$lnk.Save()

# 5) the one manual step — create the "Beatix" loopMIDI port
Write-Host "[5/5] Opening loopMIDI + starting the new bridge..."
if (Test-Path $loopExe) { Start-Process $loopExe }
Start-Process $launcher   # start the fresh (non-crashing) bridge/tray now

Write-Host @"

=====================================================================
 DONE. One manual step left (once, ever):
   In the loopMIDI window that just opened, type   Beatix   in the
   'New port-name' box and click the [ + ] button. Tick 'Autostart'.

 Then every session is automatic:
   - loopMIDI + Beatix tray start at logon (adb reverse re-applied by the tray)
   - Plug the phone (USB debugging on), open rekordbox, open the Beatix app
   - In the app: Settings -> "ADB / USB cable"  (or enter this PC's Wi-Fi IP)

 Installed to: $dest
 Import the controller map in rekordbox: MIDI -> Beatix -> IMPORT -> Beatix.midi.csv
=====================================================================
"@ -ForegroundColor Green
