# 📱 GUIDE D'INSTALLATION APK - STV Player

**Date** : 26/02/2026  
**APK** : app-prod-release.apk (4.81 MB)  
**Package** : com.example.stv

---

## 🚀 CHEMIN DE L'APK À INSTALLER

```
C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk
```

---

## ⚙️ SOLUTION 1 : Installer via ADB (Recommandé)

### Prérequis
- ✅ Android Studio installé
- ✅ Téléphone/Émulateur Android connecté en USB
- ✅ Débogage USB activé

### Commandes
```powershell
# Chemin vers ADB
$ADB = "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Vérifier les appareils
& $ADB devices

# Installer l'APK
& $ADB install -r "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

# Lancer l'app
& $ADB shell am start -n com.example.stv/.MainActivity
```

---

## 📱 SOLUTION 2 : Installer via Android Studio

### Étapes
1. Ouvrir **Android Studio**
2. Cliquer sur **Run** > **Run 'app'**
3. Sélectionner l'appareil connecté
4. L'APK sera construit et installé automatiquement

---

## 📲 SOLUTION 3 : Transfert USB (Téléphone)

### Étapes
1. Brancher le téléphone en USB sur l'ordinateur
2. Copier l'APK sur le téléphone :
```powershell
Copy-Item `
  "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk" `
  "D:\Stockage téléphone\app-prod-release.apk"
```
3. Sur le téléphone :
   - Ouvrir le gestionnaire de fichiers
   - Chercher `app-prod-release.apk`
   - Taper deux fois pour installer
   - Confirmer les permissions

---

## ✅ VÉRIFICATIONS AVANT INSTALLATION

### Activez le débogage USB (pour ADB)

**Sur le téléphone :**
1. Ouvrir Paramètres
2. About phone
3. Taper 7 fois sur "Build number"
4. Options pour les développeurs apparaît
5. Revenir à Paramètres
6. Developer options
7. USB debugging : ON

### Sur Windows
- Accepter la clé RSA quand demandé
- Vérifier que le téléphone s'affiche dans `adb devices`

---

## 🛠️ DÉPANNAGE

### Erreur : "adb : Le terme n'est pas reconnu"

**Solution :**
```powershell
# Ajouter ADB au PATH
$ADB = "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Ou exécuter avec le chemin complet
& $ADB install "chemin\app-prod-release.apk"
```

### Erreur : "Aucun appareil trouvé"

**Solutions :**
1. Vérifier que le téléphone est bien connecté en USB
2. Vérifier que le débogage USB est activé
3. Essayer une autre connexion USB
4. Redémarrer ADB :
   ```powershell
   & "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe" kill-server
   Start-Sleep -Seconds 2
   & "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe" start-server
   ```

### Erreur : "INSTALL_FAILED_INVALID_APK"

**Solutions :**
1. Reconstruire l'APK : `.\gradlew.bat clean assembleProdRelease`
2. Vérifier que le fichier APK n'est pas corrompu
3. Supprimer l'app précédente : `adb uninstall com.example.stv`

### Erreur : "INSTALL_FAILED_INSUFFICIENT_STORAGE"

**Solution :**
- Libérer de l'espace sur le téléphone (au moins 50 MB)

---

## 🚀 APRÈS INSTALLATION

### Lancer l'app
```powershell
& $ADB shell am start -n com.example.stv/.MainActivity
```

### Voir les logs
```powershell
& $ADB logcat | findstr PlayerActivity
```

### Désinstaller
```powershell
& $ADB uninstall com.example.stv
```

---

## 🎯 TESTER L'APP

Après installation, testez :
- ✅ App s'ouvre sans crash
- ✅ Interface visible
- ✅ Bouton "Lire" réactif
- ✅ Lecteur vidéo fonctionne
- ✅ Pubs AdMob s'affichent
- ✅ Navigation OK

---

## 📱 EMULATEUR ANDROID (Alternative)

Si vous n'avez pas de téléphone :

### Lancer un émulateur
```powershell
# Depuis Android Studio UI
Tools > Device Manager > Create Device

# Ou command line
& "C:\Users\soufi\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_4_API_34
```

### Installer sur l'émulateur
```powershell
& $ADB install "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"
```

---

## 📞 SUPPORT

**Problème lors de l'installation ?**

1. ✅ Vérifier le chemin d'ADB : `C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe`
2. ✅ Vérifier que le téléphone est en mode MTP (transfert de fichiers)
3. ✅ Vérifier que le débogage USB est activé
4. ✅ Redémarrer l'ordinateur et le téléphone
5. ✅ Essayer avec un autre câble USB

---

## 🎬 COMMANDES UTILES

```powershell
# Alias pour ADB
$ADB = "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Lister les appareils
& $ADB devices

# Installer
& $ADB install -r "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

# Désinstaller
& $ADB uninstall com.example.stv

# Lancer l'app
& $ADB shell am start -n com.example.stv/.MainActivity

# Voir les logs
& $ADB logcat

# Arrêter ADB
& $ADB kill-server
```

---

**GUIDE D'INSTALLATION TERMINÉ** ✅

Utilisez la commande qui vous convient le mieux !

