# Script d'installation automatique des APKs STV + SoukiTV
# Date: 26 Février 2026

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "📱 INSTALLATION STV + SOUKITV" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

# Chemins des APKs
$stvApk = "app\build\outputs\apk\prod\release\app-prod-release.apk"
$soukiApk = "soukitv\build\outputs\apk\release\soukitv-release.apk"

# Vérifier que les APKs existent
Write-Host "🔍 Vérification des fichiers..." -ForegroundColor Yellow

if (-not (Test-Path $stvApk)) {
    Write-Host "❌ ERREUR: app-prod-release.apk introuvable!" -ForegroundColor Red
    Write-Host "   Exécutez d'abord: .\gradlew :app:assembleProdRelease" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $soukiApk)) {
    Write-Host "❌ ERREUR: soukitv-release.apk introuvable!" -ForegroundColor Red
    Write-Host "   Exécutez d'abord: .\gradlew :soukitv:assembleRelease" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ STV Player APK trouvé ($(([math]::Round((Get-Item $stvApk).Length / 1MB, 2))) MB)" -ForegroundColor Green
Write-Host "✅ SoukiTV APK trouvé ($(([math]::Round((Get-Item $soukiApk).Length / 1MB, 2))) MB)" -ForegroundColor Green

# Vérifier que adb est disponible
Write-Host "`n🔍 Vérification de ADB..." -ForegroundColor Yellow

$adbPath = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbPath) {
    Write-Host "❌ ERREUR: ADB non trouvé dans le PATH!" -ForegroundColor Red
    Write-Host "   Assurez-vous que Android SDK Platform-Tools est installé" -ForegroundColor Yellow
    Write-Host "   Ou ajoutez le chemin vers adb.exe dans le PATH système" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ ADB trouvé: $($adbPath.Source)" -ForegroundColor Green

# Vérifier qu'un device est connecté
Write-Host "`n🔍 Vérification des appareils connectés..." -ForegroundColor Yellow

$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ ERREUR: Aucun appareil Android détecté!" -ForegroundColor Red
    Write-Host "`n📱 Instructions:" -ForegroundColor Yellow
    Write-Host "   1. Connectez votre téléphone via USB" -ForegroundColor White
    Write-Host "   2. Activez le 'Débogage USB' dans Paramètres > Options développeur" -ForegroundColor White
    Write-Host "   3. Acceptez l'autorisation de débogage sur le téléphone" -ForegroundColor White
    Write-Host "   4. Relancez ce script`n" -ForegroundColor White
    exit 1
}

Write-Host "✅ Appareil connecté détecté" -ForegroundColor Green

# Demander confirmation
Write-Host "`n⚠️  Les anciennes versions de STV et SoukiTV seront désinstallées" -ForegroundColor Yellow
$confirmation = Read-Host "Continuer l'installation? (O/N)"

if ($confirmation -ne "O" -and $confirmation -ne "o" -and $confirmation -ne "Y" -and $confirmation -ne "y") {
    Write-Host "`n❌ Installation annulée" -ForegroundColor Red
    exit 0
}

# Désinstaller les anciennes versions
Write-Host "`n🧹 Désinstallation des anciennes versions..." -ForegroundColor Yellow

$packages = @(
    "com.example.stv",
    "com.example.stv.dev",
    "com.example.stv.prod",
    "com.example.soukitv"
)

foreach ($pkg in $packages) {
    Write-Host "   Désinstallation de $pkg..." -ForegroundColor Gray
    adb uninstall $pkg 2>&1 | Out-Null
}

Write-Host "✅ Nettoyage terminé" -ForegroundColor Green

# Installer STV Player
Write-Host "`n📱 Installation de STV Player..." -ForegroundColor Cyan
Write-Host "   Package: com.example.stv" -ForegroundColor Gray
Write-Host "   Taille: $(([math]::Round((Get-Item $stvApk).Length / 1MB, 2))) MB" -ForegroundColor Gray

$result = adb install -r $stvApk 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ STV Player installé avec succès!" -ForegroundColor Green
} else {
    Write-Host "❌ ERREUR lors de l'installation de STV Player" -ForegroundColor Red
    Write-Host $result -ForegroundColor Red
    exit 1
}

# Installer SoukiTV
Write-Host "`n📺 Installation de SoukiTV..." -ForegroundColor Cyan
Write-Host "   Package: com.example.soukitv" -ForegroundColor Gray
Write-Host "   Taille: $(([math]::Round((Get-Item $soukiApk).Length / 1MB, 2))) MB" -ForegroundColor Gray

$result = adb install -r $soukiApk 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ SoukiTV installé avec succès!" -ForegroundColor Green
} else {
    Write-Host "❌ ERREUR lors de l'installation de SoukiTV" -ForegroundColor Red
    Write-Host $result -ForegroundColor Red
    exit 1
}

# Résumé final
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "🎉 INSTALLATION TERMINÉE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`n📱 Applications installées:" -ForegroundColor White
Write-Host "   ✅ STV Player (com.example.stv)" -ForegroundColor Green
Write-Host "   ✅ SoukiTV (com.example.soukitv)" -ForegroundColor Green

Write-Host "`n🧪 Tests recommandés:" -ForegroundColor Yellow
Write-Host "   1. Ouvrez STV Player et testez une URL:" -ForegroundColor White
Write-Host "      https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8" -ForegroundColor Cyan
Write-Host "`n   2. Ouvrez SoukiTV et cliquez sur une chaîne" -ForegroundColor White
Write-Host "      (Devrait afficher 'Installer STV Player' car Deep Link non implémenté)" -ForegroundColor Gray

Write-Host "`n📋 Voir les logs en temps réel:" -ForegroundColor Yellow
Write-Host "   adb logcat | Select-String 'stv'" -ForegroundColor Cyan

Write-Host "`n📝 Rapport complet: APK_RELEASE_READY.md`n" -ForegroundColor White

