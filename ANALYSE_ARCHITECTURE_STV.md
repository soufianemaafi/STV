# 📋 ANALYSE COMPLÈTE - Application STV

**Date** : 26/02/2026  
**Statut** : Analyse terminée  
**Objectif** : Comprendre le fonctionnement de STV et être prêt pour vos demandes

---

## 🏗️ ARCHITECTURE GLOBALE

### 1. Vue d'ensemble

STV est une **application Android de streaming vidéo** construite avec :
- **Kotlin** + **Jetpack Compose** (UI moderne)
- **ExoPlayer** (lecteur vidéo haute performance)
- **AdMob** (monétisation par publicités)
- **Architecture MVVM** avec ViewModel et StateFlow

### 2. Flux principal

```
Utilisateur ouvre STV
    ↓
MainActivity (écran d'accueil)
    ↓
Saisit/Sélectionne URL vidéo
    ↓
Clique "Lire"
    ↓
PlayerActivity (lecteur)
    ↓
Affiche ad interstitielle (AdMob)
    ↓
Lance ExoPlayer pour streaming
    ↓
Dispose du player à la fermeture
```

---

## 📱 ACTIVITÉS PRINCIPALES

### A. MainActivity
**Rôle** : Écran d'accueil et gestion des flux

**Responsabilités** :
- Affiche une liste de vidéos pré-configurées
- Permet à l'utilisateur d'entrer une URL personnalisée
- Menu latéral (drawer) avec liens utiles :
  - Politique de confidentialité
  - Conditions d'utilisation
- Gère la connexion internet (vérification)
- Initialise AdMob au démarrage

**StateFlow utilisé** :
- `streamUrl` : L'URL actuelle
- `isError` : État de validation URL

**Intent Filters (dans AndroidManifest.xml)** :
```xml
<!-- 1. Action personnalisée pour SoukiTV -->
<action android:name="com.example.stv.action.PLAY_STREAM" />

<!-- 2. Deep Link public (stv://play?url=...) -->
<data android:scheme="stv" android:host="play" />
```

---

### B. PlayerActivity
**Rôle** : Cœur de l'application - lecteur vidéo

**Responsabilités** :
1. **Initialisation du lecteur** :
   - Reçoit l'URL via Intent
   - Valide l'URL avec PlayerController
   - Crée une instance ExoPlayer

2. **Gestion des annonces** :
   - Charge une annonce interstitielle AVANT le démarrage
   - Gère les timeouts (6 secondes)
   - Fallback vers bannière si pas d'ad dispo
   - Détecte les bloqueurs d'ads

3. **Interface utilisateur** :
   - Lecteur vidéo en plein écran
   - Contrôles : play/pause, volume, sous-titres
   - Barre de progression
   - Menu de sélection de piste audio/sous-titres

4. **Modes d'écran** :
   - Paysage obligatoire (screenOrientation="sensorLandscape")
   - Support Picture-in-Picture (PiP)

**Intégrations** :
- `AdManager` : Gestion des annonces
- `AdsController` : Logique de timeout/fallback
- `PlayerController` : Validation d'URL
- `PlayerViewModel` : État du lecteur

---

### C. VideoListActivity
**Rôle** : Affiche une liste de vidéos

**Responsabilités** :
- Affiche une liste de vidéos (titre + URL)
- Permet de sélectionner une vidéo à lire

---

### D. Activités Utilitaires
- **AddVideoActivity** : Ajouter une nouvelle vidéo à la liste
- **PrivacyPolicyActivity** : Affiche la politique de confidentialité
- **TermsOfServiceActivity** : Affiche les conditions d'utilisation

---

## 🎬 COMPOSANTS CLÉS

### 1. PlayerViewModel
**Objectif** : Gère l'état du lecteur

**StateFlows** :
- `exoPlayer` : Instance du lecteur
- `isPlaying` : En lecture ?
- `currentPosition` : Position actuelle (ms)
- `bufferedPosition` : Jusqu'où est mis en cache
- `duration` : Durée totale
- `isLoading` : En train de charger ?
- `errorMessage` : Message d'erreur

**Fonctions principales** :
- `initializePlayer(url)` : Crée et configure ExoPlayer
- `playVideo()` : Lance la lecture
- `pauseVideo()` : Met en pause
- `seekTo(position)` : Saute à une position
- `releasePlayer()` : Nettoie les ressources

---

### 2. AdManager
**Objectif** : Gère le cycle de vie des annonces

**Responsabilités** :
1. **Chargement d'annonces** :
   - Charge une annonce interstitielle
   - Utilise les IDs AdMob du BuildConfig
   - Gère les erreurs de chargement

2. **Détection de bloqueurs** :
   - Compte les tentatives échouées (max 3)
   - Affiche un dialogue strict si AdBlock détecté
   - Permet à l'utilisateur de continuer ou de quitter

3. **Fallback** :
   - Si interstitielle échoue → affiche bannière
   - Logique "soft failover" : pas d'interruption utilisateur

**Configuration** :
```kotlin
// IDs test AdMob (pour dev/prod)
ADMOB_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
```

---

### 3. AdsController
**Objectif** : Logique métier des annonces

**Suspension avec timeout** :
```kotlin
suspend fun showAdIfNeeded(
    activity: Activity,
    timeoutMs: Long = 6000  // ← 6 secondes max
): AdResult
```

**Résultats possibles** :
- `AdShowed` : Annonce affichée
- `AdDismissed` : Fermée par l'utilisateur
- `FallbackBanner` : Fallback lancé
- `Timeout` : Timeout après 6s
- `AdBlockDetected` : Bloqueur détecté

---

### 4. PlayerController
**Objectif** : Valide les URLs de flux

**Validations** :
```kotlin
fun validateStreamUrl(url: String?): String?
// Retourne null si valide, sinon le message d'erreur

Vérifications :
- ✓ Non null/vide
- ✓ Commence par http:// ou https://
- ✓ Max 2048 caractères
- ✓ Format URL valide (regex Android)
```

---

### 5. PermissionHelper
**Objectif** : Sécurise l'accès aux activités exportées

**Fonctionnalités** :
- Vérifie les permissions de l'app appelante
- Valide la signature de l'app
- Empêche l'accès non autorisé

---

## 🔗 INTENT FILTERS & DEEP LINKS

### Tous les Intent Filters configurés

#### 1. Action personnalisée (pour SoukiTV)
```xml
<intent-filter>
    <action android:name="com.example.stv.action.PLAY_STREAM" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

**Utilisation** :
```kotlin
Intent("com.example.stv.action.PLAY_STREAM").apply {
    setPackage("com.example.stv")  // Force STV
    putExtra("VIDEO_URL", "https://...")
}
startActivity(intent)
```

#### 2. Deep Link public
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <data android:scheme="stv" android:host="play" />
</intent-filter>
```

**Utilisation** :
```
stv://play?url=https://example.com/stream.m3u8
```

#### 3. Fichiers vidéo (HTTP/HTTPS + MIME types)
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <data android:scheme="http" />
    <data android:scheme="https" />
    <data android:mimeType="application/vnd.apple.mpegurl" />  <!-- .m3u8 -->
    <data android:mimeType="application/x-mpegurl" />
</intent-filter>
```

#### 4. Tous types vidéo
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <data android:scheme="http" />
    <data android:scheme="https" />
    <data android:mimeType="video/*" />
</intent-filter>
```

**Résultat** : STV apparaît dans les "chooser" comme VLC, MX Player, etc.

---

## 🎨 ARCHITECTURE UI

### Compose Structure

```
STVTheme (Material Design 3)
├── MainActivity
│   ├── MainScreen
│   │   ├── TopAppBar
│   │   ├── NavigationDrawer
│   │   │   ├── PrivacyPolicyItem
│   │   │   └── TermsOfServiceItem
│   │   └── VideoListOrInputForm
│   └── StatusBar (ConnectivityStatus)
│
└── PlayerActivity
    ├── PlayerView (ExoPlayer)
    ├── PlayerControls
    │   ├── PlayPauseButton
    │   ├── VolumeSlider
    │   ├── QualitySelector
    │   └── SubtitleMenu
    ├── ProgressBar
    └── PiP Controls
```

### Thème
- **Material Design 3**
- Couleurs personnalisées via `Color.kt`
- Support Mode Sombre/Clair

---

## 📦 DÉPENDANCES PRINCIPALES

```kotlin
// Jetpack
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-ktx
androidx.activity:activity-compose
androidx.navigation:navigation-compose

// Compose UI
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.compose.material:material-icons-extended

// Lecteur vidéo
androidx.media3:media3-exoplayer
androidx.media3:media3-exoplayer-dash
androidx.media3:media3-exoplayer-hls
androidx.media3:media3-exoplayer-rtsp
androidx.media3:media3-exoplayer-smoothstreaming
androidx.media3:media3-ui

// Monétisation
com.google.android.gms:play-services-ads:23.0.0

// Splash Screen
androidx.core:core-splashscreen:1.0.1
```

---

## 🛡️ SÉCURITÉ

### Permissions requises
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Configuration réseau
```xml
android:networkSecurityConfig="@xml/network_security_config"
android:usesCleartextTraffic="true"  <!-- Permet HTTP (streaming) -->
android:hardwareAccelerated="true"   <!-- Accélération GPU -->
```

### Protection des activités
- PlayerActivity exportée (intent filters)
- Autres activités privées (exported="false")
- PermissionHelper valide les appelants

---

## 💰 MONÉTISATION AdMob

### Configuration par flavor

```kotlin
flavorDimensions.add("environment")

create("dev") {
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
        "\"ca-app-pub-3940256099942544/1033173712\"")  // Test ID
}

create("prod") {
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
        "\"ca-app-pub-3940256099942544/1033173712\"")  // À remplacer
}
```

### Comportement des annonces

1. **Au lancement de PlayerActivity** :
   - Affiche immédiatement une annonce interstitielle
   - Timeout 6 secondes
   - Si echec → annonce bannière

2. **Si AdBlock détecté** :
   - Affiche dialogue bloquant
   - Permet continuer ou quitter
   - Enregistre les tentatives échouées

3. **Statistiques** :
   - SharedPreferences sauvegarde les compteurs
   - Persist entre sessions

---

## 🔄 FLUX DE DONNÉES (MVVM)

```
UI (Composable)
    ↓
ViewModel (StateFlow)
    ↓ collect()
State ← Updates réactifs
    ↓
UI re-compose automatically
```

**Exemple** :
```kotlin
// Dans PlayerViewModel
private val _isPlaying = MutableStateFlow(false)
val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

// Dans PlayerActivity (Composable)
val isPlaying by viewModel.isPlaying.collectAsState()
PlayPauseButton(isPlaying = isPlaying)
```

---

## 🚀 CHEMINS DE CODE PRINCIPAUX

### Scénario 1 : Lire une URL depuis MainActivity
```
MainActivity.onCreate()
  ├─ setContent { MainScreen() }
  └─ Utilisateur clique "Lire"
      ├─ Valide URL (MainViewModel)
      └─ startActivity(Intent(PlayerActivity))
          └─ PlayerActivity.onCreate()
              ├─ Valide URL (PlayerController)
              ├─ Init AdManager
              └─ setContent { PlayerScreen() }
                  ├─ Affiche ad (AdsController)
                  └─ Init ExoPlayer (PlayerViewModel)
```

### Scénario 2 : SoukiTV appelle STV
```
SoukiTV app
  ├─ Intent("com.example.stv.action.PLAY_STREAM")
  ├─ setPackage("com.example.stv")
  ├─ putExtra("VIDEO_URL", url)
  └─ startActivity()
      └─ PlayerActivity lancée directement
          ├─ resolveVideoUrl() récupère l'URL
          ├─ Valide + Lance ExoPlayer
          └─ Affiche ad si besoin
```

### Scénario 3 : Deep Link
```
Navigateur/App externe
  ├─ Accès stv://play?url=https://...
  └─ Android résout le deep link
      └─ MainActivity recoit intent
          ├─ Parse l'URL depuis le scheme
          └─ Lance PlayerActivity
```

---

## 📊 QUALITÉ DE LECTURE

### Formats supportés
- **HLS** (.m3u8)
- **DASH** (.mpd)
- **MP4**, **MKV**, etc.
- **Smooth Streaming**
- **RTSP**

### Sélection de qualité
- Interface de sélection de piste
- Adaptation automatique (ExoPlayer)
- Affichage des paramètres :
  - Résolution
  - Bitrate
  - Fréquence audio

### Sous-titres
- Détection automatique
- Menu de sélection
- Formats : WebVTT, SRT, etc.

---

## 🔧 CONFIGURATION BUILD

### Gradle Structure
```
app/build.gradle.kts
├── plugins (Android, Kotlin, Compose)
├── android block
│   ├── namespace, compileSdk, minSdk
│   ├── productFlavors (dev, prod)
│   ├── signingConfigs (release.keystore)
│   └── buildTypes (release avec ProGuard)
└── dependencies
```

### Flavors
- **dev** : IDs AdMob test, suffix ".dev"
- **prod** : IDs AdMob production (à configurer)

### ProGuard/R8
- Activé pour release
- Minification du code
- Réduction des ressources

---

## ✅ POINTS CLÉS À RETENIR

1. **STV est un lecteur vidéo standalone** avec :
   - Support multiple formats
   - Monétisation AdMob
   - UI moderne Compose + Material 3

2. **Ouvert à d'autres apps** via :
   - Intent Filters explicites
   - Deep links
   - Supporté comme VLC/MX Player

3. **Architecture MVVM** :
   - ViewModel gère l'état
   - StateFlow pour réactivité
   - Composables pour UI

4. **Sécurisé** :
   - Validation URLs
   - Vérification permissions
   - Pas de fuites mémoire (appContext)

5. **Monétisation intégrée** :
   - AdMob avec timeout
   - Fallback bannière
   - Détection AdBlock

6. **Prêt pour PlayStore** :
   - Configuration flavors
   - Signing release
   - ProGuard activé

---

## 📝 PROCHAINES ÉTAPES

**Je suis prêt pour vos demandes. Vous pouvez me demander :**

- ✅ Ajouter des features
- ✅ Corriger des bugs
- ✅ Modifier la monétisation AdMob
- ✅ Personnaliser l'UI
- ✅ Intégrer d'autres services
- ✅ Optimiser les performances
- ✅ Changer la configuration
- ✅ Ajouter d'autres activités
- ✅ Etc.

**Format de vos demandes** :
```
DEMANDE : [description claire]
DÉTAILS : [spécifications techniques si besoin]
PRIORITÉ : [basse/moyenne/haute]
DEADLINE : [date si applicable]
```

---

**Analyse terminée ✅**  
**Prêt à vous aider !** 🚀

