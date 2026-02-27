#!/usr/bin/env pwsh
# Script d'installation APK STV sur Android device

param(
    [string]$APKPath = "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"
)

# Chemins
$ADB = "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$APK = $APKPath

Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║          INSTALLEUR APK - STV Player Release             ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Vérifier que ADB existe
if (-not (Test-Path $ADB)) {
    Write-Host "❌ ERREUR : ADB non trouvé à $ADB" -ForegroundColor Red
    Write-Host "Assurez-vous que Android SDK est installé." -ForegroundColor Yellow
    exit 1
}

# Vérifier que l'APK existe
if (-not (Test-Path $APK)) {
    Write-Host "❌ ERREUR : APK non trouvé à $APK" -ForegroundColor Red
    Write-Host "Assurez-vous d'avoir builder l'APK avec : ./gradlew.bat assembleProdRelease" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ ADB trouvé : $ADB" -ForegroundColor Green
Write-Host "✅ APK trouvé : $APK" -ForegroundColor Green
Write-Host ""

# Démarrer le serveur ADB
Write-Host "🔌 Démarrage du serveur ADB..." -ForegroundColor Blue
& $ADB start-server 2>&1 | Out-Null
Start-Sleep -Seconds 2

# Lister les appareils
Write-Host "📱 Vérification des appareils connectés..." -ForegroundColor Blue
$devices = & $ADB devices 2>&1
Write-Host $devices

# Parser les appareils
$deviceList = $devices | Where-Object { $_ -match "^\w+\s+(device|emulator)" } | ForEach-Object { ($_ -split '\s+')[0] }

if ($deviceList.Count -eq 0) {
    Write-Host "❌ ERREUR : Aucun appareil Android trouvé !" -ForegroundColor Red
    Write-Host ""
    Write-Host "SOLUTIONS :" -ForegroundColor Yellow
    Write-Host "1. Connectez un téléphone Android via USB" -ForegroundColor Yellow
    Write-Host "2. Activez le débogage USB dans les paramètres du téléphone" -ForegroundColor Yellow
    Write-Host "   Settings - About phone - Build number (taper 7x)" -ForegroundColor Yellow
    Write-Host "   Settings - Developer options - USB debugging (activer)" -ForegroundColor Yellow
    Write-Host "3. Ou lancez un émulateur Android depuis Android Studio" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "✅ Appareils trouvés :" -ForegroundColor Green
$i = 0
foreach ($device in $deviceList) {
    Write-Host "   [$i] $device" -ForegroundColor Green
    $i++
}
Write-Host ""

# Si un seul appareil, l'utiliser directement
if ($deviceList.Count -eq 1) {
    $selectedDevice = $deviceList[0]
    Write-Host "Un seul appareil trouvé : $selectedDevice" -ForegroundColor Green
} else {
    # Demander à l'utilisateur de choisir
    $choice = Read-Host "Sélectionnez l'appareil (numéro)"
    $selectedDevice = $deviceList[$choice]

    if (-not $selectedDevice) {
        Write-Host "❌ Choix invalide" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "📦 Installation de l'APK sur $selectedDevice..." -ForegroundColor Blue
Write-Host "Ceci peut prendre 10-30 secondes..." -ForegroundColor Blue
Write-Host ""

# Installer l'APK
$installResult = & $ADB -s $selectedDevice install -r $APK 2>&1
Write-Host $installResult

# Vérifier le résultat
if ($installResult -match "Success") {
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║              ✅ INSTALLATION RÉUSSIE !                  ║" -ForegroundColor Green
    Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "L'app STV a été installée avec succès sur l'appareil." -ForegroundColor Green
    Write-Host ""
    Write-Host "Vous pouvez maintenant :" -ForegroundColor Cyan
    Write-Host "1. Ouvrir l'app STV sur votre appareil" -ForegroundColor Cyan
    Write-Host "2. Tester le lecteur vidéo" -ForegroundColor Cyan
    Write-Host "3. Vérifier que les publicités AdMob s'affichent" -ForegroundColor Cyan
    Write-Host "4. Tester l'intégration avec SoukiTV (si disponible)" -ForegroundColor Cyan
    Write-Host ""

    # Proposer de lancer l'app
    $launch = Read-Host "Voulez-vous lancer l'app maintenant ? (O/N)"
    if ($launch -eq "O" -or $launch -eq "o") {
        Write-Host "🚀 Lancement de l'app..." -ForegroundColor Blue
        & $ADB -s $selectedDevice shell am start -n com.example.stv/.MainActivity 2>&1 | Out-Null
        Write-Host "✅ App lancée !" -ForegroundColor Green
    }

} else {
    Write-Host ""
    Write-Host "❌ INSTALLATION ÉCHOUÉE" -ForegroundColor Red
    Write-Host ""
    Write-Host "POSSIBLES CAUSES :" -ForegroundColor Yellow
    Write-Host "1. App déjà installée - utiliser -r pour remplacer" -ForegroundColor Yellow
    Write-Host "2. Appareil manque d'espace" -ForegroundColor Yellow
    Write-Host "3. Connexion USB non stable" -ForegroundColor Yellow
    Write-Host "4. Permissions manquantes" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "SOLUTIONS :" -ForegroundColor Cyan
    Write-Host "1. Vérifier la connexion USB" -ForegroundColor Cyan
    Write-Host "2. Relancer le serveur ADB : adb kill-server && adb start-server" -ForegroundColor Cyan
    Write-Host "3. Désinstaller l'app et réessayer" -ForegroundColor Cyan
    Write-Host "4. Vérifier que Android 5.0+ est installé sur le device" -ForegroundColor Cyan
    exit 1
}

