# 🎉 APKs RELEASE PRÊTS POUR TEST !

**Date :** 26 Février 2026  
**Build réussi :** ✅ Succès

---

## 📦 FICHIERS GÉNÉRÉS

### 1️⃣ STV Player (Production)
- **Fichier :** `app\build\outputs\apk\prod\release\app-prod-release.apk`
- **Taille :** 4.81 MB
- **Package Name :** `com.example.stv` (flavor prod)
- **Signature :** ✅ Signé avec release.keystore
- **ProGuard :** ✅ Activé (optimisé)

### 2️⃣ SoukiTV (Release)
- **Fichier :** `soukitv\build\outputs\apk\release\soukitv-release.apk`
- **Taille :** 2.21 MB
- **Package Name :** `com.example.soukitv`
- **Signature :** ✅ Signé avec release.keystore
- **ProGuard :** ✅ Activé (optimisé)

---

## 📲 INSTALLATION SUR VOTRE TÉLÉPHONE

### Méthode 1 : Via ADB (Câble USB)

#### Étape 1 : Connecter le téléphone
```powershell
# Activer "Débogage USB" dans Paramètres > Options développeur
# Connecter le téléphone en USB au PC

# Vérifier la connexion
adb devices
```

#### Étape 2 : Installer les APKs
```powershell
# Naviguer vers le projet
cd C:\Users\Lenovo\StudioProjects\STV4

# Installer STV Player
adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk

# Installer SoukiTV
adb install -r soukitv\build\outputs\apk\release\soukitv-release.apk
```

**Flag `-r` :** Réinstalle sans effacer les données (utile si versions précédentes installées)

---

### Méthode 2 : Copie Manuelle (Sans Câble)

#### Étape 1 : Copier les APKs sur le téléphone
```powershell
# Option A : Via Google Drive / Dropbox
# 1. Téléverser les APKs sur le cloud
# 2. Télécharger depuis le téléphone

# Option B : Via câble USB (mode transfert de fichiers)
# 1. Connecter le téléphone en USB
# 2. Copier les APKs dans le dossier "Download" du téléphone
```

#### Étape 2 : Installer depuis le téléphone
1. Ouvrir l'application **Fichiers** / **Mes fichiers**
2. Naviguer vers **Téléchargements**
3. Appuyer sur `app-prod-release.apk`
4. Accepter l'installation depuis sources inconnues (si demandé)
5. Répéter pour `soukitv-release.apk`

---

## ✅ TESTS À EFFECTUER

### Test 1 : STV Player Standalone
1. Ouvrir **STV Player**
2. Entrer une URL de test : `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`
3. Appuyer sur "Lire le stream"
4. **Vérifier :**
   - ✅ Publicité interstitielle AdMob s'affiche (ou bannière si No Fill)
   - ✅ Vidéo démarre après la pub
   - ✅ Contrôles fonctionnent (pause, play, qualité)
   - ✅ Picture-in-Picture fonctionne (bouton en haut à droite)

---

### Test 2 : Intégration SoukiTV → STV
1. Ouvrir **SoukiTV**
2. Cliquer sur une chaîne (ex: "NASA TV" ou "Big Buck Bunny")
3. **Vérifier :**
   - ✅ Dialogue "Installer STV Player" apparaît si STV non installé
   - ✅ Si STV installé, la vidéo s'ouvre automatiquement dans STV
   - ✅ Publicité affichée dans STV
   - ✅ Vidéo démarre correctement

---

### Test 3 : Gestion des Erreurs
1. Dans STV, entrer une URL invalide : `https://invalid.url`
2. **Vérifier :**
   - ✅ Message d'erreur clair ("URL invalide" ou similaire)
   - ✅ Pas de crash

---

### Test 4 : AdMob (Production)
⚠️ **IMPORTANT :** Les APKs utilisent actuellement les **IDs de test AdMob**.

Pour activer les **vraies publicités** :
1. Créer un compte AdMob : https://admob.google.com/
2. Créer des unités publicitaires (Interstitiel + Bannière)
3. Remplacer les IDs dans `app/build.gradle.kts` (flavor prod)
4. Rebuild : `.\gradlew :app:assembleProdRelease`

**IDs actuels (Test) :**
- Interstitiel : `ca-app-pub-3940256099942544/1033173712`
- Bannière : `ca-app-pub-3940256099942544/6300978111`

---

## 🐛 PROBLÈMES CONNUS

### ⚠️ Problème #1 : SoukiTV ne peut pas lancer STV
**Symptôme :** Clic sur une chaîne dans SoukiTV → rien ne se passe

**Cause :** Protection `signature-level` active dans STV

**Solution temporaire :**
→ Voir le fichier `PLAN_IMPLEMENTATION_URGENT.md` pour implémenter le Deep Link

---

### ⚠️ Problème #2 : "App non installée" lors de l'installation
**Symptôme :** Installation échoue avec "App non installée"

**Cause :** Version précédente avec signature différente

**Solution :**
```powershell
# Désinstaller les anciennes versions
adb uninstall com.example.stv
adb uninstall com.example.stv.dev
adb uninstall com.example.stv.prod
adb uninstall com.example.soukitv

# Réinstaller
adb install app\build\outputs\apk\prod\release\app-prod-release.apk
adb install soukitv\build\outputs\apk\release\soukitv-release.apk
```

---

## 📊 INFORMATIONS TECHNIQUES

### STV Player
- **MinSDK :** Android 7.0 (API 24)
- **TargetSDK :** Android 15 (API 35)
- **Permissions :**
  - `INTERNET` (streaming vidéo)
  - `ACCESS_NETWORK_STATE` (vérifier connexion)
- **Features :**
  - Media3 ExoPlayer
  - AdMob (Interstitiel + Bannière)
  - Picture-in-Picture
  - Sélection qualité vidéo
  - Buffer optimisé (démarrage rapide)

### SoukiTV
- **MinSDK :** Android 7.0 (API 24)
- **TargetSDK :** Android 15 (API 35)
- **Permissions :**
  - `INTERNET` (catalogue dynamique futur)
  - `QUERY_ALL_PACKAGES` (détection STV installé)
- **Features :**
  - Catalogue de chaînes IPTV
  - Détection STV installé
  - Redirection Play Store (si implémenté)

---

## 🚀 PROCHAINES ÉTAPES

### Étape 1 : Tests sur Téléphone ✅ (EN COURS)
→ Installer les APKs et tester tous les scénarios

### Étape 2 : Corriger les Problèmes Critiques
→ Implémenter Deep Link (voir `PLAN_IMPLEMENTATION_URGENT.md`)

### Étape 3 : Préparer Publication Play Store
- Remplacer IDs AdMob Test par IDs Production
- Créer assets graphiques (icônes, screenshots)
- Rédiger description
- Créer compte développeur Play Store (25$ one-time fee)

### Étape 4 : Publication Beta
- Upload STV en beta fermée (Internal Testing)
- Inviter testeurs
- Itérer selon retours

---

## 📞 RETOUR DE TESTS

Après avoir testé sur votre téléphone, noter :

✅ **Ce qui fonctionne :**
- [ ] STV ouvre et lit une vidéo
- [ ] Publicités s'affichent
- [ ] Contrôles (play/pause/qualité) fonctionnent
- [ ] Picture-in-Picture fonctionne
- [ ] SoukiTV affiche le catalogue

❌ **Ce qui ne fonctionne pas :**
- [ ] SoukiTV ne peut pas ouvrir STV (attendu si Deep Link non implémenté)
- [ ] Crash au lancement
- [ ] Publicités ne s'affichent pas
- [ ] Vidéo ne démarre pas
- [ ] Autre : _________________

---

## 🔧 COMMANDES UTILES

### Vérifier les logs en temps réel (pendant test)
```powershell
# Connecter le téléphone en USB
adb logcat | Select-String "stv"
```

### Forcer l'arrêt d'une app (si bloquée)
```powershell
adb shell am force-stop com.example.stv
adb shell am force-stop com.example.soukitv
```

### Capturer un screenshot du téléphone
```powershell
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png C:\Users\Lenovo\Desktop\screenshot.png
```

---

**Auteur :** GitHub Copilot  
**Build ID :** 2026-02-26_Release  
**Status :** ✅ PRÊT POUR TEST

