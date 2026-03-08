# 🔧 SOLUTION - ADB Non Reconnu

**Problème** : `adb : Le terme «adb» n'est pas reconnu`  
**Cause** : ADB n'est pas dans le PATH Windows  
**Solutions** : 3 méthodes disponibles

---

## ✅ SOLUTION 1 : Script Automatique (RECOMMANDÉ)

J'ai créé un script qui trouve automatiquement adb.exe.

```powershell
cd C:\Users\soufi\AndroidStudioProjects\STV5
.\installer_stv_apk.ps1
```

**Ce script** :
- ✅ Cherche adb.exe automatiquement (5 emplacements standard)
- ✅ Vérifie l'appareil connecté
- ✅ Installe l'APK automatiquement
- ✅ Lance l'app
- ✅ Ou propose installation manuelle si adb absent

---

## ✅ SOLUTION 2 : Installation Manuelle (SIMPLE)

**Pas besoin d'adb !** Tu peux installer directement sur ton téléphone.

### Étapes :
1. **Copier l'APK sur ton téléphone** :
   - Via USB (copier le fichier dans Téléchargements)
   - Ou envoyer par email
   - Ou via cloud (Google Drive, Dropbox)

2. **Chemin APK** :
   ```
   C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk
   ```

3. **Sur le téléphone** :
   - Ouvrir le gestionnaire de fichiers
   - Trouver le fichier `app-prod-release.apk`
   - Cliquer dessus
   - Si demandé : Autoriser "Sources inconnues"
   - Cliquer "Installer"

**Durée** : 2 minutes  
**Avantage** : Pas besoin d'adb, pas besoin de câble USB

---

## ✅ SOLUTION 3 : Ajouter ADB au PATH

Si tu veux utiliser `adb` directement dans PowerShell.

### Trouver adb.exe

**Emplacements standard** :
```
C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

Ou :
```
C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe
```

### Vérifier si adb.exe existe

```powershell
# Essayer ce chemin
Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```

Si **True** : adb.exe existe à cet emplacement.

### Utiliser le chemin complet (temporaire)

```powershell
# Au lieu de : adb install ...
# Utiliser :
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"
```

### Ajouter au PATH (permanent)

**Méthode GUI** :
1. Paramètres Windows → Système → Informations système
2. Paramètres avancés → Variables d'environnement
3. Variables utilisateur → Path → Modifier
4. Nouveau → Ajouter : `C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools`
5. OK → OK → Redémarrer PowerShell

**Méthode PowerShell (admin)** :
```powershell
# Ajouter au PATH utilisateur
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$newPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
[Environment]::SetEnvironmentVariable("Path", "$userPath;$newPath", "User")

# Redémarrer PowerShell
```

---

## ✅ SOLUTION 4 : Installer Android SDK Platform Tools

Si adb.exe n'existe pas du tout.

### Télécharger
https://developer.android.com/tools/releases/platform-tools

### Installation
1. Télécharger le ZIP
2. Extraire dans : `C:\Android\platform-tools`
3. Ajouter au PATH : `C:\Android\platform-tools`
4. Redémarrer PowerShell

---

## 🚀 SOLUTION IMMÉDIATE

**Lance simplement le script que j'ai créé** :

```powershell
cd C:\Users\soufi\AndroidStudioProjects\STV5
.\installer_stv_apk.ps1
```

Ce script :
- ✅ Trouve adb.exe automatiquement
- ✅ Ou propose installation manuelle si adb absent
- ✅ Installe l'APK
- ✅ Lance l'app

---

## 📋 INSTALLATION MANUELLE RAPIDE

Si tu veux installer sans adb ni script :

```powershell
# 1. Ouvrir le dossier de l'APK
Start-Process "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release"

# 2. Copier app-prod-release.apk sur ton téléphone

# 3. Sur le téléphone, installer l'APK
```

**Durée** : 2 minutes  
**Aucun outil requis** ✅

---

## 🎯 MA RECOMMANDATION

**Option 1** : Lance `.\installer_stv_apk.ps1` (automatique)  
**Option 2** : Copie l'APK sur ton téléphone et installe (simple)

**Les deux fonctionnent** ! Choisis celle que tu préfères.

---

**Chemin APK** :
```
C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk
```

