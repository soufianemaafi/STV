# Script d'installation et tests automatiques - STV Player

# Vérifier qu'un appareil est connecté
Write-Host "=== Vérification appareil Android ===" -ForegroundColor Cyan
$devices = adb devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ Aucun appareil Android connecté!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Actions requises:" -ForegroundColor Yellow
    Write-Host "1. Connecter un appareil Android via USB"
    Write-Host "2. Activer le débogage USB dans les paramètres développeur"
    Write-Host "3. Relancer ce script"
    Write-Host ""
    Read-Host "Appuyez sur Entrée pour quitter"
    exit
}

Write-Host "✅ Appareil connecté détecté" -ForegroundColor Green
Write-Host ""

# Chemin de l'APK
$apkPath = "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

# Vérifier que l'APK existe
if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK non trouvé: $apkPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Veuillez d'abord compiler l'APK avec:" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat assembleProdRelease"
    Write-Host ""
    Read-Host "Appuyez sur Entrée pour quitter"
    exit
}

Write-Host "✅ APK trouvé" -ForegroundColor Green
$apkInfo = Get-Item $apkPath
Write-Host "   Taille: $([math]::Round($apkInfo.Length/1MB, 2)) MB"
Write-Host "   Date: $($apkInfo.LastWriteTime)"
Write-Host ""

# Désinstaller l'ancienne version si présente
Write-Host "=== Désinstallation ancienne version ===" -ForegroundColor Cyan
adb uninstall com.example.stv 2>$null
Write-Host "✅ Nettoyage effectué" -ForegroundColor Green
Write-Host ""

# Installer la nouvelle APK
Write-Host "=== Installation de l'APK ===" -ForegroundColor Cyan
$installResult = adb install -r $apkPath 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation réussie!" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur d'installation:" -ForegroundColor Red
    Write-Host $installResult
    Read-Host "Appuyez sur Entrée pour quitter"
    exit
}
Write-Host ""

# Lancer l'application
Write-Host "=== Lancement de STV Player ===" -ForegroundColor Cyan
adb shell am start -n com.example.stv/.MainActivity
Start-Sleep -Seconds 2
Write-Host "✅ Application lancée" -ForegroundColor Green
Write-Host ""

# Afficher les logs en temps réel
Write-Host "=== Logs de l'application (Ctrl+C pour arrêter) ===" -ForegroundColor Cyan
Write-Host "Filtrage: STV, PlayerActivity, VideoListActivity, AdManager" -ForegroundColor Yellow
Write-Host ""
adb logcat -s "STV:*" "PlayerActivity:*" "VideoListActivity:*" "AdManager:*" "AndroidRuntime:E"

