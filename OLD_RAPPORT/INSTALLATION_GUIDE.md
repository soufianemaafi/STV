# 📱 INSTALLATION SUR XIAOMI — GUIDE COMPLET

## ✅ APKs Compilés

Les deux APKs release sont prêts :

```
1️⃣ STV Player (app principale)
   📁 C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk
   📦 Package: com.example.stv
   🎯 Flavor: prod + release

2️⃣ SoukiTV (catalogue de chaînes)
   📁 C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk
   📦 Package: com.example.soukitv
   🎯 BuildType: release
```

---

## 📲 MÉTHODE 1 : Installation via ADB (Recommandé)

### Prérequis
1. **Activer le débogage USB sur Xiaomi** :
   - Paramètres → À propos du téléphone
   - Appuyer 7 fois sur "Version MIUI" pour activer le mode développeur
   - Paramètres → Paramètres supplémentaires → Options pour les développeurs
   - Activer "Débogage USB"
   - Connecter le téléphone au PC via USB

2. **Vérifier la connexion** :
   ```powershell
   adb devices
   ```
   Résultat attendu : votre appareil listé

### Installation

```powershell
# 1️⃣ Installer STV Player (installer EN PREMIER)
adb install -r "C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk"

# 2️⃣ Installer SoukiTV (installer EN SECOND)
adb install -r "C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk"
```

**Options ADB** :
- `-r` : Réinstaller (remplacer si déjà installé)
- `-d` : Forcer l'installation même si la version est plus ancienne

---

## 📲 MÉTHODE 2 : Installation manuelle (Transfert fichier)

### Étapes

1. **Copier les APKs sur le téléphone** :
   - Connecter le Xiaomi au PC via USB
   - Copier les 2 APKs dans un dossier du téléphone (ex: Downloads)

2. **Installer depuis le téléphone** :
   - Ouvrir l'explorateur de fichiers
   - Naviguer vers les APKs
   - Appuyer sur chaque APK pour installer
   - **⚠️ MIUI Security** : Autoriser "Installer des apps inconnues" si demandé

3. **Ordre d'installation** :
   - ✅ Installer **STV Player** EN PREMIER
   - ✅ Installer **SoukiTV** EN SECOND

---

## ⚙️ MIUI : Désactiver l'optimisation des apps

Xiaomi/MIUI peut tuer les apps en arrière-plan. Pour éviter cela :

1. **Paramètres** → **Batterie et performances**
2. **Paramètres d'économie de batterie**
3. Trouver "STV" et "SoukiTV"
4. Sélectionner **"Aucune restriction"**

---

## 🧪 TESTER L'INSTALLATION

### Test 1 : STV Player seul
```
1. Ouvrir STV Player
2. Entrer une URL de test (ex: https://stream.skynewsarabia.com/hls/sna_720.m3u8)
3. Cliquer "Play"
4. ✅ La publicité test doit s'afficher
5. ✅ La vidéo doit se lancer après la pub
```

### Test 2 : SoukiTV → STV Player
```
1. Ouvrir SoukiTV
2. Sélectionner une catégorie (ex: News)
3. Cliquer sur une chaîne (ex: Sky News Arabia)
4. ✅ STV Player doit s'ouvrir AUTOMATIQUEMENT
5. ✅ Pas de dialogue "STV Required"
6. ✅ La publicité test doit s'afficher
7. ✅ La vidéo doit se lancer
```

---

## ⚠️ RÉSOLUTION DES PROBLÈMES

### Problème : "App not installed"
**Cause** : Signature différente d'une version déjà installée  
**Solution** :
```powershell
# Désinstaller les anciennes versions
adb uninstall com.example.stv
adb uninstall com.example.soukitv

# Réinstaller
adb install "C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk"
adb install "C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk"
```

### Problème : "Unauthorized" sur ADB
**Solution** :
1. Déconnecter/reconnecter le câble USB
2. Sur le téléphone, accepter "Autoriser le débogage USB"
3. Cocher "Toujours autoriser depuis cet ordinateur"

### Problème : SoukiTV affiche "STV Required"
**Cause** : STV Player non installé ou installé avec un package différent  
**Solution** :
- Vérifier que STV Player est installé : `adb shell pm list packages | findstr stv`
- Réinstaller STV Player EN PREMIER

### Problème : Les publicités ne s'affichent pas
**Cause** : IDs test AdMob + connexion Internet  
**Solution** :
- Vérifier la connexion Internet
- Les IDs test doivent afficher des pubs de test (bannières/interstitiels)

---

## 📝 COMMANDES UTILES

```powershell
# Vérifier les apps installées
adb shell pm list packages | findstr example

# Lancer STV Player
adb shell am start -n com.example.stv/.MainActivity

# Lancer SoukiTV
adb shell am start -n com.example.soukitv/.MainActivity

# Voir les logs en temps réel
adb logcat | findstr "STV"

# Désinstaller
adb uninstall com.example.stv
adb uninstall com.example.soukitv
```

---

## ✅ CHECKLIST PRÉ-INSTALLATION

- [ ] Débogage USB activé sur Xiaomi
- [ ] Câble USB connecté
- [ ] `adb devices` affiche le téléphone
- [ ] Anciennes versions désinstallées (si nécessaire)
- [ ] Les 2 APKs sont prêts

---

## 🚀 INSTALLATION RAPIDE (Commandes complètes)

Copiez-collez ces commandes dans PowerShell :

```powershell
# Vérifier la connexion
adb devices

# Désinstaller les anciennes versions (si nécessaire)
adb uninstall com.example.stv
adb uninstall com.example.soukitv

# Installer les apps
adb install -r "C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk"
adb install -r "C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk"

# Lancer SoukiTV pour tester
adb shell am start -n com.example.soukitv/.MainActivity
```

---

**Bon test ! 📱✨**


