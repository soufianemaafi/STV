# Script de Test - Architecture Hybride STV
# Date: 26 Février 2026

param(
    [switch]$InstallOnly,
    [switch]$TestOnly
)

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "🧪 TEST ARCHITECTURE HYBRIDE - STV & SoukiTV" -ForegroundColor Green
Write-Host "================================================`n" -ForegroundColor Cyan

# Vérifier ADB
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "❌ ERREUR: ADB non trouvé!" -ForegroundColor Red
    exit 1
}

# Vérifier device connecté
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ ERREUR: Aucun appareil connecté!" -ForegroundColor Red
    Write-Host "   Connectez votre téléphone en USB et activez le débogage`n" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Device connecté`n" -ForegroundColor Green

# ===== INSTALLATION =====
if (-not $TestOnly) {
    Write-Host "📱 INSTALLATION DES APKs..." -ForegroundColor Cyan
    Write-Host "─────────────────────────────────────────────────`n" -ForegroundColor Gray

    # Désinstaller anciennes versions
    Write-Host "🧹 Nettoyage..." -ForegroundColor Yellow
    adb uninstall com.example.stv 2>&1 | Out-Null
    adb uninstall com.example.stv.dev 2>&1 | Out-Null
    adb uninstall com.example.stv.prod 2>&1 | Out-Null
    adb uninstall com.example.soukitv 2>&1 | Out-Null
    Write-Host "   ✅ Anciennes versions supprimées`n" -ForegroundColor Green

    # Installer STV
    Write-Host "📱 Installation STV Player..." -ForegroundColor Cyan
    $result = adb install -r "app\build\outputs\apk\prod\release\app-prod-release.apk" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ STV Player installé`n" -ForegroundColor Green
    } else {
        Write-Host "   ❌ ERREUR: $result`n" -ForegroundColor Red
        exit 1
    }

    # Installer SoukiTV
    Write-Host "📺 Installation SoukiTV..." -ForegroundColor Cyan
    $result = adb install -r "soukitv\build\outputs\apk\release\soukitv-release.apk" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ SoukiTV installé`n" -ForegroundColor Green
    } else {
        Write-Host "   ❌ ERREUR: $result`n" -ForegroundColor Red
        exit 1
    }
}

if ($InstallOnly) {
    Write-Host "✅ Installation terminée!`n" -ForegroundColor Green
    exit 0
}

# ===== TESTS =====
Write-Host "`n🧪 TESTS AUTOMATISÉS..." -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────`n" -ForegroundColor Gray

# Test 1 : Deep Link
Write-Host "Test 1️⃣ : Deep Link (stv://play?url=...)" -ForegroundColor Yellow
Write-Host "   Action: Lancement via Deep Link" -ForegroundColor Gray
$url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=$url" 2>&1 | Out-Null
Start-Sleep -Seconds 2

# Vérifier les logs
$logs = adb logcat -d -s PlayerActivity:D | Select-String "deep link" -SimpleMatch
if ($logs) {
    Write-Host "   ✅ STV lancé via Deep Link" -ForegroundColor Green
    Write-Host "   📝 Log: $($logs[0])`n" -ForegroundColor Gray
} else {
    Write-Host "   ⚠️  Pas de log Deep Link détecté`n" -ForegroundColor Yellow
}

Start-Sleep -Seconds 3

# Test 2 : ACTION_VIEW avec .m3u8
Write-Host "Test 2️⃣ : ACTION_VIEW (.m3u8)" -ForegroundColor Yellow
Write-Host "   Action: Ouvrir fichier HLS" -ForegroundColor Gray
adb shell am start -a android.intent.action.VIEW -d "https://stream.example.com/video.m3u8" -t "application/vnd.apple.mpegurl" 2>&1 | Out-Null
Start-Sleep -Seconds 2

$logs = adb logcat -d -s PlayerActivity:D | Select-String "ACTION_VIEW" -SimpleMatch
if ($logs) {
    Write-Host "   ✅ STV peut gérer les fichiers .m3u8" -ForegroundColor Green
    Write-Host "   📝 Log: $($logs[-1])`n" -ForegroundColor Gray
} else {
    Write-Host "   ⚠️  Pas de log ACTION_VIEW détecté`n" -ForegroundColor Yellow
}

Start-Sleep -Seconds 3

# Test 3 : Action personnalisée (SoukiTV)
Write-Host "Test 3️⃣ : Action personnalisée (PLAY_STREAM)" -ForegroundColor Yellow
Write-Host "   Action: Simulation lancement depuis SoukiTV" -ForegroundColor Gray
adb shell am start -a com.example.stv.action.PLAY_STREAM -e VIDEO_URL "$url" --es VIDEO_URL "$url" -n com.example.stv/.PlayerActivity 2>&1 | Out-Null
Start-Sleep -Seconds 2

$logs = adb logcat -d -s PlayerActivity:D | Select-String "catalog app" -SimpleMatch
if ($logs) {
    Write-Host "   ✅ STV lancé via action personnalisée" -ForegroundColor Green
    Write-Host "   📝 Log: $($logs[-1])`n" -ForegroundColor Gray
} else {
    Write-Host "   ⚠️  Pas de log action personnalisée détecté`n" -ForegroundColor Yellow
}

# Test Manuel
Write-Host "`n📱 TEST MANUEL REQUIS:" -ForegroundColor Yellow
Write-Host "─────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host "1. Ouvrez SoukiTV sur le téléphone" -ForegroundColor White
Write-Host "2. Cliquez sur une chaîne (ex: NASA TV)" -ForegroundColor White
Write-Host "3. Vérifiez que:" -ForegroundColor White
Write-Host "   ✅ STV s'ouvre DIRECTEMENT (pas de chooser)" -ForegroundColor Green
Write-Host "   ✅ Publicité AdMob s'affiche" -ForegroundColor Green
Write-Host "   ✅ Vidéo démarre" -ForegroundColor Green
Write-Host "`n4. Si VLC est installé, testez:" -ForegroundColor White
Write-Host "   - Cliquez sur un lien .m3u8 dans le navigateur" -ForegroundColor Gray
Write-Host "   - Vérifiez que STV apparaît dans le chooser" -ForegroundColor Gray

Write-Host "`n📊 LOGS EN TEMPS RÉEL:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host "Pour voir les logs pendant les tests:" -ForegroundColor White
Write-Host "   adb logcat -s PlayerActivity:D" -ForegroundColor Yellow
Write-Host "   (Ctrl+C pour arrêter)`n" -ForegroundColor Gray

Write-Host "✅ Tests automatisés terminés!" -ForegroundColor Green
Write-Host "⏳ Effectuez maintenant le test manuel dans SoukiTV`n" -ForegroundColor Yellow

