# 🎯 PLAN DE CORRECTION DÉTAILLÉ - STV PLAYER

**Date** : 24 février 2026  
**Durée totale estimée** : 8 heures (Jour 1-2)  
**Priorité** : 🔴 CRITIQUE (Avant publication Play Store)

---

## 📋 PHASE 1 : CRITIQUES (8 HEURES - JOUR 1-2)

Tous les éléments de cette phase DOIVENT être corrigés avant toute publication.

---

## ✏️ ÉTAPE 1 : Remplacer les IDs AdMob Test par Production

**Durée estimée** : 30 minutes  
**Priorité** : 🔴 CRITIQUE  
**Impact** : Rejet Play Store CERTAIN si non fait  
**Difficulté** : Facile

### 1.1 Préalables

- Avoir un compte Google AdMob actif
- Avoir créé 2 Ad Units dans AdMob Console :
  - **1 x Interstitial Ad** (pour la pub entre les vidéos)
  - **1 x Banner Ad** (pour fallback MREC 300×250)

### 1.2 Détails des IDs AdMob

**Actuellement (TEST)** :
```
Interstitial : ca-app-pub-3940256099942544/1033173712
Banner       : ca-app-pub-3940256099942544/6300978111
```

**À faire** :
1. Se connecter à https://admob.google.com
2. Créer/récupérer les Ad Unit IDs réels
3. Format : `ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy`
   - `xxxxxxxxxxxxxxxx` = Publisher ID (10 caractères)
   - `yyyyyyyyyy` = Ad Unit ID (10 caractères)

### 1.3 Fichier à modifier

**Chemin** : `app/build.gradle.kts`

**Section à modifier** (lignes ~32-40) :

```kotlin
// ACTUELLEMENT (À REMPLACER) :
create("prod") {
    dimension = "environment"
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
        "\"ca-app-pub-3940256099942544/1033173712\"")
    buildConfigField("String", "ADMOB_BANNER_ID", 
        "\"ca-app-pub-3940256099942544/6300978111\"")
}

// À DEVENIR (EXEMPLE AVEC VOS IDS RÉELS) :
create("prod") {
    dimension = "environment"
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
        "\"ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy\"")  // ← ID réel interstitiel
    buildConfigField("String", "ADMOB_BANNER_ID", 
        "\"ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz\"")  // ← ID réel banner
}
```

### 1.4 Vérification

Après modification :
```bash
# Compiler pour vérifier
./gradlew clean assembleProdRelease

# Vérifier dans build logs qu'aucune erreur
```

**Validation** : BUILD SUCCESSFUL sans erreurs

---

## ✏️ ÉTAPE 2 : Créer Privacy Policy + Conditions d'Utilisation

**Durée estimée** : 2 heures  
**Priorité** : 🔴 CRITIQUE  
**Impact** : Rejet Play Store CERTAIN si absent  
**Difficulté** : Moyen (Rédaction)

### 2.1 Privacy Policy - Contenu Obligatoire

**À créer** : Document HTML publique (URL accessible)

**Contenu minimal requis** :

```markdown
# POLITIQUE DE CONFIDENTIALITÉ - STV PLAYER

## 1. INFORMATIONS GÉNÉRALES

Cette politique décrit comment l'application STV Player traite vos données.
Application : STV Player
Dernière mise à jour : [DATE]
Éditeur : [VOTRE NOM/ENTREPRISE]

## 2. DONNÉES COLLECTÉES

### 2.1 Données collectées automatiquement

- **Identifiants publicitaires** (Google AdMob)
  - Type : Publicités personnalisées
  - Traitement par : Google
  - Finalité : Afficher des ads adaptées

- **Métadonnées de lecture**
  - URLs de flux (stockées localement seulement)
  - Durée de session (non transmise)

### 2.2 Données NON collectées

- ❌ Aucune donnée personnelle identifiante
- ❌ Pas de géolocalisation
- ❌ Pas d'enregistrement d'appels/SMS
- ❌ Pas d'informations de contact
- ❌ Pas d'historique de navigation global

## 3. UTILISATION DES DONNÉES

Les données sont utilisées pour :
- Afficher des publicités Google AdMob pertinentes
- Mesurer performance des campagnes publicitaires
- Améliorer l'expérience utilisateur
- Comptabilité et conformité légale

## 4. PARTAGE DES DONNÉES

- **Google AdMob** : Identifiants publicitaires (nécessaire pour les ads)
- **Aucun autre tiers** : Les données ne sont pas partagées

## 5. STOCKAGE DES DONNÉES

- Données locales : Supprimées à la désinstallation
- Données Google AdMob : Sécurisées par Google
- Rétention : Selon politique Google AdMob

## 6. SÉCURITÉ

- Données AdMob sécurisées par Google Cloud
- Pas de chiffrement local (future amélioration)
- Pas de transmission d'URLs sensibles

## 7. DROITS DE L'UTILISATEUR (RGPD/CCPA/LGPD)

Vous avez le droit de :
- **Accéder** à vos données (via Google)
- **Supprimer** vos données (réinstaller l'app)
- **Refuser** les ads personnalisées (Paramètres Google)
- **Porter** vos données (si applicable)

## 8. CONTACT

Pour toute question concernant cette politique :
Email : privacy@yoursite.com
Adresse : [VOTRE ADRESSE]

## 9. MODIFICATIONS

Cette politique peut être mise à jour. Nous vous informerons de tout changement.

Dernière mise à jour : [DATE ACTUELLE]
```

### 2.2 Conditions d'Utilisation - Contenu Obligatoire

**À créer** : Document HTML publique (URL accessible)

```markdown
# CONDITIONS D'UTILISATION - STV PLAYER

## 1. ACCEPTATION DES CONDITIONS

En utilisant STV Player, vous acceptez ces conditions.

## 2. LICENCE D'UTILISATION

L'application est fournie sous licence personnelle, non-commerciale.
Vous pouvez :
- ✅ Télécharger et installer l'application
- ✅ Utiliser pour visionnage personnel
- ✅ Partager le lien de téléchargement

Vous ne pouvez pas :
- ❌ Modifier le code/ressources
- ❌ Vendre ou distribuer pour profit
- ❌ Utiliser à titre commercial

## 3. CONTENU UTILISATEUR

Vous êtes responsable :
- Du contenu que vous regardez (droits d'auteur)
- De la conformité légale dans votre juridiction
- Du respect des droits de propriété intellectuelle

## 4. LIMITATION DE RESPONSABILITÉ

L'application est fournie "telle quelle" sans garantie.
Nous ne sommes pas responsables de :
- Perte de données
- Dommages indirects
- Interruptions de service
- Contenu des flux vidéo

## 5. PROPRIÉTÉ INTELLECTUELLE

- STV Player © [VOTRE NOM/ENTREPRISE]
- ExoPlayer © Google (Apache 2.0)
- Jetpack Compose © Google (Apache 2.0)
- Tous les logos/marques sont propriétés respectives

## 6. RÉSILIATION

Nous pouvons résilier accès si vous :
- Violez ces conditions
- Utilisez l'app à des fins illégales
- Modifiez/piratez l'application

## 7. MODIFICATIONS

Ces conditions peuvent être mises à jour sans préavis.
Consultez régulièrement cette page.

Dernière mise à jour : [DATE ACTUELLE]
```

### 2.3 Où héberger

**Options recommandées** (dans l'ordre) :

**Option 1 : Site Web Personnel/Entreprise (MEILLEUR)**
```
https://yoursite.com/privacy
https://yoursite.com/terms
```
- Meilleur contrôle
- Plus professionnel
- Facilement updatable

**Option 2 : GitHub Pages (GRATUIT)**
```
https://yourusername.github.io/stv-player/privacy
https://yourusername.github.io/stv-player/terms
```
- Gratuit
- Versionning Git
- HTTPS automatique

**Option 3 : Google Sites (GRATUIT & SIMPLE)**
```
https://sites.google.com/view/stvplayerprivacy
https://sites.google.com/view/stvplayerterms
```
- Très simple
- HTTPS
- Pas de codage

**Option 4 : Notion Public Page (GRATUIT)**
```
https://notion.so/stv-privacy
https://notion.so/stv-terms
```
- Facile d'édition
- Collaboration possible
- Formatage automatique

### 2.4 Intégration dans Play Store

Une fois hébergé, vous devrez :

**Dans Google Play Console** :
1. Aller sur l'app
2. "Store listing" → "App content"
3. Remplir les URLs :
   - Privacy Policy : https://yoursite.com/privacy
   - Terms of Service : https://yoursite.com/terms

**Important** : Ces URLs doivent être PUBLIQUES et ACCESSIBLES

### 2.5 Vérification

Tester :
```bash
# Les URLs sont-elles accessibles ?
curl https://yoursite.com/privacy
curl https://yoursite.com/terms

# Contiennent-elles le texte ?
# Sont-elles bien formatées en HTML ?
```

---

## ✏️ ÉTAPE 3 : Ajouter ContentDescription (Accessibilité)

**Durée estimée** : 3 heures  
**Priorité** : 🔴 CRITIQUE  
**Impact** : Possible rejet Play Store + utilisateurs malvoyants  
**Difficulté** : Facile (Répétitif)

### 3.1 Quoi faire ?

Ajouter `contentDescription` à **TOUS** les icons et boutons.

**Problème actuel** :
```kotlin
Icon(
    imageVector = Icons.Filled.Play,
    contentDescription = null  // ❌ MANQUANT
)
```

**À devenir** :
```kotlin
Icon(
    imageVector = Icons.Filled.Play,
    contentDescription = "Démarrer la lecture"  // ✅ AJOUTÉ
)
```

### 3.2 Fichiers à modifier

**Fichier principal** : `app/src/main/java/com/example/stv/PlayerActivity.kt`

**Éléments à trouver et modifier** :

#### A) PlayButton
```kotlin
// AVANT :
IconButton(onClick = onPlayClick) {
    Icon(Icons.Filled.Play, contentDescription = null)
}

// APRÈS :
IconButton(onClick = onPlayClick) {
    Icon(
        Icons.Filled.Play, 
        contentDescription = "Démarrer la lecture"
    )
}
```

#### B) PauseButton
```kotlin
// AVANT :
Icon(Icons.Filled.Pause, contentDescription = null)

// APRÈS :
Icon(
    Icons.Filled.Pause,
    contentDescription = "Mettre en pause"
)
```

#### C) BackButton / ArrowBack
```kotlin
// AVANT :
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)

// APRÈS :
Icon(
    Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription = "Retour"
)
```

#### D) BlockIcon
```kotlin
// AVANT :
Icon(Icons.Filled.Block, contentDescription = "Bloqué")

// APRÈS (DÉJÀ BON) :
Icon(
    Icons.Filled.Block,
    contentDescription = "Accès bloqué"  // C'est correct
)
```

#### E) Menu Icon
```kotlin
// AVANT :
Icon(Icons.Filled.Menu, contentDescription = null)

// APRÈS :
Icon(
    Icons.Filled.Menu,
    contentDescription = "Menu"
)
```

#### F) Autres Icons
```kotlin
// Pour tous les autres icons :
Icon(imageVector, contentDescription = "Description courte")
```

### 3.3 Liste complète de vérification

**Chercher tous les contentDescription = null dans PlayerActivity.kt** :

```
✅ Play icon → "Démarrer la lecture"
✅ Pause icon → "Mettre en pause"
✅ Back arrow → "Retour"
✅ Menu icon → "Menu"
✅ Settings icon → "Paramètres"
✅ Block icon → "Accès bloqué" (déjà OK)
✅ Info icon → "Information"
✅ Close icon → "Fermer"
```

### 3.4 Format recommandé

```kotlin
Icon(
    imageVector = Icons.Filled.Play,
    contentDescription = "Démarrer la lecture",
    modifier = Modifier.size(32.dp),
    tint = Color.White
)
```

**Bonnes pratiques** :
- Description courte (1-3 mots)
- Actionnable ("Démarrer" pas "Button")
- Pas de répétition ("Icon" pas besoin)
- En français ou anglais (cohérent)

---

## ✏️ ÉTAPE 4 : Améliorer le Contraste (WCAG)

**Durée estimée** : 1 heure  
**Priorité** : 🔴 CRITIQUE  
**Impact** : Possible rejet Play Store  
**Difficulté** : Facile

### 4.1 Problème actuel

**Contraste faible** : Noir (background) + Gris (text) = ~3:1 (WCAG FAIL)

```kotlin
Text(
    text = "Chargement...",
    color = Color.Gray,      // ❌ Gris = faible contraste
    modifier = Modifier.background(Color.Black)
)
```

**Ratio WCAG requis** :
- Normal text : minimum 4.5:1
- Large text (18sp+) : minimum 3:1
- Votre actuel : 3:1 (borderline)

### 4.2 Solution : Utiliser Color.White

**À remplacer partout** :

```kotlin
// AVANT :
Text(text = "Préparation du flux...", color = Color.Gray)

// APRÈS :
Text(text = "Préparation du flux...", color = Color.White)
```

**Contraste résultant** : Noir + Blanc = 21:1 ✅ EXCELLENT

### 4.3 Fichier à modifier

**Fichier** : `app/src/main/java/com/example/stv/PlayerActivity.kt`

**Chercher et remplacer tous** :
```kotlin
color = Color.Gray  →  color = Color.White
```

**Exceptions** :
- `Color.Red` (pour icône Block) : OK (contraste 5.2:1)
- `Color.LightGray` : OK si > 4.5:1 ratio

### 4.4 Vérification

Après modification, tester :
1. Lancer l'app en release
2. Vérifier que le texte est bien lisible
3. Pas de scintillement/clignotement

---

## ✏️ ÉTAPE 5 : Nettoyer les Logs Sensibles

**Durée estimée** : 1 heure  
**Priorité** : 🔴 CRITIQUE  
**Impact** : Sécurité  
**Difficulté** : Facile

### 5.1 Problème actuel

**Logs de debug en production** :

```kotlin
Log.d(TAG, "Ad showed successfully")        // ❌ À supprimer
Log.w(TAG, "Invalid URL: $videoUrl")        // ❌ URL exposée
Log.e(TAG, "AdBlockDetected: $result")      // ❌ Sensible
```

### 5.2 Solution : Utiliser BuildConfig.DEBUG

**Pattern recommandé** :

```kotlin
// Toujours utile :
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Ad showed successfully")
}

// Masquer données sensibles :
if (videoUrl.isEmpty()) {
    Log.w(TAG, "Invalid URL provided")  // Sans l'URL
} else {
    if (BuildConfig.DEBUG) {
        Log.w(TAG, "Invalid URL: $videoUrl")
    }
}

// Logs importants (garde-les) :
Log.e(TAG, "Critical: AdBlock detected - user blocked")
```

### 5.3 Fichier à modifier

**Fichier** : `app/src/main/java/com/example/stv/PlayerActivity.kt`

**Chercher et modifier tous les Log.d()** :

```kotlin
// AVANT :
Log.d(TAG, "Ad showed successfully")

// APRÈS :
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Ad showed successfully")
}
```

**Chercher et modifier tous les Log.w()** :

```kotlin
// AVANT :
Log.w(TAG, "Invalid URL: $urlError")

// APRÈS (Option 1 - Supprimer) :
// Supprimer la ligne

// APRÈS (Option 2 - Masquer) :
if (urlError != null) {
    Log.w(TAG, "Invalid URL provided")
}
```

### 5.4 Checklist de nettoyage

```
Chercher dans PlayerActivity.kt :
☐ Tous Log.d() → Entourer de if (BuildConfig.DEBUG)
☐ Tous Log.w() → Supprimer ou masquer sensible
☐ Tous Log.e() → Garder seulement critiques
☐ Aucune URL en log
☐ Aucun state sensible en log
```

---

## ✏️ ÉTAPE 6 : Tester Build Release Final

**Durée estimée** : 1 heure  
**Priorité** : 🔴 CRITIQUE  
**Impact** : Validation que tout fonctionne  
**Difficulté** : Facile

### 6.1 Compiler APK Release

```bash
# Terminal dans le dossier du projet
cd C:\Users\Lenovo\AndroidStudioProjects\STV1

# Nettoyer et compiler
.\gradlew.bat clean assembleProdRelease

# Attendre la compilation (5-10 min)
# Vérifier : BUILD SUCCESSFUL
```

### 6.2 Vérifier l'APK

```bash
# Localisation de l'APK
ls app/build/outputs/apk/prod/release/

# Vous devriez voir :
# app-prod-release.apk (5-6 MB)
```

### 6.3 Tester sur Device

```bash
# Installer sur Xiaomi
adb install -r app/build/outputs/apk/prod/release/app-prod-release.apk

# Lancer l'app
adb shell am start -n com.example.stv/.PlayerActivity

# Tester manuellement :
# 1. Aucun crash au démarrage ?
# 2. Privacy Policy accessible depuis play store listing ?
# 3. ContentDescription fonctionnels (TalkBack test) ?
# 4. Contraste texte lisible ?
# 5. Aucun log sensible en logcat ?
```

### 6.4 Vérification Finale

```bash
# Voir les logs en temps réel
adb logcat | grep com.example.stv

# Vérifications :
☐ Pas de ERROR logs
☐ Pas de WARNING sur données sensibles
☐ Pas d'exception
☐ App stable > 2 minutes
```

---

## 📊 RÉSUMÉ ÉTAPES 1-6

| Étape | Durée | Fichier | Action |
|-------|-------|---------|--------|
| 1 | 30 min | `app/build.gradle.kts` | Remplacer IDs AdMob |
| 2 | 2h | Externe (URL) | Créer Privacy + TOS |
| 3 | 3h | `PlayerActivity.kt` | Ajouter contentDescription |
| 4 | 1h | `PlayerActivity.kt` | Changer Color.Gray → Color.White |
| 5 | 1h | `PlayerActivity.kt` | Wrapper logs de BuildConfig.DEBUG |
| 6 | 1h | Terminal | Compiler + tester |

**TOTAL : 8 heures**

---

## ✅ VALIDATION FINALE (JOUR 2)

Une fois toutes les 6 étapes terminées :

### Checklist Finale

```
☐ IDs AdMob production sont dans build.gradle.kts
☐ Privacy Policy URL est publique et accessible
☐ Terms of Service URL est publique et accessible
☐ Tous les icons ont contentDescription
☐ Tous les textes sont en Color.White (contraste OK)
☐ Tous Log.d() sont entourés de if (BuildConfig.DEBUG)
☐ APK release compile sans erreurs
☐ APK release fonctionne sur device sans crash
☐ Aucun log sensible en logcat
```

### Avant Publication Play Store

Une fois la checklist validée :
1. ✅ Push du code final
2. ✅ Version code/name à jour
3. ✅ Screenshots prêts
4. ✅ Description app rédigée
5. ✅ Content rating sélectionné
6. ✅ Privacy Policy + TOS liées en Play Console

---

## 🚀 PROCHAINE PHASE (JOUR 3-7)

**Soft Launch - Internal Testing**

```
Jour 3 : Upload APK en Internal Testing
Jour 3-7 : Beta testers testent
Jour 7 : Collecte feedback bugs
```

---

**Fin du plan de correction - Étapes 1-6 (Phase 1 Critique)**

À exécuter dans l'ordre, sans sauter d'étape.


