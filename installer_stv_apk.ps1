# Script d'installation APK - STV Player (sans besoin d'adb dans PATH)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INSTALLATION APK - STV PLAYER v1.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Chemins possibles pour adb.exe
$adbPaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe",
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)

# Chercher adb.exe
$adb = $null
foreach ($path in $adbPaths) {
    if (Test-Path $path) {
        $adb = $path
        Write-Host "✅ ADB trouvé : $adb" -ForegroundColor Green
        break
    }
}

if (-not $adb) {
    Write-Host "❌ ADB non trouvé dans les emplacements standard" -ForegroundColor Red
    Write-Host ""
    Write-Host "SOLUTIONS :" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. MÉTHODE SIMPLE (Recommandée) - Installation manuelle :" -ForegroundColor Cyan
    Write-Host "   a. Connecter téléphone via USB" -ForegroundColor White
    Write-Host "   b. Sur le téléphone, activer 'Sources inconnues' dans paramètres" -ForegroundColor White
    Write-Host "   c. Copier l'APK sur le téléphone (USB, email, cloud)" -ForegroundColor White
    Write-Host "   d. Ouvrir l'APK avec le gestionnaire de fichiers" -ForegroundColor White
    Write-Host "   e. Cliquer 'Installer'" -ForegroundColor White
    Write-Host ""
    Write-Host "2. MÉTHODE AVANCÉE - Installer Android SDK Platform Tools :" -ForegroundColor Cyan
    Write-Host "   Télécharger depuis : https://developer.android.com/tools/releases/platform-tools" -ForegroundColor White
    Write-Host "   Extraire dans : C:\Android\platform-tools" -ForegroundColor White
    Write-Host "   Relancer ce script" -ForegroundColor White
    Write-Host ""
    Write-Host "APK à copier/installer :" -ForegroundColor Yellow
    Write-Host "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk" -ForegroundColor Green
    Write-Host ""
    Read-Host "Appuyez sur Entrée pour ouvrir le dossier de l'APK"
    Start-Process "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release"
    exit
}

# ADB trouvé, continuer l'installation
Write-Host ""

# Chemin APK
$apkPath = "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK non trouvé : $apkPath" -ForegroundColor Red
    exit
}

Write-Host "✅ APK trouvé" -ForegroundColor Green
$apkInfo = Get-Item $apkPath
Write-Host "   Taille : $([math]::Round($apkInfo.Length/1MB, 2)) MB" -ForegroundColor Gray
Write-Host "   Date : $($apkInfo.LastWriteTime)" -ForegroundColor Gray
Write-Host ""

# Vérifier appareil connecté
Write-Host "Vérification appareil Android..." -ForegroundColor Yellow
$devices = & $adb devices 2>&1 | Select-String -Pattern "device$"

if ($devices.Count -eq 0) {
    Write-Host "❌ Aucun appareil connecté !" -ForegroundColor Red
    Write-Host ""
    Write-Host "ACTIONS REQUISES :" -ForegroundColor Yellow
    Write-Host "1. Connecter téléphone Android via USB" -ForegroundColor White
    Write-Host "2. Sur le téléphone : Paramètres → À propos → Appuyer 7 fois sur 'Numéro de build'" -ForegroundColor White
    Write-Host "3. Paramètres → Options développeur → Activer 'Débogage USB'" -ForegroundColor White
    Write-Host "4. Autoriser le débogage sur le téléphone (pop-up)" -ForegroundColor White
    Write-Host "5. Relancer ce script" -ForegroundColor White
    Write-Host ""
    Write-Host "OU utiliser la méthode manuelle (voir ci-dessus)" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "Appuyez sur Entrée pour ouvrir le dossier de l'APK"
    Start-Process "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release"
    exit
}

Write-Host "✅ Appareil connecté détecté" -ForegroundColor Green
Write-Host ""

# Désinstaller ancienne version
Write-Host "Désinstallation ancienne version..." -ForegroundColor Yellow
& $adb uninstall com.example.stv 2>&1 | Out-Null
Write-Host "✅ Nettoyage effectué" -ForegroundColor Green
Write-Host ""

# Installer nouvelle version
Write-Host "Installation de STV Player v1.0..." -ForegroundColor Yellow
$installResult = & $adb install -r $apkPath 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅✅✅ INSTALLATION RÉUSSIE ! ✅✅✅" -ForegroundColor Green
    Write-Host ""
    Write-Host "📱 STV Player v1.0 installé avec succès" -ForegroundColor Cyan
    Write-Host ""

    # Lancer l'application
    Write-Host "Lancement de l'application..." -ForegroundColor Yellow
    & $adb shell am start -n com.example.stv/.MainActivity 2>&1 | Out-Null
    Write-Host "✅ Application lancée" -ForegroundColor Green
    Write-Host ""

    Write-Host "🎯 À TESTER SUR TON TÉLÉPHONE :" -ForegroundColor Yellow
    Write-Host "  1. Splash screen STV (logo rouge ~500ms)" -ForegroundColor White
    Write-Host "  2. Accueil avec menu et boutons" -ForegroundColor White
    Write-Host "  3. Menu → Politique de Confidentialité" -ForegroundColor White
    Write-Host "  4. Menu → Conditions d'Utilisation" -ForegroundColor White
    Write-Host "  5. Ajouter une vidéo + tester lecture" -ForegroundColor White
    Write-Host ""
    Write-Host "⚠️ N'OUBLIE PAS :" -ForegroundColor Red
    Write-Host "  - Remplacer 'support@example.com' par ton email" -ForegroundColor White
    Write-Host "  - Rebuild APK après modification" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "❌ ERREUR INSTALLATION" -ForegroundColor Red
    Write-Host $installResult
    Write-Host ""
    Write-Host "Essayer l'installation manuelle :" -ForegroundColor Yellow
    Read-Host "Appuyez sur Entrée pour ouvrir le dossier de l'APK"
    Start-Process "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release"
}

Write-Host ""
Read-Host "Appuyez sur Entrée pour quitter"

