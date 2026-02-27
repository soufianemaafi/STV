# Script de test - Splash Screen STV

Write-Host "=== Test Splash Screen STV ===" -ForegroundColor Cyan
Write-Host ""

# Vérifier appareil connecté
$devices = adb devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ Aucun appareil Android connecté!" -ForegroundColor Red
    Write-Host "Connectez un appareil via USB et activez le débogage USB" -ForegroundColor Yellow
    Read-Host "Appuyez sur Entrée pour quitter"
    exit
}

Write-Host "✅ Appareil détecté" -ForegroundColor Green
Write-Host ""

# Chemin APK
$apkPath = "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK non trouvé!" -ForegroundColor Red
    exit
}

Write-Host "✅ APK trouvé" -ForegroundColor Green
Write-Host ""

# Désinstaller ancienne version
Write-Host "=== Désinstallation ancienne version ===" -ForegroundColor Cyan
adb uninstall com.example.stv 2>$null
Write-Host "✅ Nettoyage effectué" -ForegroundColor Green
Write-Host ""

# Installer nouvelle version
Write-Host "=== Installation nouvelle version ===" -ForegroundColor Cyan
$result = adb install -r $apkPath 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation réussie" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur installation:" -ForegroundColor Red
    Write-Host $result
    exit
}
Write-Host ""

# Instructions test manuel
Write-Host "=== TEST SPLASH SCREEN ===" -ForegroundColor Yellow
Write-Host ""
Write-Host "Je vais lancer l'app 5 fois pour tester le splash screen." -ForegroundColor White
Write-Host "Observez bien l'écran à chaque lancement !" -ForegroundColor White
Write-Host ""
Write-Host "À VÉRIFIER :" -ForegroundColor Cyan
Write-Host "  ✓ Logo STV (cercle rouge + triangle play blanc)" -ForegroundColor White
Write-Host "  ✓ Visible pendant ~1 seconde" -ForegroundColor White
Write-Host "  ✓ Transition fluide vers l'accueil" -ForegroundColor White
Write-Host ""

Read-Host "Appuyez sur Entrée pour commencer les tests"

# Lancer 5 fois avec pause entre chaque
for ($i = 1; $i -le 5; $i++) {
    Write-Host ""
    Write-Host ">>> Test $i/5 - Lancement de l'app..." -ForegroundColor Yellow

    # Fermer l'app si elle tourne
    adb shell am force-stop com.example.stv 2>$null
    Start-Sleep -Seconds 1

    # Lancer l'app
    adb shell am start -n com.example.stv/.MainActivity 2>$null

    Write-Host "    Observez le splash screen maintenant !" -ForegroundColor Green

    # Attendre avant le prochain test
    if ($i -lt 5) {
        Start-Sleep -Seconds 4
    }
}

Write-Host ""
Write-Host "=== FIN DES TESTS ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "RÉSULTATS À NOTER :" -ForegroundColor Yellow
Write-Host "  [ ] Splash screen visible à chaque lancement ?" -ForegroundColor White
Write-Host "  [ ] Logo rouge avec triangle play visible ?" -ForegroundColor White
Write-Host "  [ ] Durée appropriée (~1 seconde) ?" -ForegroundColor White
Write-Host "  [ ] Transition fluide vers l'accueil ?" -ForegroundColor White
Write-Host "  [ ] Pas de clignotement ou saccades ?" -ForegroundColor White
Write-Host ""

$validation = Read-Host "Le splash screen fonctionne-t-il correctement ? (o/n)"

if ($validation -eq "o" -or $validation -eq "O") {
    Write-Host ""
    Write-Host "✅ SPLASH SCREEN VALIDÉ !" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "⚠️ Problème détecté" -ForegroundColor Yellow
    Write-Host "Vérifiez les logs avec :" -ForegroundColor White
    Write-Host "  adb logcat -s SplashScreen:* MainActivity:*" -ForegroundColor Cyan
    Write-Host ""
}

Read-Host "Appuyez sur Entrée pour quitter"

