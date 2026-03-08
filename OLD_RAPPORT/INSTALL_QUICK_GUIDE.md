# 📱 INSTALLATION RAPIDE — 3 MÉTHODES

## ✅ LES APKs SONT PRÊTS ICI :

```
📦 STV Player (À installer EN PREMIER)
C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk

📦 SoukiTV (À installer EN SECOND)
C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk
```

---

## 🚀 MÉTHODE 1 : SCRIPT AUTOMATIQUE (Plus facile)

1. **Connecter le Xiaomi au PC via USB**
2. **Activer le débogage USB** :
   - Paramètres → À propos du téléphone
   - Appuyer 7 fois sur "Version MIUI"
   - Paramètres → Paramètres supplémentaires → Options développeurs
   - Activer "Débogage USB"
   - Autoriser sur le téléphone quand demandé

3. **Exécuter le script** :
   - Ouvrir PowerShell en tant qu'Administrateur
   - Copier-coller cette commande :
   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
   cd C:\Users\Lenovo\AndroidStudioProjects\STV1
   .\install_apps.ps1
   ```

---

## 📲 MÉTHODE 2 : INSTALLATION MANUELLE (Sans câble USB)

### Option A : Via Bluetooth / Partage de fichiers

1. **Envoyer les 2 APKs au téléphone** (Bluetooth, Email, Drive, etc.)
2. **Sur le Xiaomi** :
   - Paramètres → Paramètres supplémentaires → Confidentialité
   - Activer "Installer des apps provenant de sources inconnues"
3. **Ouvrir l'explorateur de fichiers**
4. **Trouver les APKs** (dossier Downloads ou Bluetooth)
5. **Installer dans l'ordre** :
   - ✅ Appuyer sur `app-prod-release.apk` → Installer
   - ✅ Appuyer sur `soukitv-release.apk` → Installer

### Option B : Via câble USB (transfert simple)

1. **Connecter le Xiaomi au PC via USB**
2. **Sur le téléphone** : Sélectionner "Transfert de fichiers" (MTP)
3. **Copier les 2 APKs** vers `Stockage interne/Download/`
4. **Débrancher le téléphone**
5. **Ouvrir l'explorateur de fichiers** → Downloads
6. **Installer les APKs** dans l'ordre

---

## 🔧 MÉTHODE 3 : COMMANDES ADB MANUELLES

Si ADB est installé :

```powershell
# 1. Vérifier la connexion
adb devices

# 2. Désinstaller les anciennes versions (optionnel)
adb uninstall com.example.stv
adb uninstall com.example.soukitv

# 3. Installer STV Player
adb install -r "C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk"

# 4. Installer SoukiTV
adb install -r "C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk"

# 5. Lancer SoukiTV
adb shell am start -n com.example.soukitv/.MainActivity
```

---

## ⚠️ IMPORTANT : ORDRE D'INSTALLATION

**TOUJOURS installer dans cet ordre :**
1. ✅ STV Player EN PREMIER
2. ✅ SoukiTV EN SECOND

Sinon SoukiTV affichera "STV Required".

---

## 🧪 TESTER APRÈS INSTALLATION

1. **Ouvrir SoukiTV** sur le téléphone
2. **Sélectionner une catégorie** (ex: News)
3. **Cliquer sur une chaîne**
4. ✅ STV Player doit s'ouvrir automatiquement
5. ✅ Une publicité test doit s'afficher
6. ✅ La vidéo doit se lancer

---

## 🔐 PARAMÈTRES XIAOMI/MIUI

### Autoriser l'installation
- Paramètres → Confidentialité → Sources inconnues → Activer

### Éviter que MIUI tue les apps
- Paramètres → Batterie et performances
- Paramètres d'économie de batterie
- Sélectionner STV et SoukiTV → "Aucune restriction"

### Autorisations
Les deux apps ont besoin de :
- ✅ Internet (auto)
- ✅ Réseau (auto)

---

## 📍 LOCALISATION DES APKs

Si vous ne trouvez pas les APKs, ils sont ici :

**STV Player :**
```
C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk
```

**SoukiTV :**
```
C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk
```

Vous pouvez les copier sur votre Bureau pour faciliter le transfert :

```powershell
# Copier sur le Bureau
Copy-Item "C:\Users\Lenovo\AndroidStudioProjects\STV1\app\build\outputs\apk\prod\release\app-prod-release.apk" "$env:USERPROFILE\Desktop\STV-Player.apk"
Copy-Item "C:\Users\Lenovo\AndroidStudioProjects\STV1\soukitv\build\outputs\apk\release\soukitv-release.apk" "$env:USERPROFILE\Desktop\SoukiTV.apk"
```

---

## ✅ RÉSUMÉ

**3 méthodes au choix :**
1. 🚀 **Script automatique** (via USB + ADB) → Plus rapide
2. 📲 **Installation manuelle** (transfert fichier) → Plus simple
3. 🔧 **Commandes ADB** (si vous maîtrisez) → Plus professionnel

**Choisissez celle qui vous convient le mieux !**

---

**Bon test ! 📱✨**


