Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST SPLASH SCREEN STV - FINAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier appareil
Write-Host "Vérification appareil..." -ForegroundColor Yellow
$devices = adb devices 2>&1 | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ ERREUR : Aucun appareil connecté" -ForegroundColor Red
    Write-Host ""
    Write-Host "Actions requises :" -ForegroundColor Yellow
    Write-Host "1. Connecter téléphone via USB"
    Write-Host "2. Activer débogage USB"
    Write-Host "3. Relancer ce script"
    Read-Host "`nAppuyez sur Entrée pour quitter"
    exit
}
Write-Host "✅ Appareil détecté" -ForegroundColor Green
Write-Host ""

# Chemin APK
$apk = "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"
if (!(Test-Path $apk)) {
    Write-Host "❌ ERREUR : APK non trouvé" -ForegroundColor Red
    exit
}
Write-Host "✅ APK trouvé" -ForegroundColor Green
Write-Host ""

# Désinstallation complète
Write-Host "Désinstallation ancienne version..." -ForegroundColor Yellow
adb uninstall com.example.stv 2>&1 | Out-Null
adb shell pm clear com.example.stv 2>&1 | Out-Null
Write-Host "✅ Nettoyage complet effectué" -ForegroundColor Green
Write-Host ""

# Installation
Write-Host "Installation nouvelle version..." -ForegroundColor Yellow
$result = adb install -r $apk 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERREUR installation" -ForegroundColor Red
    Write-Host $result
    exit
}
Write-Host "✅ Installation réussie" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       TEST DU SPLASH SCREEN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Je vais lancer l'app 3 fois." -ForegroundColor White
Write-Host ""
Write-Host "À OBSERVER SUR TON TÉLÉPHONE :" -ForegroundColor Yellow
Write-Host "  ✓ Logo STV (cercle ROUGE + triangle BLANC)" -ForegroundColor White
Write-Host "  ✓ Fond NOIR" -ForegroundColor White
Write-Host "  ✓ Visible pendant 1 SECONDE" -ForegroundColor White
Write-Host "  ✓ Puis transition vers l'accueil" -ForegroundColor White
Write-Host ""

Read-Host "Appuyez sur Entrée pour commencer"

for ($i = 1; $i -le 3; $i++) {
    Write-Host ""
    Write-Host ">>> TEST $i/3 <<<" -ForegroundColor Yellow

    # Fermer app
    adb shell am force-stop com.example.stv 2>&1 | Out-Null
    Start-Sleep -Seconds 2

    # Lancer
    Write-Host "Lancement de l'app..." -ForegroundColor White
    adb shell am start -n com.example.stv/.MainActivity 2>&1 | Out-Null

    Write-Host ">>> REGARDE TON TÉLÉPHONE MAINTENANT ! <<<" -ForegroundColor Green -BackgroundColor Black

    if ($i -lt 3) {
        Start-Sleep -Seconds 5
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "          RÉSULTATS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$reponse = Read-Host "Le splash screen STV est-il apparu ? (o/n)"

if ($reponse -eq "o" -or $reponse -eq "O") {
    Write-Host ""
    Write-Host "🎉 FÉLICITATIONS ! SPLASH SCREEN VALIDÉ !" -ForegroundColor Green
    Write-Host ""
    Write-Host "Prochaines étapes :" -ForegroundColor Yellow
    Write-Host "  1. Tester navigation (voir PLAN_TESTS_STV_RELEASE.md)"
    Write-Host "  2. Tester ajout/suppression vidéos"
    Write-Host "  3. Tester lecture vidéo"
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ PROBLÈME DÉTECTÉ" -ForegroundColor Red
    Write-Host ""
    Write-Host "Actions de dépannage :" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Vérifier version Android :" -ForegroundColor White
    Write-Host "   adb shell getprop ro.build.version.sdk" -ForegroundColor Cyan
    Write-Host "   (doit être ≥ 21)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Voir les logs splash screen :" -ForegroundColor White
    Write-Host "   adb logcat -s SplashScreen:* MainActivity:*" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "3. Redémarrer le téléphone puis retester" -ForegroundColor White
    Write-Host ""
    Write-Host "Documentation : SPLASH_SCREEN_FIX_DEFINITIF.md" -ForegroundColor Yellow
    Write-Host ""
}

Read-Host "Appuyez sur Entrée pour quitter"

