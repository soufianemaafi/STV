# Guide de test rapide - STV Player

## Installation rapide

### Option 1 : Via script PowerShell (recommandé)
```powershell
cd C:\Users\soufi\AndroidStudioProjects\STV5
.\test_apk.ps1
```
Ce script :
- ✅ Vérifie qu'un appareil est connecté
- ✅ Désinstalle l'ancienne version
- ✅ Installe la nouvelle APK
- ✅ Lance l'application
- ✅ Affiche les logs en temps réel

### Option 2 : Installation manuelle via ADB
```powershell
# 1. Vérifier l'appareil
adb devices

# 2. Installer l'APK
adb install -r "app\build\outputs\apk\prod\release\app-prod-release.apk"

# 3. Lancer l'app
adb shell am start -n com.example.stv/.MainActivity

# 4. Voir les logs
adb logcat | Select-String "STV"
```

### Option 3 : Installation sur téléphone (sans PC)
1. Copier `app-prod-release.apk` sur le téléphone (USB, email, cloud)
2. Ouvrir le fichier avec le gestionnaire de fichiers
3. Autoriser "Sources inconnues" si demandé
4. Installer

---

## Tests rapides (5 minutes)

### ✅ Test 1 : Premier lancement
- Lancer l'app
- **Vérifier que le splash screen STV apparaît** (logo rouge avec triangle play, ~1 seconde)
- Vérifier que l'accueil s'affiche avec "STV" en haut

### ✅ Test 2 : Ajouter une vidéo
- Cliquer sur "+" au centre
- Titre : `Test`
- URL : `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`
- Cliquer "Save"
- Aller dans "Mes vidéos"
- Vérifier que "Test" apparaît

### ✅ Test 3 : Lire une vidéo
- Cliquer sur "Big Buck Bunny"
- Attendre la pub (ou 5s si fallback)
- Vérifier que la vidéo lit

### ✅ Test 4 : Navigation
- Depuis la liste, cliquer "+"
- Ajouter une vidéo
- Cliquer "Save"
- Vérifier retour direct à la liste (pas l'accueil)
- Vérifier que la nouvelle vidéo apparaît immédiatement

### ✅ Test 5 : Supprimer une vidéo
- Dans la liste, cliquer l'icône delete sur "Test"
- Vérifier que la vidéo disparaît

---

## Tests complets (30 minutes)

Voir le fichier détaillé : `PLAN_TESTS_STV_RELEASE.md`

---

## Commandes utiles

### Désinstaller l'app
```powershell
adb uninstall com.example.stv
```

### Voir les logs en temps réel
```powershell
adb logcat -s "STV:*" "PlayerActivity:*" "VideoListActivity:*"
```

### Capturer un screenshot
```powershell
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### Vérifier la signature de l'APK
```powershell
$keytool = "C:\Program Files\JetBrains\AndroidStudio\jbr\bin\keytool.exe"
& $keytool -printcert -jarfile "app\build\outputs\apk\prod\release\app-prod-release.apk"
```

---

## Checklist avant publication Play Store

- [ ] Tous les tests passent
- [ ] Aucun crash détecté
- [ ] Navigation cohérente
- [ ] Liste mise à jour automatiquement
- [ ] Player lit les vidéos
- [ ] **⚠️ REMPLACER IDs AdMob test par IDs production**
- [ ] Tester sur Android 7, 10, 13 minimum
- [ ] Vérifier sur différentes tailles d'écran

---

## Problèmes connus

### Pub ne s'affiche pas
- Normal : IDs test AdMob peuvent échouer selon réseau
- Comportement : Fallback bannière 5s puis player démarre
- À corriger : Remplacer par IDs production

### Navigation retour ne fonctionne pas bien
- ✅ Corrigé dans cette version
- Vérifier : Retour depuis AddVideoActivity va bien à la liste

### Liste ne se met pas à jour
- ✅ Corrigé dans cette version
- Vérifier : onResume() recharge automatiquement

---

## Support

**Documentation complète** :
- `PLAN_TESTS_STV_RELEASE.md` - Plan de tests détaillé
- `RAPPORT_VERIFICATION_STV_2026-02-27.md` - Rapport d'audit
- `GUIDE_DEVELOPPEMENT_CATALOGUES.md` - Guide pour apps catalogue

