# 🎥 TUTORIEL VIDÉO - Installation APK STV (Format Texte)

**Date** : 26/02/2026  
**Durée** : 5 minutes  
**Niveau** : Débutant

---

## 📺 ÉTAPE 1 : PRÉPARER LE TÉLÉPHONE (2 MIN)

### Vidéo Script
```
[Scene 1] Écran du téléphone
📍 Localisation: Paramètres

1. Ouvrir "Paramètres"
2. Scroller jusqu'à "About phone" (À propos du téléphone)
3. Trouver "Build number"
4. Taper 7 fois sur "Build number"
   ✅ Message: "Developer options unlocked!"

[Scene 2] Retour paramètres
5. Revenir à Paramètres
6. Scroller vers le haut
7. Nouvelle option apparaît : "Developer options"
8. Ouvrir "Developer options"
9. Chercher "USB debugging"
10. Basculer "USB debugging" ON (vert)

✅ Téléphone prêt pour installation!
```

### À retenir
- ✅ Build number (7 taps) = Developer options débloqué
- ✅ USB debugging = OUI
- ✅ Garder le téléphone en mode transfert de fichiers (MTP)

---

## 🔌 ÉTAPE 2 : CONNECTER LE TÉLÉPHONE (1 MIN)

### Vidéo Script
```
[Scene 1] Ordinateur Windows
1. Brancher le câble USB
2. Téléphone affichera: "Allow USB debugging?"
3. Taper "Always allow" OU "Allow this time"

[Scene 2] Vérification
4. Téléphone se connecte
5. Vous devriez voir notification de stockage

✅ Téléphone détecté!
```

### À retenir
- ✅ Accepter les permissions sur le téléphone
- ✅ Ne pas débrancher pendant l'installation
- ✅ Garder l'écran allumé (pas de mise en veille)

---

## 💾 ÉTAPE 3 : INSTALLER L'APK (2 MIN)

### Vidéo Script - Méthode 1 (Batch)

```
[Scene 1] Explorateur Windows
1. Naviguer vers: C:\Users\soufi\AndroidStudioProjects\STV5
2. Chercher "install_apk.bat"
3. Double-cliquer sur install_apk.bat

[Scene 2] Terminal noir
4. Terminal s'ouvre
5. Messages:
   "Demarrage du serveur ADB..."
   "Appareils connectes: [device_id]"
   "Installation de l'APK..."
   
6. Patienter 10-30 secondes

[Scene 3] Notification téléphone
7. Téléphone affiche:
   "STV est en cours d'installation..."

[Scene 4] Terminal
8. Terminal affiche:
   "SUCCES! L'app a ete installee"
   "Voulez-vous lancer l'app? (O/N)"

9. Taper O et Entrée

[Scene 5] Téléphone
10. App STV se lance automatiquement

✅ INSTALLATION RÉUSSIE!
```

### Vidéo Script - Méthode 2 (PowerShell)

```
[Scene 1] Ouvrir PowerShell
1. Touche Windows + R
2. Taper: powershell
3. Entrée

[Scene 2] Copier les commandes
4. Copier la première commande:
   $ADB = "C:\Users\soufi\AppData\Local\Android\Sdk\platform-tools\adb.exe"

5. Coller dans PowerShell (Ctrl+V)
6. Entrée

7. Copier la deuxième commande:
   & $ADB devices

8. Entrée
9. Vous devriez voir [device_id]

[Scene 3] Installation
10. Copier:
    & $ADB install -r "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

11. Entrée
12. Patienter 10-30 secondes

13. Message: "Success [or error]"

✅ Si "Success" -> Installation réussie!
```

---

## ✅ ÉTAPE 4 : VÉRIFIER L'INSTALLATION (1 MIN)

### Vidéo Script

```
[Scene 1] Téléphone
1. Ouvrir "Apps" / "Tous les apps"
2. Chercher "STV" dans la liste
3. L'app "STV Player" doit apparaître

[Scene 2] Lancer l'app
4. Taper sur "STV Player"
5. App s'ouvre
6. Vous voyez:
   - Menu avec "Sky News Arabia" etc.
   - Champ pour entrer une URL personnalisée
   - Bouton "LIRE LA VIDÉO"

[Scene 3] Tester
7. Taper sur "Sky News Arabia"
8. App passe en mode lecteur
9. Pub AdMob s'affiche
10. Après 6 secondes, vidéo commence à jouer

✅ APP FONCTIONNE PARFAITEMENT!
```

---

## 🎬 CAS D'ERREUR

### Si erreur "Aucun appareil trouvé"

```
[Scene 1] Vérifications
1. Téléphone bien branché en USB?
   ✓ Vérifier le câble
   ✓ Essayer un autre port USB

2. USB debugging activé?
   ✓ Paramètres > Developer options > USB debugging ON

3. Permissions acceptées?
   ✓ Débrancher et rebrancher
   ✓ Accepter les permissions quand demandé

[Scene 2] Solutions
4. Redémarrer ADB:
   & $ADB kill-server
   Start-Sleep 2
   & $ADB start-server
   Start-Sleep 3
   & $ADB devices

5. Si toujours pas de device:
   ✓ Installer les drivers USB (Google USB Driver)
   ✓ Redémarrer Windows
   ✓ Essayer avec un émulateur
```

### Si erreur "Installation failed"

```
[Scene 1] Causes
1. APK corrompu?
   ✓ Reconstruire: ./gradlew clean assembleProdRelease

2. App déjà installée?
   ✓ Désinstaller d'abord: & $ADB uninstall com.example.stv
   ✓ Puis installer: & $ADB install -r ...

3. Pas assez d'espace?
   ✓ Libérer 50+ MB sur le téléphone
   ✓ Aller à Paramètres > Storage et nettoyer

[Scene 2] Nouvelle tentative
4. Recommencer l'installation
5. Si toujours erreur: contacter support
```

---

## 📱 TEST DE L'APP

### Vidéo Script - Test complet

```
[Scene 1] Interface principale
✓ App s'ouvre sans crash
✓ Menu visible et réactif
✓ Boutons cliquables

[Scene 2] Lire une vidéo
1. Taper sur "Sky News Arabia"
2. Interface du lecteur s'affiche
3. Spinner "Chargement..."
4. Pub AdMob apparaît (6 secondes)
5. Vidéo commence à jouer

[Scene 3] Contrôles du lecteur
1. Taper sur play/pause → Fonctionne ✓
2. Taper sur volume → Barre volume ✓
3. Taper sur qualité → Menu qualité ✓
4. Taper sur sous-titres → Menu ST ✓

[Scene 4] Rotation écran
1. Tourner le téléphone
2. Lecteur s'adapte automatiquement
3. Interface responsive ✓

✅ TOUS LES TESTS PASSENT!
```

---

## 🎁 RÉSUMÉ RAPIDE (1 MIN)

### Checklist installation
- [ ] Téléphone prêt (USB debugging ON)
- [ ] Téléphone branché en USB
- [ ] Permissions acceptées sur le téléphone
- [ ] Script batch lancé OU commandes PowerShell
- [ ] Patienter 30 secondes
- [ ] App "STV" apparaît
- [ ] App se lance sans erreur
- [ ] Vidéo joue correctement

---

## 📞 SUPPORT

**Si vous avez un problème** :

1. Vérifier le GUIDE_INSTALLATION_APK.md complet
2. Essayer Méthode 1 (batch) OU Méthode 2 (PowerShell)
3. Redémarrer l'ordinateur et le téléphone
4. Essayer avec un autre câble USB
5. Utiliser Android Studio (Méthode 3)

---

## 🎉 FÉLICITATIONS!

Vous avez installé **STV Player** avec succès! 🎉

Vous pouvez maintenant :
- ✅ Lire des vidéos HLS, DASH, MP4
- ✅ Voir les publicités AdMob
- ✅ Utiliser tous les contrôles du lecteur
- ✅ Intégrer avec SoukiTV
- ✅ Partager avec d'autres appareils

**Amusez-vous bien!** 🚀

---

*Tutoriel texte - Format vidéo*  
*Date: 26/02/2026*

