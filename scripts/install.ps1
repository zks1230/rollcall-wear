# 一条命令：编译 + 装到手表 + 启动
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$devices = adb devices | Select-String "device$"
if (-not $devices) {
    Write-Host "没有检测到手表。请先：手表打开 USB 调试 -> 数据线连电脑 -> 表上点「允许」。" -ForegroundColor Yellow
    Write-Host "adb devices 现在输出：" -ForegroundColor Yellow
    adb devices
    exit 1
}

Write-Host "编译中..." -ForegroundColor Cyan
.\gradlew.bat :app:assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw "编译失败" }

$apk = "app\build\outputs\apk\debug\app-debug.apk"
Write-Host "安装 $apk" -ForegroundColor Cyan
adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw "安装失败（签名冲突就先 adb uninstall com.juhao.rollcall）" }

Write-Host "启动..." -ForegroundColor Cyan
adb shell am start -n com.juhao.rollcall/.MainActivity
Write-Host "完成。看日志：adb logcat -s AndroidRuntime:E" -ForegroundColor Green
