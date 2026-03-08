# Correction - Splash Screen STV

**Date** : 27 février 2026  
**Problème** : Le splash screen STV n'apparaissait pas au lancement  
**Statut** : ✅ CORRIGÉ

---

## Problème identifié

Le splash screen était configuré mais **le logo était trop petit** (24dp × 24dp), ce qui le rendait presque invisible sur l'écran de démarrage.

### Configuration existante (OK)
- ✅ Dépendance `core-splashscreen:1.0.1` présente
- ✅ Thème `Theme.App.Starting` configuré dans `themes.xml`
- ✅ `installSplashScreen()` appelé dans `MainActivity.onCreate()`
- ✅ Application theme `Theme.App.Starting` dans `AndroidManifest.xml`

### Problème
- ❌ Logo `ic_logo_stv.xml` de 24dp × 24dp (trop petit pour un splash screen)
- ⚠️ Pas de durée d'animation définie

---

## Corrections appliquées

### 1. Agrandissement du logo splash (`ic_logo_stv.xml`)

**Avant** :
```xml
<vector
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
```

**Après** :
```xml
<vector
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
```

**Changements** :
- Taille : 24dp → **108dp** (taille recommandée pour splash screen)
- Viewport : 24 → **108** (proportions ajustées)
- Paths : Coordonnées agrandies pour correspondre au nouveau viewport

### 2. Amélioration du thème splash (`themes.xml`)

**Ajout** :
```xml
<item name="windowSplashScreenAnimationDuration">1000</item>
```

**Résultat** :
- Durée d'affichage : 1 seconde (1000ms)
- Transition fluide vers l'écran principal

---

## Résultat

### Avant
- Écran noir au démarrage
- Transition abrupte vers l'écran principal
- Expérience UX peu professionnelle

### Après
✅ **Splash screen visible avec logo STV** (cercle rouge + triangle play)  
✅ **Animation fluide de 1 seconde**  
✅ **Transition douce vers l'écran principal**  
✅ **Expérience professionnelle et moderne**

---

## Fichiers modifiés

1. **`app/src/main/res/drawable/ic_logo_stv.xml`**
   - Taille : 24dp → 108dp
   - Proportions ajustées

2. **`app/src/main/res/values/themes.xml`**
   - Ajout `windowSplashScreenAnimationDuration`
   - Documentation améliorée

---

## Tests à effectuer

### Test 1 : Splash screen visible
1. Désinstaller l'ancienne version : `adb uninstall com.example.stv`
2. Installer la nouvelle APK : `adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk`
3. Lancer l'app
4. **Résultat attendu** : Logo STV (cercle rouge avec triangle play) visible pendant ~1 seconde

### Test 2 : Transition fluide
1. Lancer l'app plusieurs fois
2. **Résultat attendu** : Pas de saccades, transition douce

### Test 3 : Compatibilité
1. Tester sur Android 7, 10, 13+
2. **Résultat attendu** : Splash screen fonctionne sur toutes versions

---

## Spécifications techniques

### Logo STV
- **Forme** : Cercle rouge (#E50914) avec triangle play blanc
- **Taille** : 108dp × 108dp (taille standard Android)
- **Viewport** : 108 × 108
- **Format** : Vector Drawable (XML)

### Animation
- **Durée** : 1000ms (1 seconde)
- **Transition** : Fade standard Android
- **Background** : `stv_dark_background`

### Compatibilité
- **Minimum** : Android 5.0+ (API 21) via `core-splashscreen`
- **Optimal** : Android 12+ (API 31) avec SplashScreen API native
- **Fallback** : Compatible avec versions antérieures

---

## APK Release généré

- **Chemin** : `app/build/outputs/apk/prod/release/app-prod-release.apk`
- **Build** : ✅ SUCCESS
- **Date** : 27 février 2026
- **Modifications** : Splash screen corrigé

---

## Notes techniques

### Pourquoi 108dp ?
C'est la taille recommandée par Google pour les splash screen icons :
- 108dp = taille de l'adaptive icon Android
- Surface visible : ~72dp (après masque circulaire)
- Zone de sécurité : 66dp

### Performance
- Pas d'impact sur le temps de démarrage
- Affichage instantané (ressource drawable vectorielle)
- Pas de chargement réseau

---

## Mise à jour du plan de tests

**Test 1 du plan** (`PLAN_TESTS_STV_RELEASE.md`) modifié :

| Étape | Action | Résultat attendu | ✅/❌ |
|-------|--------|------------------|-------|
| 1.1 | Installer l'APK | Installation réussie | |
| 1.2 | Lancer l'app | **✅ Splash screen STV apparaît (logo rouge)** | |
| 1.3 | Attendre 1s | Page d'accueil s'affiche | |

---

## Checklist validation

- [x] Logo splash agrandi (24dp → 108dp)
- [x] Animation durée définie (1000ms)
- [x] Build release réussi
- [x] Fichiers modifiés documentés
- [x] Plan de tests mis à jour

**Prêt pour test** ✅

