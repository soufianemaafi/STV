@echo off
REM Script d'installation APK STV - Version Batch

setlocal enabledelayedexpansion

echo.
echo ====== INSTALLEUR APK STV PLAYER ======
echo.

REM Définir les chemins
set ADB=C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe
set APK=C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk

REM Vérifier ADB
if not exist "%ADB%" (
    echo ERREUR: ADB non trouve
    echo Recherche en cours...
    dir /s /b "C:\Users\soufi\AppData" | find "adb.exe"
    pause
    exit /b 1
)

echo ADB: OK - %ADB%
echo APK: OK - %APK%
echo.

REM Démarrer le serveur ADB
echo Demarrage du serveur ADB...
"%ADB%" start-server
timeout /t 3

echo.
echo Appareils connectes:
"%ADB%" devices
echo.

REM Installer l'APK
echo Installation de l'APK...
"%ADB%" install -r "%APK%"

if errorlevel 1 (
    echo.
    echo ERREUR lors de l'installation
    echo Assurez-vous que:
    echo  - Le telephone est bien connecte en USB
    echo  - Le debogage USB est active
    echo  - Vous avez accepte les permissions sur le telephone
    echo.
) else (
    echo.
    echo SUCCES! L'app a ete installee.
    echo.
    echo Voulez-vous lancer l'app? (O/N)
    set /p LAUNCH=

    if /i "!LAUNCH!"=="O" (
        echo Lancement de l'app...
        "%ADB%" shell am start -n com.example.stv/.MainActivity
        timeout /t 2
    )
)

echo.
pause

