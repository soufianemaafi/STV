# ✅ CHECKLIST FINALE - Avant Publication PlayStore

**Date** : 26/02/2026  
**Type** : Checklist Complète de Validation  
**Status** : À Utiliser Avant Upload

---

## 📋 CHECKLIST ARCHITECTURE

### STV Player Configuration

```
INTENTFILTERS :
☐ Custom Action (com.example.stv.action.PLAY_STREAM) présente
☐ Deep Link (stv://play?url=...) présente
☐ HTTP/HTTPS présente
☐ Video/* MIME type présente
☐ m3u8/mpegurl MIME type présente

MANIFEST PERMISSIONS :
☐ INTERNET déclarée
☐ ACCESS_NETWORK_STATE déclarée
☐ FOREGROUND_SERVICE (optionnel)

ACTIVITIES :
☐ PlayerActivity exportée et correcte
☐ MainActivity présente
☐ VideoListActivity présente
☐ launchMode="singleTask" sur PlayerActivity

CONFIGURATION :
☐ screenOrientation="sensorLandscape" sur PlayerActivity
☐ supportsPictureInPicture="true" sur PlayerActivity
☐ NetworkSecurityConfig présent
☐ usesCleartextTraffic="true" pour HTTP
```

### SoukiTV Configuration

```
INTENT HANDLING :
☐ Intent("com.example.stv.action.PLAY_STREAM") utilisé
☐ setPackage("com.example.stv") appliqué
☐ putExtra("VIDEO_URL", streamUrl) présent
☐ FLAG_ACTIVITY_NEW_TASK ajouté

MANIFEST PERMISSIONS :
☐ INTERNET déclarée
☐ ACCESS_NETWORK_STATE déclarée
☐ QUERY_PACKAGE (pour détecter STV) déclaré

QUERIES ELEMENT :
☐ <queries> contient "com.example.stv"
☐ <queries> contient "com.example.stv.dev"
☐ <queries> contient "com.example.stv.prod"

ERROR HANDLING :
☐ Dialog "Install STV" si pas installée
☐ Redirection PlayStore implementée
☐ Try-catch autour startActivity
☐ Toast sur erreur
```

---

## 🧪 CHECKLIST TESTS

### Test 1 : STV Autonomous

```
SETUP :
☐ STV installée
☐ SoukiTV DÉSINSTALLÉE
☐ Téléphone connecté ADB

EXECUTION :
☐ Ouvrir Chrome
☐ Chercher "test m3u8"
☐ Ouvrir lien vidéo
☐ Chooser Android affiché
☐ STV visible dans options
☐ Cliquer STV
☐ Vidéo joue

VALIDATION :
☐ Pas de crash
☐ Pas d'erreur de permissions
☐ Ads chargent (test ad)
☐ PiP fonctionne (si testé)

STATUS: ☐ PASSÉ  ☐ ÉCHOUÉ
```

### Test 2 : SoukiTV Force STV

```
SETUP :
☐ STV installée
☐ SoukiTV installée
☐ Téléphone connecté ADB

EXECUTION :
☐ Ouvrir SoukiTV
☐ Clique sur une chaîne
☐ Observer : PAS de chooser
☐ STV ouvre directement
☐ Vidéo joue

VALIDATION :
☐ Pas de crash
☐ Intent Explicite fonctionne
☐ Pas d'alternatives affichées
☐ Vidéo joue immédiatement

STATUS: ☐ PASSÉ  ☐ ÉCHOUÉ
```

### Test 3 : Coexistence VLC

```
SETUP :
☐ STV installée
☐ VLC installée (PlayStore)
☐ SoukiTV installée
☐ Téléphone connecté ADB

EXECUTION :
☐ Ouvrir Chrome
☐ Lien vidéo
☐ Chooser Android affiché
☐ Vérifier : STV visible ✅
☐ Vérifier : VLC visible ✅
☐ User libre de choisir

VALIDATION :
☐ Pas de monopole
☐ User freedom respectée
☐ Pas de forçage abusif

STATUS: ☐ PASSÉ  ☐ ÉCHOUÉ
```

### Test 4 : SoukiTV Sans STV

```
SETUP :
☐ STV DÉSINSTALLÉE
☐ SoukiTV installée
☐ Téléphone connecté ADB

EXECUTION :
☐ Ouvrir SoukiTV
☐ Clique chaîne
☐ Dialog "Install STV" affiché
☐ Clique "Install"
☐ PlayStore s'ouvre
☐ STV visible pour install
☐ Clique back → SoukiTV OK

VALIDATION :
☐ Dialog correct
☐ Pas de crash
☐ Erreur gracieux handling
☐ User peut refuser

STATUS: ☐ PASSÉ  ☐ ÉCHOUÉ
```

### Test 5 : Picture-in-Picture

```
SETUP :
☐ STV installée
☐ Vidéo en lecture

EXECUTION :
☐ Clique bouton PiP
☐ Appui home button
☐ Vidéo continue en PiP
☐ Audio continue
☐ Clique PiP → STV retour

VALIDATION :
☐ PiP fonctionne
☐ Pas de crash
☐ Pas de lag
☐ Audio continu

STATUS: ☐ PASSÉ  ☐ ÉCHOUÉ
```

### Test 6 : AdMob Ads

```
SETUP :
☐ STV installée
☐ Internet actif
☐ Données mobiles ON

EXECUTION :
☐ Ouvrir STV
☐ Attendre chargement
☐ Banner pub visible (test ad)
☐ Attendre 5s
☐ Vidéo joue
☐ Pas de pub interstitielle

VALIDATION :
☐ Ads chargent
☐ Pas de crash
☐ Pas de ANR (Application Not Responding)
☐ AdMob SDK fonctionne

STATUS: ☐ PASSÉ  ☐ ÉCHOUÉ

RÉSULTAT FINAL TEST : ☐ 6/6 PASSÉ
```

---

## 🏗️ CHECKLIST BUILD

### Avant Build

```
JAVA_HOME :
☐ Défini correctement
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
☐ Gradle version 8.x+
☐ Android SDK level 35
☐ Kotlin 1.9.x+

CONFIGURATIONS :
☐ app/build.gradle.kts
   ├─ namespace = "com.example.stv"
   ├─ applicationId = "com.example.stv"
   ├─ versionCode = 1
   ├─ versionName = "1.0"
   ├─ compileSdk = 35
   ├─ targetSdk = 35
   ├─ minSdk = 24
   └─ signingConfig = release

☐ soukitv/build.gradle.kts
   ├─ namespace = "com.example.soukitv"
   ├─ applicationId = "com.example.soukitv"
   ├─ versionCode = 1
   ├─ versionName = "1.0"
   └─ signingConfig = release

KEYSTORE :
☐ release.keystore présent
☐ Password : android
☐ Key alias : key0
☐ Key password : android
```

### Build STV

```
COMMAND :
./gradlew.bat clean
./gradlew.bat assembleReleaseProd

RÉSULTAT EXPECTED :
☐ BUILD SUCCESSFUL
☐ APK généré : app/build/outputs/apk/releaseProd/app-releaseProd-release.apk
☐ Taille : ~50-80 MB
☐ Signé avec release keystore

VÉRIFICATION :
☐ Aucune warning
☐ Aucune erreur
☐ APK listable : aapt dump permissions [APK]
☐ Manifest complet
```

### Build SoukiTV

```
COMMAND :
./gradlew.bat :soukitv:assembleRelease

RÉSULTAT EXPECTED :
☐ BUILD SUCCESSFUL
☐ APK généré : soukitv/build/outputs/apk/release/soukitv-release.apk
☐ Taille : ~30-50 MB
☐ Signé avec release keystore

VÉRIFICATION :
☐ Aucune warning
☐ Aucune erreur
☐ APK listable
☐ Manifest complet
☐ Queries correct
```

---

## 📱 CHECKLIST INSTALLATION

### Installer sur Téléphone

```
PRÉPARATION :
☐ Téléphone branché USB
☐ USB Debug ON
☐ ADB reconnaît device
   adb devices → [Device ID] device

INSTALLATION :
☐ adb install -r [STV APK] → Success
☐ adb install -r [SoukiTV APK] → Success
☐ Apps visibles dans Play Store (Mes Apps)

VÉRIFICATION :
☐ STV accessible dans launcher
☐ SoukiTV accessible dans launcher
☐ Pas de crash au lancement
☐ Permissions OK
```

---

## 📊 CHECKLIST ASSETS PLAYSTORE

### Icônes

```
STV PLAYER ICON :
☐ Fichier : 512x512 PNG
☐ Format : RGBA (pas ICC profile)
☐ Taille fichier : < 1 MB
☐ Logo/Icône clair et visible
☐ Pas de texte
☐ Nom : stv_icon.png ou similar

SoukiTV ICON :
☐ Fichier : 512x512 PNG
☐ Format : RGBA
☐ Taille fichier : < 1 MB
☐ Logo distinct de STV
☐ Nom : soukitv_icon.png ou similar
```

### Screenshots

```
POUR STV (4-5 images) :
☐ Screenshot 1 : Vidéo en lecture
☐ Screenshot 2 : Contrôles visibles
☐ Screenshot 3 : Quality selector
☐ Screenshot 4 : PiP mode
☐ Screenshot 5 (optionnel) : Settings

POUR SoukiTV (4-5 images) :
☐ Screenshot 1 : Écran accueil with chaînes
☐ Screenshot 2 : Liste complète chaînes
☐ Screenshot 3 : Chaîne en lecture (STV)
☐ Screenshot 4 : Install dialog (optionnel)
☐ Screenshot 5 (optionnel) : Settings

FORMAT :
☐ Résolution : 1080x1920 (ou même aspect ratio)
☐ Format : PNG ou JPG
☐ Taille : < 2 MB par image
☐ Pas de floutage
☐ Texte lisible
```

### Descriptions

```
STV PLAYER DESCRIPTION :

TITRE :
"STV Player - Lecteur Vidéo IPTV"

DESCRIPTION COURTE (80 caractères max) :
"Lecteur vidéo pour flux IPTV et vidéos internet"

DESCRIPTION LONGUE (4000 caractères max) :
"STV Player est un lecteur vidéo polyvalent pour :
- Flux IPTV
- Vidéos internet (HTTP/HTTPS)
- M3U8 playlists
- MP4, MKV, et autres formats vidéo

Caractéristiques :
✓ Interface intuitive
✓ Picture-in-Picture (PiP) mode
✓ Sélection de qualité vidéo
✓ Compatible avec d'autres apps catalogues
✓ Support complet des gestes

Utilisation :
1. Lancer STV directement
2. Ouvrir lien vidéo dans navigateur
3. Sélectionner STV en tant que lecteur
4. Utiliser avec apps catalogues (SoukiTV, etc.)

Permissions :
- INTERNET : pour streamer vidéos
- ACCESS_NETWORK_STATE : pour qualité vidéo adaptée

Note : Publicités supportées (AdMob)
"

CATÉGORIE : Video Player

CONTENU / RATING :
Everyone (ou Teen si nécessaire)

TYPE CONTENU :
Libre (non limité)
```

```
SoukiTV DESCRIPTION :

TITRE :
"SoukiTV - Catalogue de Chaînes"

DESCRIPTION COURTE :
"Accédez à votre catalogue de chaînes"

DESCRIPTION LONGUE :
"SoukiTV est un catalogue de chaînes vidéo qui requiert 
STV Player pour la lecture vidéo.

Fonctionnalités :
✓ Accès instant aux chaînes
✓ Interface intuitive
✓ Lecture avec STV Player (optimisé)
✓ Support complet des formats

Prérequis :
STV Player (gratuit, disponible sur PlayStore)

L'application STV Player doit être installée pour 
utiliser SoukiTV. Si manquante, l'app vous guidera 
vers l'installation.

Permissions :
- INTERNET : pour charger catalogue et streams
- QUERY_PACKAGES : pour détecter STV Player

Note : Application compagnon de STV Player
"

CATÉGORIE : Entertainment

CONTENU / RATING :
Everyone

TYPE CONTENU :
Libre (non limité)
```

---

## 🔐 CHECKLIST CONFORMITÉ

### Permissions & Policies

```
STV PERMISSIONS :
☐ INTERNET → Clear (pour streaming)
☐ ACCESS_NETWORK_STATE → Clear (pour adaptative bitrate)

SoukiTV PERMISSIONS :
☐ INTERNET → Clear (pour chargement)
☐ QUERY_PACKAGES → Clear (pour détection STV)

POLICIES :
☐ Politique Confidentialité rédigée
☐ Conditions d'Utilisation présentes
☐ Support contact listé
☐ Déclaration données collectées
☐ Pas de malware/spyware déclaré
```

### Legal & Compliance

```
PLAYSTOIRE COMPLIANCE :
☐ Pas de modification système
☐ Pas de désactivation d'autres apps
☐ Pas de forçage abusif
☐ Transparence sur dépendances
☐ Respect de la vie privée

ADBLOCK HANDLING :
☐ STV peut détecter adblock
☐ Dialogue affiché si détecté
☐ App fonctionne normalement (pas de blocage total)

CONTENT POLICY :
☐ Pas de contenu adulte
☐ Pas de contenu violent
☐ Pas de contenu haineux
☐ Pas de contenu discriminatoire
```

---

## 🚀 CHECKLIST PRÉ-UPLOAD PLAYSTORE

### Préparation PlayStore Console

```
GOOGLE PLAY CONSOLE :
☐ Compte Google Play développeur actif
☐ $25 frais de développeur payés
☐ Identité vérifiée
☐ Compte bancaire lié (si monétisation)

CRÉATION APP STV :
☐ Nom : "STV Player"
☐ Package : "com.example.stv"
☐ Catégorie : "Video Player"
☐ Store listing complet
☐ Rating content declaration rempli
☐ Privacy policy URL fournie

CRÉATION APP SoukiTV :
☐ Nom : "SoukiTV"
☐ Package : "com.example.soukitv"
☐ Catégorie : "Entertainment"
☐ Store listing complet
☐ Dépendance STV mentionnée
☐ Privacy policy URL fournie
```

### Upload APK

```
POUR STV :
☐ Sélectionner "Production" track
☐ Upload APK : app-releaseProd-release.apk
☐ Review APK (Google Play vérifie automatiquement)
☐ Version name : "1.0"
☐ Version code : "1"
☐ Release notes : "Initial release"

POUR SoukiTV :
☐ Sélectionner "Production" track
☐ Upload APK : soukitv-release.apk
☐ Review APK
☐ Version name : "1.0"
☐ Version code : "1"
☐ Release notes : "Initial release"
```

### Avant Submit

```
VÉRIFICATION FINALE :
☐ Tous les champs remplis
☐ Screenshots visibles
☐ Icônes correctes
☐ Descriptions sans erreurs
☐ Pas de liens cassés
☐ Support email valide
☐ Website (optionnel) valide
☐ Privacy policy URL valide
☐ Politique confidentialité complète
☐ Contact de support présent

CONTENU APPS :
☐ STV : Fonctionnalités listées
☐ SoukiTV : Dépendance STV claire
☐ Pas d'erreurs spelling
☐ Pas de promesses non tenues
☐ Pas de contenu politique
☐ Pas de contenu religieux offensant
```

---

## 📋 CHECKLIST POST-PUBLICATION

### Après Upload à PlayStore

```
IMMÉDIATEMENT :
☐ Apps en attente de modération
☐ Email de confirmation reçu
☐ Status : "Pending Review" visible
☐ Timeline modération visible (~2-24h)

PENDANT MODÉRATION (2-24h) :
☐ Vérifier status toutes les heures
☐ Vérifier emails pour rejet (unlikely)
☐ Préparer réponses aux questions (si posées)
☐ Monitorer reviews utilisateurs

APRÈS APPROBATION :
☐ Status : "Live" visible
☐ Apps downloadable depuis PlayStore
☐ Analytics commencent à s'afficher
☐ Vérifier 5+ downloads
☐ Vérifier 5+ reviews positives

PROBLÈMES POTENTIELS :
☐ Si rejeté : lire raison, corriger, re-submit
☐ Si crash rapporté : fix immédiat, Update v1.1
☐ Si problème permissions : re-test, update
☐ Si notes basses : analyser, améliorer v1.1
```

### Monitoring Continu

```
SEMAINE 1 APRÈS PUBLICATION :
☐ 10+ downloads pour chaque app
☐ 5+ reviews pour chaque app
☐ Pas de crash crashes reports
☐ Pas de bug reports majeurs
☐ Rating > 3.0 stars (initial)
☐ Pas d'issues légales

MOIS 1 APRÈS PUBLICATION :
☐ 100+ downloads (STV)
☐ 50+ downloads (SoukiTV)
☐ Rating stable > 3.5 stars
☐ 0 crash reports en production
☐ Feedback positif
☐ Préparer v1.1 avec améliorations
```

---

## ✅ SIGNOFF FINAL

### Avant de cliquer "Publish"

```
CONFIRMEZ-VOUS :

☐ Architecture validée
☐ Code testé sur téléphone
☐ 6/6 tests passés
☐ Tous les assets prêts
☐ Descriptions sans erreurs
☐ Conformité PlayStore vérifiée
☐ Pas de violations détectées
☐ Team informée

SI OUI POUR TOUS :
→ Vous pouvez publier ! 🚀

SI NON SUR UN POINT :
→ Corriger avant publication
→ Ne pas cliquer "Publish" sinon
```

---

**✅ CHECKLIST COMPLÈTE - PRÊT POUR PUBLICATION**

_Utilisez cette checklist avant upload PlayStore_  
_Cochez chaque case avant de procéder_  
_Ne skippez PAS les étapes_

---

**🚀 Bonne chance pour la publication !**

