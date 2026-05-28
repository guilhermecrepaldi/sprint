param(
    [switch]$NoBackend,
    [switch]$NoScreenshot
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Backend = Join-Path $Root "backend"
$Adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$Emulator = Join-Path $env:LOCALAPPDATA "Android\Sdk\emulator\emulator.exe"
$JavaHome = "C:\Program Files\Android\Android Studio\jbr"

function Test-Http($Url) {
    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Wait-ForBoot {
    & $Adb wait-for-device | Out-Null
    for ($i = 0; $i -lt 90; $i++) {
        $booted = (& $Adb shell getprop sys.boot_completed 2>$null).Trim()
        if ($booted -eq "1") { return }
        Start-Sleep -Seconds 2
    }
    throw "Emulator did not finish booting."
}

Set-Location $Root

if (!(Test-Path $Adb)) {
    throw "adb not found at $Adb. Check Android SDK installation."
}

if (!(Test-Path $JavaHome)) {
    throw "Android Studio JBR not found at $JavaHome."
}
$env:JAVA_HOME = $JavaHome

if (!$NoBackend) {
    Push-Location $Backend
    try {
        python -m alembic upgrade head
    } finally {
        Pop-Location
    }
}

$devices = (& $Adb devices | Select-String -Pattern "`tdevice").Count
if ($devices -eq 0) {
    if (!(Test-Path $Emulator)) {
        throw "No Android device connected and emulator.exe was not found."
    }
    $avd = (& $Emulator -list-avds | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($avd)) {
        throw "No AVD found. Create a tablet emulator in Android Studio Device Manager."
    }
    Start-Process -FilePath $Emulator -ArgumentList @("-avd", $avd) -WindowStyle Hidden
    Wait-ForBoot
}

if (!$NoBackend -and !(Test-Http "http://127.0.0.1:8000/")) {
    Start-Process -FilePath "powershell" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "Set-Location '$Backend'; python -m uvicorn main:app --host 0.0.0.0 --port 8000") `
        -WindowStyle Hidden

    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Http "http://127.0.0.1:8000/") { break }
        Start-Sleep -Seconds 1
    }
    if (!(Test-Http "http://127.0.0.1:8000/")) {
        throw "Backend did not start on http://127.0.0.1:8000."
    }
}

.\gradlew.bat :app:assembleDebug :app:installDebug

& $Adb logcat -c
& $Adb shell am force-stop com.strava_matematica | Out-Null
& $Adb shell am start -n com.strava_matematica/.MainActivity | Out-Null
Start-Sleep -Seconds 5

$fatal = & $Adb logcat -d -t 600 | Select-String -Pattern "FATAL EXCEPTION|Unable to start activity|ANR"
if ($fatal) {
    $fatal | ForEach-Object { $_.Line }
    throw "App launched with fatal Android runtime errors."
}

if (!$NoScreenshot) {
    $shotDir = Join-Path $Root ".sprint"
    New-Item -ItemType Directory -Force -Path $shotDir | Out-Null
    $shot = Join-Path $shotDir "android_screenshot.png"
    $remoteShot = "/sdcard/sprint_screenshot.png"
    & $Adb shell screencap -p $remoteShot | Out-Null
    & $Adb pull $remoteShot $shot | Out-Null
    & $Adb shell rm $remoteShot | Out-Null
    Write-Host "Screenshot: $shot"
}

Write-Host "OK: Sprint installed and launched on Android emulator/device."
