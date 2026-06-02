param(
    [string]$DeviceId = "R3CN5030RFM",
    [int]$Cycles = 6,
    [switch]$ShowBars
)

$ErrorActionPreference = "Stop"

$packageName = "com.soundvisualization.accessibility"
$activityName = "$packageName/.MainActivity"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$projectRoot = Split-Path -Parent $PSScriptRoot
$reportPath = Join-Path $projectRoot "framestats.txt"

if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb.exe was not found at $adb"
}

if ($ShowBars) {
    & $adb -s $DeviceId shell setprop debug.hwui.profile visual_bars
} else {
    & $adb -s $DeviceId shell setprop debug.hwui.profile false
}

& $adb -s $DeviceId shell pm grant $packageName android.permission.RECORD_AUDIO 2>$null
& $adb -s $DeviceId shell pm grant $packageName android.permission.POST_NOTIFICATIONS 2>$null
& $adb -s $DeviceId shell am force-stop $packageName
& $adb -s $DeviceId shell am start -n $activityName | Out-Null
Start-Sleep -Seconds 8

$sizeLines = & $adb -s $DeviceId shell wm size
$overrideLine = $sizeLines | Select-String -Pattern "Override size:\s*(\d+x\d+)" | Select-Object -First 1
$physicalLine = $sizeLines | Select-String -Pattern "Physical size:\s*(\d+x\d+)" | Select-Object -First 1

if ($overrideLine) {
    $sizeText = $overrideLine.Matches[0].Groups[1].Value
} elseif ($physicalLine) {
    $sizeText = $physicalLine.Matches[0].Groups[1].Value
} else {
    $sizeText = $null
}

if (-not $sizeText) {
    throw "Could not read device size."
}

$parts = $sizeText.Split("x")
$width = [int]$parts[0]
$height = [int]$parts[1]

function TapRatio([double]$xRatio, [double]$yRatio) {
    $x = [int]($script:width * $xRatio)
    $y = [int]($script:height * $yRatio)
    & $script:adb -s $script:DeviceId shell input tap $x $y
}

& $adb -s $DeviceId shell dumpsys gfxinfo $packageName reset | Out-Null
Start-Sleep -Milliseconds 500

for ($i = 0; $i -lt $Cycles; $i += 1) {
    TapRatio 0.50 0.94
    Start-Sleep -Milliseconds 950
    TapRatio 0.79 0.94
    Start-Sleep -Milliseconds 950
    TapRatio 0.18 0.94
    Start-Sleep -Milliseconds 950
}

$dump = & $adb -s $DeviceId shell dumpsys gfxinfo $packageName framestats
$dump | Set-Content -LiteralPath $reportPath -Encoding UTF8

$summaryPatterns = @(
    "Total frames rendered:",
    "Janky frames:",
    "50th percentile:",
    "90th percentile:",
    "95th percentile:",
    "99th percentile:",
    "Number Missed Vsync:",
    "Number High input latency:",
    "Number Slow UI thread:",
    "Number Slow bitmap uploads:",
    "Number Slow issue draw commands:"
)

Write-Host "GPU profiling report: $reportPath"
foreach ($pattern in $summaryPatterns) {
    $line = $dump | Select-String -Pattern ([regex]::Escape($pattern)) | Select-Object -First 1
    if ($line) {
        Write-Host $line.Line.Trim()
    }
}
