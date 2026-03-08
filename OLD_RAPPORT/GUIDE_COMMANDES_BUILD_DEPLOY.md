# 🔧 GUIDE COMMANDES - Build & Déploiement STV & SoukiTV

**Date :** 26 Février 2026  
**Shell :** PowerShell (Windows)  
**Workspace :** C:\Users\Lenovo\StudioProjects\STV4

---

## 📋 TABLE DES MATIÈRES

1. [Commandes Build Basiques](#commandes-build-basiques)
2. [Installation sur Device/Emulator](#installation-sur-deviceemulator)
3. [Gestion des Flavors (dev/prod)](#gestion-des-flavors-devprod)
4. [Debugging & Logs](#debugging--logs)
5. [Testing Deep Link](#testing-deep-link)
6. [Build Release (APK signé)](#build-release-apk-signé)
7. [Maintenance & Nettoyage](#maintenance--nettoyage)
8. [Troubleshooting Rapide](#troubleshooting-rapide)

---

## 🛠️ COMMANDES BUILD BASIQUES

### Build STV (Debug)
```powershell
# Naviguer vers le workspace
cd C:\Users\Lenovo\StudioProjects\STV4

# Build debug (flavor dev par défaut)
.\gradlew :app:assembleDevDebug

# Ou en une ligne
cd C:\Users\Lenovo\StudioProjects\STV4; .\gradlew :app:assembleDevDebug
```

**Output :**
```
BUILD SUCCESSFUL in 45s
APK généré : app\build\outputs\apk\dev\debug\app-dev-debug.apk
```

---

### Build SoukiTV (Debug)
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4
.\gradlew :soukitv:assembleDebug
```

**Output :**
```
BUILD SUCCESSFUL in 30s
APK généré : soukitv\build\outputs\apk\debug\soukitv-debug.apk
```

---

### Build des DEUX apps en une commande
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4
.\gradlew :app:assembleDevDebug :soukitv:assembleDebug
```

---

## 📱 INSTALLATION SUR DEVICE/EMULATOR

### Pré-requis
```powershell
# Vérifier que adb est accessible
adb version

# Lister les devices connectés
adb devices
```

**Output attendu :**
```
List of devices attached
emulator-5554   device
```

⚠️ Si "unauthorized" → Débloquer le device et accepter le débogage USB

---

### Installer STV
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# Méthode 1 : Build + Install automatique
.\gradlew :app:installDevDebug

# Méthode 2 : Install manuel après build
adb install -r app\build\outputs\apk\dev\debug\app-dev-debug.apk
```

**Flag `-r` :** Réinstalle en gardant les données (utile pour tests)

---

### Installer SoukiTV
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# Méthode 1 : Build + Install automatique
.\gradlew :soukitv:installDebug

# Méthode 2 : Install manuel
adb install -r soukitv\build\outputs\apk\debug\soukitv-debug.apk
```

---

### Installer les DEUX apps d'un coup
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4
.\gradlew :app:installDevDebug :soukitv:installDebug
```

---

### Désinstaller une app
```powershell
# Désinstaller STV
adb uninstall com.example.stv.dev

# Désinstaller SoukiTV
adb uninstall com.example.soukitv

# Désinstaller les deux
adb uninstall com.example.stv.dev; adb uninstall com.example.soukitv
```

---

## 🎨 GESTION DES FLAVORS (dev/prod)

### Build STV flavor "dev" (Debug)
```powershell
.\gradlew :app:assembleDevDebug
```

**Package Name :** `com.example.stv.dev`

---

### Build STV flavor "prod" (Debug)
```powershell
.\gradlew :app:assembleProdDebug
```

**Package Name :** `com.example.stv.prod`

---

### Build STV flavor "prod" (Release - signé)
```powershell
.\gradlew :app:assembleProdRelease
```

**Output :**
```
APK signé : app\build\outputs\apk\prod\release\app-prod-release.apk
Taille : ~8-12 MB (après ProGuard)
```

---

### Lister tous les variants disponibles
```powershell
.\gradlew :app:tasks --group="build"
```

**Output extrait :**
```
assembleDevDebug
assembleDevRelease
assembleProdDebug
assembleProdRelease
```

---

## 🐛 DEBUGGING & LOGS

### Afficher les logs en temps réel
```powershell
# Tous les logs
adb logcat

# Filtrer par app (STV)
adb logcat | Select-String "stv"

# Filtrer par tag précis
adb logcat | Select-String "PlayerActivity"

# Filtrer par niveau (Erreurs uniquement)
adb logcat *:E
```

---

### Sauvegarder les logs dans un fichier
```powershell
# Logs complets
adb logcat > logs_complets.txt

# Logs filtrés (dernières 500 lignes)
adb logcat -d -t 500 > logs_recents.txt

# Logs d'une app spécifique (filtré)
adb logcat | Select-String "com.example.stv" > logs_stv.txt
```

**Ctrl+C** pour arrêter la capture

---

### Afficher les logs d'un crash précis
```powershell
# Dernières erreurs fatales (FATAL)
adb logcat *:E | Select-String "FATAL"

# Stack trace complet
adb logcat -b crash
```

---

### Nettoyer le buffer de logs
```powershell
adb logcat -c
```

---

## 🔗 TESTING DEEP LINK

### Ouvrir STV avec une URL de test
```powershell
# Vidéo de test Big Buck Bunny
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

# NASA TV (live)
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://ntv1.akamaized.net/hls/live/2013975/NASA-NTV1-HLS/master.m3u8"

# Sky News Arabia
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://stream.skynewsarabia.com/hls/sna_720.m3u8"
```

**Résultat attendu :**
- STV s'ouvre automatiquement
- Publicité interstitielle chargée (ou fallback bannière)
- Vidéo démarre après la pub

---

### Tester avec URL encodée (si caractères spéciaux)
```powershell
# Encoder manuellement les caractères spéciaux (:, /, ?)
# Exemple : https://example.com/video.m3u8
# Encodé  : https%3A%2F%2Fexample.com%2Fvideo.m3u8

adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https%3A%2F%2Ftest-streams.mux.dev%2Fx36xhzz%2Fx36xhzz.m3u8"
```

---

### Vérifier qu'aucune app ne gère stv:// (STV non installé)
```powershell
# Désinstaller STV
adb uninstall com.example.stv.dev

# Tenter d'ouvrir le Deep Link
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test.m3u8"
```

**Résultat attendu :**
```
Error: Activity not started, unable to resolve Intent
```

→ Confirme que SoukiTV doit afficher le dialogue "Installer STV"

---

## 📦 BUILD RELEASE (APK signé)

### Build STV en Release (Production)
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# Build Release (flavor prod)
.\gradlew :app:assembleProdRelease

# Vérifier que l'APK est signé
jarsigner -verify -verbose -certs app\build\outputs\apk\prod\release\app-prod-release.apk
```

**Output attendu :**
```
jar verified.
...
Signed by "CN=..., O=..., L=..."
```

---

### Tester l'APK Release sur device
```powershell
# Installer l'APK Release
adb install app\build\outputs\apk\prod\release\app-prod-release.apk

# Lancer l'app
adb shell am start -n com.example.stv/.MainActivity

# Tester le Deep Link
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
```

---

### Build SoukiTV en Release
```powershell
.\gradlew :soukitv:assembleRelease

# Vérifier signature
jarsigner -verify -verbose soukitv\build\outputs\apk\release\soukitv-release.apk
```

---

### Créer un Bundle AAB (pour Play Store)
```powershell
# STV Bundle (format Play Store)
.\gradlew :app:bundleProdRelease

# Output : app\build\outputs\bundle\prodRelease\app-prod-release.aab
```

**⚠️ Important :** Le Play Store préfère le format `.aab` (Android App Bundle) plutôt que `.apk`

---

## 🧹 MAINTENANCE & NETTOYAGE

### Nettoyer les builds précédents
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# Clean complet du projet
.\gradlew clean

# Clean uniquement STV
.\gradlew :app:clean

# Clean uniquement SoukiTV
.\gradlew :soukitv:clean
```

---

### Rebuild complet (clean + build)
```powershell
# STV
.\gradlew :app:clean :app:assembleDevDebug

# SoukiTV
.\gradlew :soukitv:clean :soukitv:assembleDebug

# Les deux
.\gradlew clean :app:assembleDevDebug :soukitv:assembleDebug
```

---

### Supprimer les fichiers temporaires Gradle
```powershell
# Supprimer le cache Gradle (⚠️ re-téléchargera les dépendances)
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches

# Supprimer les builds locaux
Remove-Item -Recurse -Force app\build, soukitv\build
```

---

### Vérifier la taille des APKs
```powershell
# STV Debug
(Get-Item app\build\outputs\apk\dev\debug\app-dev-debug.apk).Length / 1MB

# STV Release
(Get-Item app\build\outputs\apk\prod\release\app-prod-release.apk).Length / 1MB

# SoukiTV Debug
(Get-Item soukitv\build\outputs\apk\debug\soukitv-debug.apk).Length / 1MB
```

**Tailles attendues :**
- STV Debug : ~15-20 MB
- STV Release (ProGuard) : ~8-12 MB
- SoukiTV Debug : ~10-15 MB

---

## 🚨 TROUBLESHOOTING RAPIDE

### Problème : "Command not found: gradlew"
```powershell
# Vérifier que gradlew.bat existe
Test-Path .\gradlew.bat

# Si absent, recréer avec Gradle Wrapper
gradle wrapper

# Ou exécuter directement avec gradle (si installé globalement)
gradle :app:assembleDevDebug
```

---

### Problème : "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
**Cause :** Tentative d'installer une app avec une signature différente

**Solution :**
```powershell
# Désinstaller complètement l'ancienne version
adb uninstall com.example.stv.dev

# Puis réinstaller
.\gradlew :app:installDevDebug
```

---

### Problème : "adb: device offline"
```powershell
# Redémarrer le serveur adb
adb kill-server
adb start-server

# Re-lister les devices
adb devices
```

---

### Problème : Build échoue avec "Out of Memory"
```powershell
# Augmenter la mémoire Gradle (modifier gradle.properties)
echo "org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m" >> gradle.properties

# Ou temporairement
$env:GRADLE_OPTS="-Xmx4096m"
.\gradlew :app:assembleDevDebug
```

---

### Problème : ProGuard casse l'app (crash au lancement)
**Symptôme :** APK Release plante immédiatement, Debug fonctionne

**Solution :**
1. Vérifier les règles ProGuard dans `app/proguard-rules.pro`
2. Ajouter les exclusions nécessaires :
```proguard
# Media3 ExoPlayer
-keep class androidx.media3.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }

# Compose
-keep class androidx.compose.** { *; }
```
3. Rebuild Release : `.\gradlew :app:clean :app:assembleProdRelease`

---

### Problème : Deep Link ne fonctionne pas après install
**Cause :** Android n'a pas mis à jour le Intent Filter registry

**Solution :**
```powershell
# Forcer la réinstallation complète
adb uninstall com.example.stv.dev
.\gradlew :app:installDevDebug

# Tester immédiatement après installation
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
```

---

### Problème : SoukiTV ne détecte pas STV installé
**Debug :**
```powershell
# Vérifier que STV est bien installé
adb shell pm list packages | Select-String "stv"

# Output attendu :
# package:com.example.stv.dev
```

**Si absent :**
```powershell
.\gradlew :app:installDevDebug
```

**Si présent mais non détecté :**
→ Vérifier le code de SoukiTV (bloc `intent.resolveActivity()`)

---

## 🎯 COMMANDES UTILES AVANCÉES

### Lancer STV directement avec une URL (sans passer par SoukiTV)
```powershell
# Via Intent extra (ancien système)
adb shell am start -n com.example.stv.dev/.PlayerActivity --es VIDEO_URL "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

# Via Deep Link (nouveau système)
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
```

---

### Forcer l'arrêt de STV (si bloqué)
```powershell
adb shell am force-stop com.example.stv.dev
```

---

### Effacer les données de STV (reset complet)
```powershell
# Effacer données ET cache
adb shell pm clear com.example.stv.dev

# Effacer uniquement le cache
adb shell rm -rf /data/data/com.example.stv.dev/cache
```

---

### Capturer un screenshot du device
```powershell
# Prendre un screenshot
adb shell screencap -p /sdcard/screenshot.png

# Télécharger sur PC
adb pull /sdcard/screenshot.png C:\Users\Lenovo\Desktop\screenshot.png

# Nettoyer
adb shell rm /sdcard/screenshot.png
```

---

### Enregistrer une vidéo de l'écran (pour démo)
```powershell
# Démarrer l'enregistrement (max 180s)
adb shell screenrecord /sdcard/demo.mp4

# Arrêter avec Ctrl+C après usage

# Télécharger
adb pull /sdcard/demo.mp4 C:\Users\Lenovo\Desktop\demo_stv.mp4

# Nettoyer
adb shell rm /sdcard/demo.mp4
```

---

### Lister tous les packages installés sur le device
```powershell
# Tous les packages
adb shell pm list packages

# Uniquement ceux contenant "example"
adb shell pm list packages | Select-String "example"

# Uniquement apps tierces (non-système)
adb shell pm list packages -3
```

---

## 📚 RESSOURCES COMPLÉMENTAIRES

### Gradle Tasks Utiles
```powershell
# Lister toutes les tâches disponibles
.\gradlew tasks

# Lister uniquement les tâches de build
.\gradlew tasks --group="build"

# Lister les dépendances de STV
.\gradlew :app:dependencies

# Analyser pourquoi un build échoue (verbose)
.\gradlew :app:assembleDevDebug --stacktrace --info
```

---

### ADB Commandes Réseau
```powershell
# Connecter via Wi-Fi (device et PC sur même réseau)
# 1. Connecter device en USB
adb tcpip 5555
# 2. Déconnecter USB, récupérer IP du device (Settings > About > Status)
# 3. Connecter via Wi-Fi
adb connect 192.168.1.XXX:5555

# Déconnecter
adb disconnect
```

---

### Documentation Officielle
- **Gradle Build Tool :** https://docs.gradle.org/current/userguide/userguide.html
- **ADB (Android Debug Bridge) :** https://developer.android.com/tools/adb
- **ProGuard :** https://www.guardsquare.com/manual/home

---

## 🚀 WORKFLOW DE DÉVELOPPEMENT RECOMMANDÉ

### 1. Modifier le Code
```powershell
# Ouvrir dans JetBrains-AI (IntelliJ IDEA / Android Studio)
# Faire les modifications (ex: ajouter Deep Link)
```

---

### 2. Build & Test Rapide
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# Clean + Build + Install STV
.\gradlew :app:clean :app:installDevDebug

# Tester immédiatement
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

# Observer les logs
adb logcat | Select-String "PlayerActivity"
```

---

### 3. Si Bug → Debug
```powershell
# Capturer les logs du crash
adb logcat -b crash > crash_logs.txt

# Analyser le stack trace
notepad crash_logs.txt
```

---

### 4. Fix → Rebuild
```powershell
.\gradlew :app:clean :app:installDevDebug
# Re-tester
```

---

### 5. Test d'Intégration SoukiTV ↔ STV
```powershell
# Installer les deux apps
.\gradlew :app:installDevDebug :soukitv:installDebug

# Lancer SoukiTV
adb shell am start -n com.example.soukitv/.MainActivity

# Cliquer manuellement sur une chaîne dans SoukiTV
# Vérifier que STV s'ouvre correctement
```

---

### 6. Avant Commit Git
```powershell
# Build Release pour vérifier que ProGuard ne casse rien
.\gradlew :app:assembleProdRelease

# Si succès → commit
git add .
git commit -m "feat: Add Deep Link support to PlayerActivity"
git push
```

---

## 🎓 ASTUCES PRO

### Alias PowerShell (gagner du temps)
Ajouter dans votre profil PowerShell (`notepad $PROFILE`) :

```powershell
# Alias pour naviguer vers le projet
function stv { cd C:\Users\Lenovo\StudioProjects\STV4 }

# Alias pour build + install STV
function buildstv { .\gradlew :app:clean :app:installDevDebug }

# Alias pour logs STV
function logstv { adb logcat | Select-String "stv" }

# Alias pour tester Deep Link
function teststv { adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8" }
```

**Usage après redémarrage PowerShell :**
```powershell
stv        # Navigate vers projet
buildstv   # Build + Install
teststv    # Test Deep Link
logstv     # Voir les logs
```

---

**Auteur :** GitHub Copilot  
**Version :** 1.0  
**Date :** 26 Février 2026

