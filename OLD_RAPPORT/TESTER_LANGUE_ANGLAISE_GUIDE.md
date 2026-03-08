# 🧪 TESTER L'ANGLAIS SANS CHANGER LA LANGUE DU TÉLÉPHONE

**Durée** : 2-5 minutes  
**Difficulté** : ⭐ Très facile

---

## 🎯 MÉTHODE 1 : Per-App Language Settings (Android 13+) ⭐ RECOMMANDÉ

Si ton téléphone est **Android 13 ou plus récent**, tu peux changer la langue **uniquement pour STV** sans toucher au reste du système !

### Étapes

1. **Installer l'APK STV**
   ```powershell
   .\installer_stv_apk.ps1
   ```

2. **Sur le téléphone** :
   - Paramètres → Applications
   - Trouver "**STV**" dans la liste
   - Cliquer sur STV
   - Cliquer sur "**Langue de l'application**" (ou "**App language**")
   - Choisir "**English**"

3. **Relancer STV**
   - Fermer complètement STV (swipe depuis récents)
   - Relancer STV
   - ✅ L'app est maintenant en **anglais**
   - ✅ Le reste du téléphone reste en **français**

**Avantages** :
- ✅ Ton téléphone reste en français
- ✅ Seule STV passe en anglais
- ✅ Facile à tester
- ✅ Pas de perturbation du système

---

## 🎯 MÉTHODE 2 : Changer langue système (Android 7+)

Si ton téléphone est **Android 7-12**, tu dois changer la langue du système temporairement.

### Étapes

#### Sur Android 10+ :
1. **Paramètres → Système → Langues et saisie**
2. **Langues → Ajouter une langue**
3. Choisir "**English (United States)**"
4. **Glisser "English" en 1ère position** (important!)
5. Le système passe en anglais
6. Lancer STV → Tout en anglais ✅

#### Sur Android 7-9 :
1. **Paramètres → Langues et saisie**
2. **Langue → Ajouter une langue**
3. Choisir "**English**"
4. Définir comme langue principale
5. Lancer STV → Tout en anglais ✅

### Remettre en français après test
1. Paramètres → System → Languages (menu en anglais maintenant)
2. Languages → Glisser "Français" en 1ère position
3. Système revient en français

**Avantages** :
- ✅ Fonctionne sur tous Android 7+
- ✅ Test complet du multi-langues

**Inconvénients** :
- ⚠️ Tout le téléphone passe en anglais temporairement
- ⚠️ Navigation menus en anglais pour revenir

---

## 🎯 MÉTHODE 3 : Adb + Locale (SANS changer téléphone) ⭐ GEEK

Si tu as `adb` configuré, tu peux forcer la langue **uniquement pour STV** !

### Prérequis
- ✅ Téléphone connecté via USB
- ✅ Débogage USB activé
- ✅ adb configuré (ou utiliser script `installer_stv_apk.ps1`)

### Commandes

```powershell
# Trouver adb
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Installer APK
& $adb install -r "app\build\outputs\apk\prod\release\app-prod-release.apk"

# Forcer locale anglais pour STV uniquement
& $adb shell "am start -n com.example.stv/.MainActivity --es locale en"

# Ou via configuration app
& $adb shell "settings put system system_locales en-US"
& $adb shell am force-stop com.example.stv
& $adb shell am start -n com.example.stv/.MainActivity
```

**Note** : Cette méthode peut ne pas fonctionner sur tous les appareils (dépend de la version Android).

---

## 🎯 MÉTHODE 4 : Émulateur Android Studio ⭐ SANS TOUCHER TON TEL

Utilise un émulateur Android avec langue anglaise !

### Étapes

1. **Ouvrir Android Studio**
2. **Device Manager** (icône téléphone sur le côté)
3. **Create Device** (ou utiliser émulateur existant)
4. Choisir un appareil (ex: Pixel 5)
5. Choisir système (ex: Android 13)
6. **Cliquer sur l'engrenage** (Advanced Settings)
7. **Boot options → Set system locale → English (en_US)**
8. **Finish** → Lancer émulateur

9. **Installer APK sur émulateur**
   ```powershell
   adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk
   ```

10. **Lancer STV** → Tout en anglais ✅

**Avantages** :
- ✅ Ton téléphone reste intact
- ✅ Test complet
- ✅ Peut tester plusieurs langues facilement

**Inconvénients** :
- ⚠️ Nécessite Android Studio
- ⚠️ Émulateur plus lent que device réel

---

## 🎯 MA RECOMMANDATION POUR TOI

### Si Android 13+ ⭐ MEILLEURE OPTION
**Méthode 1** : Per-App Language Settings
- ✅ Rapide (30 secondes)
- ✅ Téléphone reste en français
- ✅ Seule STV en anglais

### Si Android 7-12
**Méthode 2** : Changer langue système temporairement
- ✅ Fonctionne partout
- ⚠️ Téléphone entier en anglais 2 minutes

### Si tu ne veux pas toucher ton téléphone
**Méthode 4** : Émulateur Android Studio
- ✅ Téléphone intact
- ⚠️ Nécessite Android Studio

---

## 📋 CHECKLIST TEST LANGUE ANGLAISE

### Ce que tu dois vérifier

**Écran d'accueil** :
- [ ] Titre : "**Welcome to STV**" (pas "Bienvenue")
- [ ] Tagline : "**Watch your favorite videos streaming**"
- [ ] Bouton : "**Videos**" (pas "Mes Vidéos")

**Menu drawer** :
- [ ] "**History**" (pas "Historique")
- [ ] "**Favorites**" (pas "Favoris")
- [ ] "**Settings**" (pas "Paramètres")
- [ ] "**Quit**" (pas "Quitter")
- [ ] "**Privacy Policy**"
- [ ] "**Terms of Service**"

**Page vidéos** :
- [ ] Titre : "**Videos**"
- [ ] Bouton : "**+**" (universel)
- [ ] Placeholder : "**No streams added.**"

**Page ajout** :
- [ ] Titre : "**Add Stream**" (pas "Ajouter un flux")
- [ ] Champs : "**Title**", "**URL**"
- [ ] Boutons : "**Save**", "**Cancel**"

**Player** :
- [ ] Boutons : "**Quality**", "**Format**"
- [ ] Dialogue : "**Video quality**", "**Display format**"
- [ ] Options : "**Auto**", "**Fit**", "**Fill**", "**Zoom**"

**Messages d'erreur** :
- [ ] "**URL not provided**"
- [ ] "**Playback error**"
- [ ] "**No internet connection**"

---

## ✅ RÉSULTAT ATTENDU

### En anglais (système EN)
```
Écran accueil :
━━━━━━━━━━━━━━━━━━
        [STV]
  Welcome to STV
  
Watch your favorite 
videos streaming

      [+]

    [Videos]
━━━━━━━━━━━━━━━━━━
```

### En français (système FR)
```
Écran accueil :
━━━━━━━━━━━━━━━━━━
        [STV]
  Bienvenue dans STV
  
Regardez vos vidéos 
préférées en streaming

      [+]

   [Mes Vidéos]
━━━━━━━━━━━━━━━━━━
```

---

## 🚨 SI ÇA NE FONCTIONNE PAS

### Problème : App reste en français même en anglais

**Causes possibles** :
1. ❌ APK ancien (pas rebuild)
   - Solution : Rebuild APK (`.\gradlew.bat assembleProdRelease`)
   
2. ❌ Cache Android
   - Solution : Désinstaller complètement puis réinstaller
   ```powershell
   adb uninstall com.example.stv
   adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk
   ```

3. ❌ App pas fermée/relancée
   - Solution : Fermer depuis récents + relancer

4. ❌ Langue système mal détectée
   - Solution : Redémarrer le téléphone

---

## 💡 ASTUCE RAPIDE

**Pour tester rapidement SANS changer langue téléphone** :

### Méthode super simple (Android 13+)
```
1. Installer STV
2. Paramètres → Applications → STV → Langue app → English
3. Relancer STV
4. ✅ STV en anglais, téléphone en français !
```

**Durée** : 30 secondes ⚡

---

## 🎯 VÉRIFICATION VISUELLE RAPIDE

Une fois en anglais, tu devrais voir **instantanément** :

**Menu drawer** :
- "**Quit**" au lieu de "Quitter" ✅
- "**History**" au lieu de "Historique" ✅

**Accueil** :
- "**Welcome to STV**" au lieu de "Bienvenue dans STV" ✅

**Si tu vois ces textes en anglais** → ✅ Multi-langues fonctionne !

---

## 📞 BESOIN D'AIDE ?

**Version Android** : Vérifie ta version
```
Paramètres → À propos du téléphone → Version Android
```

- **Android 13-15** : Utilise Méthode 1 (per-app language)
- **Android 7-12** : Utilise Méthode 2 (changer système temporairement)

---

**LE PLUS SIMPLE : Méthode 1 si Android 13+** ⚡

Ton téléphone reste en français, seule l'app STV passe en anglais ! 🎉

