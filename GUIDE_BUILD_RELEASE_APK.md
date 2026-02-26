# 🚀 GUIDE BUILD & RELEASE - APK Production

**Date** : 26/02/2026  
**Objective** : Générer les APK Release testables et prêts pour PlayStore

---

## 📋 CHECKLIST PRÉ-BUILD

### Environnement Java

```powershell
# Vérifier que JAVA_HOME est défini
echo $env:JAVA_HOME

# Résultat attendu :
# C:\Program Files\Android\Android Studio\jbr (ou équivalent)

# Si vide, le définir :
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Dépendances

```powershell
# Vérifier gradle
./gradlew.bat --version

# Résultat attendu : Gradle 8.x+
```

---

## 🔨 BUILD APK RELEASE - STV Player

### Step 1 : Nettoyer et Préparer

```powershell
# Navigation
cd C:\Users\Lenovo\StudioProjects\STV4

# Clean (supprimer les anciens builds)
./gradlew.bat clean

# Expected output:
# BUILD SUCCESSFUL in Xs
```

### Step 2 : Build APK Release (Flavor Prod)

```powershell
# Build le flavor "prod" release
./gradlew.bat assembleReleaseProd

# Expected output:
# BUILD SUCCESSFUL in Xs
```

### Step 3 : Localiser l'APK

```powershell
# APK location
# C:\Users\Lenovo\StudioProjects\STV4\app\build\outputs\apk\releaseProd\

# Fichier généré :
# app-releaseProd-release.apk
```

---

## 🔨 BUILD APK RELEASE - SoukiTV

### Step 1 : Retourner à root et nettoyer

```powershell
# Navigation (si pas déjà à C:\Users\Lenovo\StudioProjects\STV4)
cd C:\Users\Lenovo\StudioProjects\STV4

# Le build de SoukiTV
./gradlew.bat :soukitv:assembleRelease

# Expected output:
# BUILD SUCCESSFUL in Xs
```

### Step 2 : Localiser l'APK SoukiTV

```powershell
# APK location
# C:\Users\Lenovo\StudioProjects\STV4\soukitv\build\outputs\apk\release\

# Fichier généré :
# soukitv-release.apk
```

---

## 📱 INSTALLATION SUR TÉLÉPHONE

### Méthode 1 : Via ADB (Recommandé)

```powershell
# Vérifier que le téléphone est connecté
adb devices

# Expected output:
# List of attached devices
# [DEVICE_ID]               device

# Installer STV
adb install -r "C:\Users\Lenovo\StudioProjects\STV4\app\build\outputs\apk\releaseProd\app-releaseProd-release.apk"

# Installer SoukiTV
adb install -r "C:\Users\Lenovo\StudioProjects\STV4\soukitv\build\outputs\apk\release\soukitv-release.apk"

# Expected output:
# Success
```

### Méthode 2 : Via File Manager

```powershell
# Copier les APK à un dossier connu
Copy-Item "C:\Users\Lenovo\StudioProjects\STV4\app\build\outputs\apk\releaseProd\app-releaseProd-release.apk" -Destination "C:\Users\Lenovo\Downloads\"
Copy-Item "C:\Users\Lenovo\StudioProjects\STV4\soukitv\build\outputs\apk\release\soukitv-release.apk" -Destination "C:\Users\Lenovo\Downloads\"

# Sur téléphone : ouvrir File Manager → Downloads
# Taper sur chaque APK → Installer
```

---

## ✅ TESTS DE VALIDATION

### Test 1 : STV Lance Seul (Sans SoukiTV)

```
1. Ouvrir Navigateur
2. Rechercher "test video m3u8"
3. Trouver un lien IPTV test
4. Taper sur le lien

Résultat Attendu :
├─ Android affiche chooser
├─ Options : STV Player, MX Player (si installé), etc.
├─ Sélectionner STV Player
└─ ✅ Vidéo joue immédiatement

Validation :
✅ STV fonctionne autonome
✅ Intent Filters HTTP/HTTPS marchent
✅ Pas de dépendance SoukiTV
```

### Test 2 : SoukiTV Force STV

```
1. Ouvrir SoukiTV
2. Sélectionner une chaîne

Résultat Attendu :
├─ SoukiTV détecte STV installée
├─ Lance STV AUTOMATIQUEMENT (pas de dialog)
├─ Vidéo joue immédiatement
└─ ✅ Pas de chooser

Validation :
✅ Intent Explicite fonctionne
✅ setPackage() force bien STV
✅ Pas d'alternatives affichées
```

### Test 3 : VLC/MX Player Coexiste

```
1. Installer VLC Player (PlayStore)
2. Ouvrir lien vidéo dans navigateur

Résultat Attendu :
├─ Android affiche chooser
├─ Options : STV Player, VLC, [Autres players]
├─ User peut choisir librement
└─ ✅ Pas de forçage abusif

Validation :
✅ STV n'interfère pas avec autres apps
✅ Liberté de l'utilisateur respectée
✅ Conforme PlayStore
```

### Test 4 : SoukiTV sans STV (Pas Installé)

```
1. Désinstaller STV Player
2. Ouvrir SoukiTV
3. Taper sur une chaîne

Résultat Attendu :
├─ SoukiTV détecte STV manquante
├─ Dialog : "STV Player required"
├─ Bouton : "Install STV"
├─ Redirection PlayStore
└─ ✅ User peut installer ou refuser

Validation :
✅ Gestion d'erreur correcte
✅ User freedom respectée
✅ Pas de crash app
```

### Test 5 : Picture-in-Picture (PiP)

```
1. Ouvrir vidéo dans STV
2. Taper bouton PiP
3. Appuyer home button

Résultat Attendu :
├─ Vidéo continue en PiP
├─ Appli STV reste en arrière-plan
└─ ✅ PiP fonctionne

Validation :
✅ PiP implementation correcte
✅ Pas de crash
```

### Test 6 : AdMob Ads

```
1. Ouvrir STV
2. Laisser se charger

Résultat Attendu :
├─ Bannière AdMob apparaît (test ad)
├─ Après 5s, vidéo joue
├─ Publicité interstitielle possible
└─ ✅ Ads chargent

Validation :
✅ AdMob configuration correcte
✅ Pas de blocage Adblock (test)
```

---

## 🔍 STRUCTURE DES APK GÉNÉRÉS

### STV APK (releaseProd)

```
app-releaseProd-release.apk (Flaveur PROD)
├─ Package Name : com.example.stv
├─ Version : 1.0
├─ Version Code : 1
├─ Signature : release.keystore
├─ Taille : ~50-80 MB
└─ Contenu :
   ├─ PlayerActivity
   ├─ VideoListActivity
   ├─ AdMob Interstitial + Banner
   ├─ ExoPlayer Media Library
   ├─ Compose UI Framework
   └─ Navigation Support
```

### SoukiTV APK (Release)

```
soukitv-release.apk
├─ Package Name : com.example.soukitv
├─ Version : 1.0
├─ Version Code : 1
├─ Signature : release.keystore
├─ Taille : ~30-50 MB
└─ Contenu :
   ├─ MainActivity (HomeScreen)
   ├─ Channel Catalogue Data
   ├─ STV Player Intent Handling
   ├─ Install Dialog System
   └─ UI Components
```

---

## 📊 VÉRIFICATION APK (Optionnel)

### Analyser l'APK avec APK Analyzer

```powershell
# Android Studio a un outil graphique
# Menu : Build → Analyze APK
# Sélectionner : app-releaseProd-release.apk
# Vérifier :
# ✅ Permissions correctes
# ✅ Pas de ressources inutiles
# ✅ Taille raisonnable
# ✅ Dépendances correctes
```

### Vérifier les Permissions

```powershell
# Utiliser aapt (Android Asset Packaging Tool)
# Présent dans : C:\Users\Lenovo\AppData\Local\Android\Sdk\build-tools\35.0.0\

# Exemple :
C:\Users\Lenovo\AppData\Local\Android\Sdk\build-tools\35.0.0\aapt dump permissions "C:\Users\Lenovo\StudioProjects\STV4\app\build\outputs\apk\releaseProd\app-releaseProd-release.apk"

# Expected output (STV) :
# uses-permission: android.permission.INTERNET
# uses-permission: android.permission.ACCESS_NETWORK_STATE

# Expected output (SoukiTV) :
# uses-permission: android.permission.INTERNET
# uses-permission: android.permission.ACCESS_NETWORK_STATE
# uses-permission: android.permission.QUERY_ALL_PACKAGES (pour détecter STV)
```

---

## 🔐 VÉRIFICATION SIGNATURE RELEASE

### Keystore Information

```powershell
# Fichier keystore
# C:\Users\Lenovo\StudioProjects\STV4\app\release.keystore

# Passwords (du build.gradle.kts) :
# storePassword = "android"
# keyAlias = "key0"
# keyPassword = "android"

# Vérifier le certificat :
keytool -list -v -keystore "C:\Users\Lenovo\StudioProjects\STV4\app\release.keystore" -storepass android

# Expected output :
# Alias name: key0
# Owner: CN=...
# Validity: ... (years)
# Certificate fingerprint (SHA-256): ...
```

---

## 📤 PRÉPARATION PLAYSTORE

### Fichiers à Préparer

```
Pour STV Player :
├─ APK : app-releaseProd-release.apk ✅
├─ Icône : 512x512 PNG
├─ Screenshots : 4+ images
├─ Description : "Lecteur vidéo IPTV universel"
├─ Permissions : Clarifier les usages
└─ Politique Confidentialité : Ajouter

Pour SoukiTV (si PlayStore) :
├─ APK : soukitv-release.apk ✅
├─ Icône : 512x512 PNG
├─ Screenshots : 4+ images
├─ Description : "Catalogue de chaînes - Requiert STV Player"
├─ Dépendances : Mentionner STV Player
└─ Politique Confidentialité : Ajouter
```

### Checklist PlayStore

```
BEFORE UPLOAD :

STV :
☐ APK release généré ✅
☐ Versioning correct (1.0, code 1)
☐ Signature en place
☐ AdMob IDs en place (tests)
☐ Permissions déclarées
☐ Intent Filters documentés
☐ Description claire
☐ Icône 512x512

SoukiTV :
☐ APK release généré ✅
☐ Versioning correct (1.0, code 1)
☐ Signature en place
☐ Permissions déclarées
☐ Description mentionne STV
☐ Dépendances claires
☐ Icône 512x512

AFTER UPLOAD :

☐ Attendre modération (2-24h)
☐ Vérifier reviews utilisateurs
☐ Monitor crashes
☐ Préparer Update 1.1 (features)
```

---

## 🚀 PROCESSUS DE RELEASE FINAL

### Timeline Recommandée

```
SEMAINE 1 :
├─ Day 1-2  : Tests sur device (tous les tests listés ci-dessus)
├─ Day 3    : Corriger les bugs trouvés
├─ Day 4-5  : Re-tester
└─ Day 6    : Build Release final + vérifications

SEMAINE 2 :
├─ Day 1    : Préparer PlayStore store page
├─ Day 2-3  : Upload APK + Assets
├─ Day 4    : Remplir toutes les infos
├─ Day 5    : Review final + Submit
└─ Day 6-7  : Attendre modération

SEMAINE 3 :
├─ Day 1-3  : Modération Google (2-24h typique)
├─ Day 4    : Apps LIVE sur PlayStore 🎉
└─ Day 5+   : Monitor + prepare updates
```

---

## 🆘 TROUBLESHOOTING

### Build Échoue : "JAVA_HOME not set"

```powershell
# Solution :
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
echo $env:JAVA_HOME

# Ou permanent dans PowerShell Profile :
# $profile : C:\Users\Lenovo\Documents\WindowsPowerShell\profile.ps1
```

### APK Installé mais Crash au Lancement

```powershell
# Vérifier les logs :
adb logcat -c  # Clear logs
adb logcat | Select-String "PlayerActivity" -SimpleMatch

# Chercher les exceptions :
adb logcat | Select-String "Exception" -SimpleMatch
```

### SoukiTV ne Détecte pas STV

```powershell
# Vérifier l'installation STV :
adb shell pm list packages | Select-String "stv"

# Expected output :
# package:com.example.stv

# Si absent, réinstaller :
adb install -r "C:\Users\Lenovo\StudioProjects\STV4\app\build\outputs\apk\releaseProd\app-releaseProd-release.apk"
```

---

## ✅ VALIDATION COMPLÈTE

Après tous les tests, vous devriez avoir :

```
✅ STV APK prêt pour PlayStore
├─ Fonctionne autonome
├─ Compatible avec liens vidéo
├─ Ads chargent correctement
└─ Pas de crashes

✅ SoukiTV APK prêt pour déploiement
├─ Détecte STV installée
├─ Force STV pour ses chaînes
├─ Affiche dialog si STV absent
└─ Pas de crashes

✅ Architecture validée
├─ Intent Filters multiples (STV)
├─ Intent Explicite (SoukiTV → STV)
├─ Conforme PlayStore
└─ Prête pour 10+ catalogues futurs
```

---

**🎉 APK Release Prêts pour Publication !**

