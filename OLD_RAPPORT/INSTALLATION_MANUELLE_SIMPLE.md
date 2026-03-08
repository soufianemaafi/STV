# 📱 INSTALLATION MANUELLE STV PLAYER - GUIDE RAPIDE

**Temps** : 2 minutes  
**Aucun outil requis** ✅

---

## 🎯 ÉTAPES SIMPLES

### 1. Copier l'APK sur ton téléphone

**Chemin sur PC** :
```
C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk
```

**Comment copier** :
- **Option A** : Via câble USB → Copier dans dossier "Téléchargements" du téléphone
- **Option B** : Envoyer par email → Ouvrir email sur téléphone
- **Option C** : Upload sur Google Drive → Télécharger sur téléphone
- **Option D** : Via Bluetooth

---

### 2. Activer "Sources inconnues"

**Sur Android 8+ (Oreo et plus récent)** :
1. Paramètres → Sécurité
2. Installer des apps inconnues
3. Trouver "Gestionnaire de fichiers" ou "Mes fichiers"
4. Activer "Autoriser cette source"

**Sur Android 7 (ancien)** :
1. Paramètres → Sécurité
2. Activer "Sources inconnues"

---

### 3. Installer l'APK

1. Ouvrir le **Gestionnaire de fichiers** sur le téléphone
2. Aller dans **Téléchargements** (ou l'endroit où tu as copié l'APK)
3. Trouver **app-prod-release.apk**
4. Cliquer dessus
5. Cliquer **Installer**
6. Attendre ~10 secondes
7. Cliquer **Ouvrir** pour lancer l'app

---

### 4. Tester

1. ✅ Splash screen visible (logo rouge STV)
2. ✅ Accueil s'affiche
3. ✅ Menu (☰) → Politique de Confidentialité → S'affiche (pas de crash)
4. ✅ Menu → Conditions d'Utilisation → S'affiche (pas de crash)
5. ✅ Bouton "+" → Ajouter vidéo
6. ✅ Lire vidéo (Big Buck Bunny)

---

## ⚠️ APRÈS INSTALLATION

**À FAIRE** :
1. Remplacer `support@example.com` par ton email
2. Rebuild APK
3. Réinstaller

**Comment** :
```
Ouvrir PrivacyPolicyActivity.kt
Ctrl+H : Find "support@example.com" → Replace "ton_email@gmail.com"
Rebuild : .\gradlew.bat assembleProdRelease
Réinstaller (même méthode)
```

---

## 🎯 PROBLÈMES COURANTS

### "Installation bloquée"
→ Activer "Sources inconnues" (étape 2)

### "App non installée"
→ Désinstaller ancienne version d'abord

### "Fichier corrompu"
→ Vérifier que l'APK a bien été copié (8 MB)

---

## 📁 DOSSIER APK OUVERT

J'ai ouvert le dossier contenant l'APK :
```
C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\
```

**Tu y trouveras** :
- ✅ `app-prod-release.apk` (~8 MB)

**Copie ce fichier sur ton téléphone !** 📱

---

**C'est la méthode la plus simple si tu n'as pas adb configuré.** ✅

