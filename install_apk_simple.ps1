#!/usr/bin/env pwsh
# Script simple d'installation APK STV

$ADB = "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$APK = "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

Write-Host "====== INSTALLEUR APK STV ======`n" -ForegroundColor Cyan

# Vérifications préalables
if (-not (Test-Path $ADB)) {
    Write-Host "ERREUR: ADB non trouvé`n" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $APK)) {
    Write-Host "ERREUR: APK non trouvé`n" -ForegroundColor Red
    exit 1
}

Write-Host "ADB: OK`n" -ForegroundColor Green
Write-Host "APK: OK`n" -ForegroundColor Green

# Démarrer ADB
Write-Host "Demarrage ADB...`n" -ForegroundColor Blue
& $ADB start-server 2>&1 | Out-Null
Start-Sleep -Seconds 2

# Lister les appareils
Write-Host "Appareils connectes:`n" -ForegroundColor Blue
$devices = & $ADB devices 2>&1
$devices

# Installation
Write-Host "`nInstallation de l'APK...`n" -ForegroundColor Blue
& $ADB install -r $APK 2>&1

Write-Host "`nFait!`n" -ForegroundColor Green

