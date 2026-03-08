# Résumé - Tests STV Player APK Release

**Date** : 27 février 2026  
**Version** : 1.0  
**APK** : `app/build/outputs/apk/prod/release/app-prod-release.apk`

---

## 📦 Fichiers créés pour les tests

### 1. `PLAN_TESTS_STV_RELEASE.md` ✅
**Plan de tests complet avec 15 scénarios détaillés**
- Installation et premier lancement
- Navigation et ajout de vidéos
- Suppression de vidéos
- Lecture vidéo et player
- Mode Picture-in-Picture
- Menu drawer
- Validation d'URL
- Recherche
- Deep links
- Gestion réseau
- Publicités AdMob
- Rotation écran
- Sécurité
- Tests de régression

Chaque test contient :
- Objectif clair
- Étapes détaillées
- Résultat attendu
- Colonne pour noter ✅/❌

### 2. `test_apk.ps1` ✅
**Script PowerShell d'installation automatique**
- Vérifie qu'un appareil Android est connecté
- Désinstalle l'ancienne version
- Installe la nouvelle APK
- Lance l'application
- Affiche les logs en temps réel

**Usage** :
```powershell
.\test_apk.ps1
```

### 3. `TEST_RAPIDE_STV.md` ✅
**Guide de test rapide (5 minutes)**
- 5 tests essentiels pour validation rapide
- Commandes ADB utiles
- Checklist avant publication Play Store

---

## 🚀 Comment tester l'APK

### Méthode 1 : Script automatique (recommandé)
```powershell
cd C:\Users\soufi\AndroidStudioProjects\STV5
.\test_apk.ps1
```

### Méthode 2 : Installation manuelle
```powershell
# Connecter un appareil Android via USB
adb devices

# Installer l'APK
adb install -r "app\build\outputs\apk\prod\release\app-prod-release.apk"

# Lancer l'app
adb shell am start -n com.example.stv/.MainActivity
```

### Méthode 3 : Sur téléphone sans PC
1. Copier `app-prod-release.apk` sur le téléphone
2. Ouvrir avec le gestionnaire de fichiers
3. Installer (autoriser sources inconnues si nécessaire)

---

## ✅ Tests critiques à valider

### Test 1 : Installation
- [ ] APK s'installe sans erreur
- [ ] App se lance correctement
- [ ] Splash screen puis accueil

### Test 2 : Ajout vidéo depuis accueil
- [ ] Bouton "+" fonctionne
- [ ] Page "Ajouter un flux" s'affiche
- [ ] Saisir titre + URL valide
- [ ] Cliquer "Save" → retour accueil
- [ ] Aller dans "Mes vidéos"
- [ ] **La vidéo apparaît dans la liste**

### Test 3 : Ajout vidéo depuis liste (test navigation corrigée)
- [ ] Depuis "Mes vidéos", cliquer "+"
- [ ] Ajouter une vidéo
- [ ] Cliquer "Save"
- [ ] **Retour DIRECT à la liste (pas l'accueil)**
- [ ] **La vidéo apparaît IMMÉDIATEMENT**

### Test 4 : Lecture vidéo
- [ ] Cliquer sur "Big Buck Bunny"
- [ ] Pub ou fallback (5s)
- [ ] Player démarre et lit la vidéo
- [ ] Contrôles fonctionnent (play/pause)
- [ ] Bouton retour ramène à la liste

### Test 5 : Suppression
- [ ] Icône delete visible sur vidéos ajoutées
- [ ] Cliquer delete → vidéo disparaît
- [ ] Pas d'icône delete sur "Big Buck Bunny"

---

## ⚠️ Points d'attention spécifiques

### Navigation (corrections appliquées)
✅ **Correction validée dans le code** :
- `AddVideoActivity` fait maintenant `finish()` au lieu de forcer `MainActivity`
- `VideoListActivity` recharge la liste dans `onResume()`

**À vérifier pendant le test** :
- Quand on ajoute une vidéo depuis la liste, on doit revenir à la liste (pas l'accueil)
- La nouvelle vidéo doit apparaître immédiatement sans action manuelle

### APIs modernes (corrections appliquées)
✅ **Corrections validées** :
- Icons AutoMirrored pour support RTL (arabe/hébreu)
- NetworkInfo déprécié supprimé

**À tester (optionnel)** :
- Changer la langue système en arabe → flèches se retournent

### Publicités
⚠️ **IDs test AdMob actifs** :
- Normal de voir des pubs "test"
- Peuvent échouer → fallback bannière 5s
- **À REMPLACER** avant publication Play Store

---

## 📋 Checklist avant publication Play Store

- [ ] Tous les tests critiques (1-5) passent
- [ ] Aucun crash détecté
- [ ] Navigation cohérente (test 3 crucial)
- [ ] Liste mise à jour automatiquement (test 3)
- [ ] Player lit les vidéos (test 4)
- [ ] Tests sur Android 7, 10, 13+
- [ ] Tests sur différentes tailles d'écran
- [ ] **⚠️ REMPLACER IDs AdMob test par IDs production**
- [ ] Sauvegarder `release.keystore` en sécurité

---

## 🔧 Commandes utiles

### Voir les logs pendant les tests
```powershell
adb logcat -s "STV:*" "PlayerActivity:*" "VideoListActivity:*" "AdManager:*"
```

### Désinstaller l'app
```powershell
adb uninstall com.example.stv
```

### Vérifier la signature
```powershell
& "C:\Program Files\JetBrains\AndroidStudio\jbr\bin\keytool.exe" -printcert -jarfile "app\build\outputs\apk\prod\release\app-prod-release.apk"
```

### Capturer un screenshot
```powershell
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png screenshot.png
```

---

## 📊 Résultats attendus

### Si tous les tests passent
✅ **APK validé pour publication** (après remplacement IDs AdMob)

### Si tests critiques échouent
❌ **APK à corriger** avant publication

### Bugs prioritaires à surveiller
1. **Navigation** : Retour depuis AddVideoActivity doit aller à la liste
2. **Refresh** : Liste doit se mettre à jour automatiquement
3. **Crash** : Aucun crash toléré sur tests critiques

---

## 📞 Support

**Fichiers de référence** :
- `PLAN_TESTS_STV_RELEASE.md` - Tests détaillés (15 scénarios)
- `TEST_RAPIDE_STV.md` - Tests rapides (5 minutes)
- `RAPPORT_VERIFICATION_STV_2026-02-27.md` - Audit complet
- `test_apk.ps1` - Script d'installation

**Corrections appliquées dans cette version** :
1. ✅ Keystore sécurisé
2. ✅ PlayerActivity sécurisé
3. ✅ Navigation cohérente (finish au lieu de forcer MainActivity)
4. ✅ Liste mise à jour automatiquement (onResume + refreshVideos)
5. ✅ APIs modernes (AutoMirrored icons, pas de NetworkInfo déprécié)

---

**Prêt pour les tests ! 🚀**

