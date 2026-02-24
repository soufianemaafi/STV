# Script d'installation automatique pour Xiaomi
# Sauvegarder ce fichier en tant que: install_apps.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INSTALLATION STV + SOUKITV SUR XIAOMI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Chemins des APKs
$stvApk = "C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk"
$soukitvApk = "C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk"

# Vérifier que les APKs existent
if (-not (Test-Path $stvApk)) {
    Write-Host "[ERREUR] APK STV non trouvé: $stvApk" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $soukitvApk)) {
    Write-Host "[ERREUR] APK SoukiTV non trouvé: $soukitvApk" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] APKs trouvés" -ForegroundColor Green
Write-Host "  STV: $stvApk" -ForegroundColor Gray
Write-Host "  SoukiTV: $soukitvApk" -ForegroundColor Gray
Write-Host ""

# Trouver ADB
$adbPaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe",
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)

$adb = $null
foreach ($path in $adbPaths) {
    if (Test-Path $path) {
        $adb = $path
        break
    }
}

if ($adb -eq $null) {
    Write-Host "[ERREUR] ADB non trouvé. Veuillez installer Android SDK Platform Tools" -ForegroundColor Red
    Write-Host "Téléchargement: https://developer.android.com/studio/releases/platform-tools" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "OU installer manuellement:" -ForegroundColor Yellow
    Write-Host "1. Connecter le téléphone au PC via USB" -ForegroundColor Yellow
    Write-Host "2. Activer 'Sources inconnues' dans les paramètres" -ForegroundColor Yellow
    Write-Host "3. Copier les APKs sur le téléphone" -ForegroundColor Yellow
    Write-Host "4. Installer manuellement depuis l'explorateur de fichiers" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] ADB trouvé: $adb" -ForegroundColor Green
Write-Host ""

# Vérifier la connexion
Write-Host "Vérification de la connexion au téléphone..." -ForegroundColor Cyan
$devices = & $adb devices
Write-Host $devices

if ($devices -notmatch "device$") {
    Write-Host ""
    Write-Host "[ERREUR] Aucun appareil détecté!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Vérifiez que:" -ForegroundColor Yellow
    Write-Host "1. Le téléphone est connecté via USB" -ForegroundColor Yellow
    Write-Host "2. Le débogage USB est activé:" -ForegroundColor Yellow
    Write-Host "   - Paramètres → À propos → Appuyer 7x sur 'Version MIUI'" -ForegroundColor Yellow
    Write-Host "   - Paramètres → Paramètres supplémentaires → Options développeurs" -ForegroundColor Yellow
    Write-Host "   - Activer 'Débogage USB'" -ForegroundColor Yellow
    Write-Host "3. Autoriser le débogage USB sur le téléphone (popup)" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Téléphone connecté" -ForegroundColor Green
Write-Host ""

# Désinstaller les anciennes versions
Write-Host "Désinstallation des anciennes versions (si présentes)..." -ForegroundColor Cyan
& $adb uninstall com.example.stv 2>$null
& $adb uninstall com.example.soukitv 2>$null
Write-Host "[OK] Nettoyage terminé" -ForegroundColor Green
Write-Host ""

# Installer STV Player
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Installation 1/2: STV Player..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
$result1 = & $adb install -r $stvApk 2>&1
Write-Host $result1

if ($result1 -match "Success") {
    Write-Host "[OK] STV Player installé avec succès!" -ForegroundColor Green
} else {
    Write-Host "[ERREUR] Échec de l'installation de STV Player" -ForegroundColor Red
    Write-Host $result1
    exit 1
}
Write-Host ""

# Installer SoukiTV
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Installation 2/2: SoukiTV..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
$result2 = & $adb install -r $soukitvApk 2>&1
Write-Host $result2

if ($result2 -match "Success") {
    Write-Host "[OK] SoukiTV installé avec succès!" -ForegroundColor Green
} else {
    Write-Host "[ERREUR] Échec de l'installation de SoukiTV" -ForegroundColor Red
    Write-Host $result2
    exit 1
}
Write-Host ""

# Succès
Write-Host "========================================" -ForegroundColor Green
Write-Host "  INSTALLATION TERMINÉE AVEC SUCCÈS!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Les applications sont installées:" -ForegroundColor Cyan
Write-Host "  ✅ STV Player (com.example.stv)" -ForegroundColor Green
Write-Host "  ✅ SoukiTV (com.example.soukitv)" -ForegroundColor Green
Write-Host ""

# Lancer SoukiTV
Write-Host "Lancement de SoukiTV pour test..." -ForegroundColor Cyan
& $adb shell am start -n com.example.soukitv/.MainActivity
Write-Host ""
Write-Host "SoukiTV devrait s'ouvrir sur votre téléphone!" -ForegroundColor Green
Write-Host ""
Write-Host "Pour tester:" -ForegroundColor Yellow
Write-Host "1. Sélectionnez une chaîne dans SoukiTV" -ForegroundColor Yellow
Write-Host "2. STV Player devrait s'ouvrir automatiquement" -ForegroundColor Yellow
Write-Host "3. Une publicité test devrait s'afficher" -ForegroundColor Yellow
Write-Host "4. La vidéo devrait se lancer après la pub" -ForegroundColor Yellow
Write-Host ""
Write-Host "Bon test! 📱✨" -ForegroundColor Cyan

