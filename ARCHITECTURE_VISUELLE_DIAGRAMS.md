# 🎯 ARCHITECTURE VISUELLE - STV Player

**Date** : 26/02/2026  
**Format** : Diagrammes et schémas  
**Objectif** : Visualiser les flux et dépendances

---

## 1. ARCHITECTURE GLOBALE

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION STV                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────────────┐         ┌──────────────────────┐          │
│  │   PRESENTATION       │         │    BUSINESS LOGIC    │          │
│  │      LAYER           │         │       LAYER          │          │
│  ├──────────────────────┤         ├──────────────────────┤          │
│  │ MainActivity         │         │ MainViewModel        │          │
│  │ (Écran d'accueil)    │         │ PlayerViewModel      │          │
│  │                      │         │                      │          │
│  │ PlayerActivity       │◄────────│ PlayerController     │          │
│  │ (Lecteur)            │         │ AdManager            │          │
│  │                      │         │ AdsController        │          │
│  │ VideoListActivity    │         │ PermissionHelper     │          │
│  │                      │         │                      │          │
│  │ Composables (UI)     │         │                      │          │
│  │ - MainScreen         │         │                      │          │
│  │ - PlayerScreen       │         │                      │          │
│  │ - VideoListScreen    │         │                      │          │
│  └──────────────────────┘         └──────────────────────┘          │
│           │                                  │                       │
│           └──────────────────┬───────────────┘                       │
│                              │                                       │
│                    ┌─────────▼──────────┐                           │
│                    │  DATA LAYER        │                           │
│                    ├────────────────────┤                           │
│                    │ SharedPreferences  │                           │
│                    │ (AdMob stats)      │                           │
│                    └─────────────────────┘                          │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ EXTERNAL LIBRARIES & SERVICES                                 │  │
│  ├───────────────────────────────────────────────────────────────┤  │
│  │ • ExoPlayer (Media3) - Streaming vidéo                        │  │
│  │ • Jetpack Compose - UI moderne                                │  │
│  │ • Jetpack Navigation - Routing                                │  │
│  │ • Google AdMob - Monétisation                                 │  │
│  │ • Android Lifecycle - Lifecycle awareness                     │  │
│  │ • Kotlin Coroutines - Async/Concurrency                       │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. FLUX D'ACTIVITÉS

### Scénario 1 : Utilisateur ouvre STV et joue une vidéo

```
START
  │
  ├─ Application.onCreate()
  │   └─ MobileAds.initialize()
  │
  ├─ MainActivity.onCreate()
  │   ├─ installSplashScreen()
  │   ├─ setContent { MainScreen() }
  │   ├─ adManager = AdManager(this)
  │   └─ adManager.loadInterstitialAd()
  │       └─ (Charge une pub en arrière-plan)
  │
  ├─ MainScreen affichée
  │   ├─ TopAppBar avec menu
  │   ├─ Liste vidéos OU champ URL personnalisée
  │   └─ Bouton "Lire"
  │
  ├─ Utilisateur saisit/sélectionne une URL
  │   └─ MainViewModel.updateUrl(url)
  │
  ├─ Utilisateur clique "Lire"
  │   ├─ MainViewModel.validateUrl()
  │   ├─ Intent créé
  │   └─ startActivity(PlayerActivity)
  │
  ├─ PlayerActivity.onCreate()
  │   ├─ hideSystemUI() ← Mode plein écran
  │   ├─ resolveVideoUrl(intent) ← Récupère URL
  │   ├─ PlayerController.validateStreamUrl(url)
  │   ├─ setContent { PlayerScreen() }
  │   │   └─ PlayerViewModel initialisé
  │   │
  │   ├─ AdsController.showAdIfNeeded()
  │   │   ├─ (Timeout 6 secondes)
  │   │   ├─ AdManager.loadAndShowInterstitial()
  │   │   └─ Retour AdResult (AdShowed, Timeout, etc)
  │   │
  │   └─ PlayerViewModel.initializePlayer(url)
  │       ├─ Crée ExoPlayer
  │       ├─ Config qualité auto
  │       ├─ Charge vidéo
  │       └─ Observe play events
  │
  ├─ PlayerScreen affichée
  │   ├─ PlayerView (ExoPlayer)
  │   ├─ Contrôles (play, pause, volume)
  │   ├─ Barre de progression
  │   └─ Menu qualité/sous-titres
  │
  ├─ Utilisateur regarde la vidéo
  │   ├─ PlayerViewModel observe les événements
  │   ├─ Mise à jour position
  │   └─ Recomposition Compose automatique
  │
  └─ Utilisateur ferme l'app
      ├─ PlayerActivity.onDestroy()
      ├─ PlayerViewModel.releasePlayer()
      ├─ ExoPlayer cleanup
      └─ END
```

---

### Scénario 2 : SoukiTV appelle STV directement

```
SoukiTV App
  │
  ├─ Intent crée
  │   ├─ Action: "com.example.stv.action.PLAY_STREAM"
  │   ├─ Package: "com.example.stv" (FORCE)
  │   └─ Extra "VIDEO_URL": "https://..."
  │
  ├─ SoukiTV.startActivity(intent)
  │
  └─► Android résout l'intent
      └─ PlayerActivity lancée directement
          └─ Même flux que scénario 1 (à partir de PlayerActivity)
```

---

### Scénario 3 : Deep Link

```
Navigateur/SMS/Email
  │
  ├─ Lien: "stv://play?url=https://example.com/stream.m3u8"
  │
  └─► Android résout le deep link
      └─ MainActivity reçoit l'intent
          ├─ Parse intent data
          ├─ Extrait URL du paramètre "url"
          └─ startActivity(PlayerActivity avec URL)
```

---

## 3. HIÉRARCHIE DES COMPOSABLES

```
STVTheme (Material 3 Colors & Typography)
│
├─ MainActivity
│  │
│  └─ MainScreen()
│     │
│     ├─ Scaffold
│     │  │
│     │  ├─ TopAppBar()
│     │  │  ├─ Title "STV"
│     │  │  └─ MenuButton (opens drawer)
│     │  │
│     │  ├─ SnackbarHost()
│     │  │  └─ (Notifications)
│     │  │
│     │  └─ ModalNavigationDrawer()
│     │     │
│     │     ├─ ModalDrawerSheet()
│     │     │  ├─ NavigationDrawerItem()
│     │     │  │  ├─ "Politique de Confidentialité"
│     │     │  │  │   └─ → PrivacyPolicyActivity
│     │     │  │  │
│     │     │  │  └─ "Conditions d'Utilisation"
│     │     │  │      └─ → TermsOfServiceActivity
│     │     │  │
│     │     │  └─ Divider()
│     │     │
│     │     └─ MainScreenContent()
│     │        │
│     │        ├─ IF (urlError) → ErrorText()
│     │        │
│     │        ├─ URL Input Field
│     │        │   └─ onValueChange → MainViewModel.updateUrl()
│     │        │
│     │        ├─ Play Button
│     │        │   └─ onClick → Valide & Lance PlayerActivity
│     │        │
│     │        └─ Video List (LazyColumn)
│     │            └─ VideoItem() × N
│     │               └─ onClick → Lance PlayerActivity(video.url)
│     │
│     └─ ConnectivityStatusBar()
│        └─ "Internet: OK / Pas de connexion"
│
└─ PlayerActivity
   │
   └─ PlayerScreen()
      │
      └─ Surface (Color.Black)
         │
         ├─ Box (fullScreen)
         │  │
         │  ├─ AndroidView
         │  │  └─ PlayerView (ExoPlayer)
         │  │     └─ Lecteur vidéo natif
         │  │
         │  ├─ Column (Controls Overlay)
         │  │  │
         │  │  ├─ TopAppBar (Titre)
         │  │  │  └─ Back Button
         │  │  │
         │  │  ├─ Spacer()
         │  │  │
         │  │  ├─ PlayerControls()
         │  │  │  ├─ PlayPauseButton()
         │  │  │  ├─ VolumeSlider()
         │  │  │  ├─ QualitySelector()
         │  │  │  ├─ SubtitleMenu()
         │  │  │  └─ PiPButton()
         │  │  │
         │  │  ├─ ProgressBar()
         │  │  │  ├─ Slider (Position)
         │  │  │  ├─ CurrentTime
         │  │  │  └─ Duration
         │  │  │
         │  │  └─ SpeedSelector() (Optional)
         │  │
         │  ├─ LoadingIndicator()
         │  │  └─ (Visible si isLoading=true)
         │  │
         │  ├─ ErrorDialog()
         │  │  └─ (Visible si error)
         │  │
         │  ├─ QualityMenuDropdown()
         │  │  ├─ "720p" → Track(0)
         │  │  ├─ "1080p" → Track(1)
         │  │  └─ "Auto" → Auto selection
         │  │
         │  └─ SubtitleMenuDropdown()
         │     ├─ "Sans sous-titres"
         │     ├─ "Français"
         │     ├─ "English"
         │     └─ ...
         │
         └─ PiPMode Composable
            └─ (Léger si déjà en PiP)
```

---

## 4. ÉTAT ET FLUX DE DONNÉES (MVVM + StateFlow)

```
┌────────────────────────────────────────────────────────────┐
│              MVVM STATE MANAGEMENT FLOW                     │
└────────────────────────────────────────────────────────────┘

MainViewModel (Écran d'accueil)
├─ _streamUrl: MutableStateFlow<String>
│   └─ streamUrl: StateFlow<String> (readonly)
│       ↓ (utilisateur change l'URL)
│       Composable recompose automatiquement
│
└─ _isError: MutableStateFlow<Boolean>
    └─ isError: StateFlow<Boolean>
        └─ (affiche message erreur si true)

PlayerViewModel (Lecteur)
├─ _exoPlayer: ExoPlayer?
│   └─ exoPlayer: ExoPlayer?
│       ├─ Init → setMediaItem()
│       ├─ Play → player.play()
│       └─ Dispose → player.release()
│
├─ _isPlaying: MutableStateFlow<Boolean>
│   └─ Observe player.isPlaying
│       └─ PlayButton change d'icône
│
├─ _currentPosition: MutableStateFlow<Long>
│   └─ Observe player.currentPosition
│       └─ ProgressBar update
│
├─ _duration: MutableStateFlow<Long>
│   └─ Observe player.duration
│       └─ TimeLabel update
│
├─ _isLoading: MutableStateFlow<Boolean>
│   └─ Observe player.isLoading
│       └─ LoadingSpinner visibility
│
└─ _errorMessage: MutableStateFlow<String?>
    └─ Observe player.playerError
        └─ ErrorDialog affichée si non-null

Data Flow:
┌──────────────────────────────────────┐
│  User Interaction (UI Event)         │
│  - Click play button                 │
│  - Seek to position                  │
│  - Select quality                    │
└──────────────────┬───────────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │  ViewModel Function │
         │  - playVideo()      │
         │  - seekTo(pos)      │
         │  - selectTrack()    │
         └────────┬────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │  Update StateFlow   │
         │  _isPlaying.value   │
         │  _currentPosition   │
         │  _selectedTrack     │
         └────────┬────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │  Composable collect │
         │  (observe changes)  │
         └────────┬────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │  Recomposition &    │
         │  UI Update          │
         └─────────────────────┘
```

---

## 5. CYCLE DE VIE DES COMPOSANTS

### Android Activity Lifecycle (PlayerActivity)

```
onCreate()
  ├─ hideSystemUI()
  ├─ AdManager.init()
  ├─ setContent { PlayerScreen() }
  └─ ViewModel créé
      │
      ▼
onStart()
  ├─ PlayerView attache à décor
  └─ ExoPlayer devient visible
      │
      ▼
onResume()
  ├─ ExoPlayer.play()
  ├─ Window inserts config
  └─ Lecteur en avant-plan
      │
      ▼
  [UTILISATEUR UTILISE L'APP]
      │
      ├─ Pause (home button)
      │  │
      │  ▼
      │ onPause()
      │  ├─ ExoPlayer.pause()
      │  └─ Sauvegarde position
      │      │
      │      ▼
      │  onResume()
      │   └─ ExoPlayer.play()
      │      └─ Reprend lecture
      │
      └─ Ferme l'activité
          │
          ▼
       onStop()
          └─ Cleanup commenced
              │
              ▼
       onDestroy()
          ├─ PlayerViewModel.releasePlayer()
          ├─ ExoPlayer.release()
          └─ Ressources nettoyées

Composable Lifecycle (Compose):
composition() → initial composition
  │
  ├─ LaunchedEffect hooks
  ├─ State initialization
  └─ Initial render
      │
      ▼
recomposition() → StateFlow changes
  ├─ Collect { newValue }
  └─ Re-render
      │
      └─ (repeat on state change)
          │
          ▼
disposal() → Activity destroyed
  ├─ Cancel LaunchedEffect
  ├─ Cleanup observers
  └─ Release resources
```

---

## 6. DIAGRAMME DE SÉQUENCE : Lecture d'une vidéo

```
Utilisateur          MainActivity      PlayerActivity      PlayerViewModel    ExoPlayer      AdMob
    │                    │                   │                  │               │              │
    │ Clique "Lire"      │                   │                  │               │              │
    ├───────────────────>│                   │                  │               │              │
    │                    │ startActivity()   │                  │               │              │
    │                    ├──────────────────>│                  │               │              │
    │                    │                   │ onCreate()       │               │              │
    │                    │                   ├─────────────────>│               │              │
    │                    │                   │                  │ init()        │              │
    │                    │                   │                  ├─────────────>│              │
    │                    │                   │<─────────────────┤               │              │
    │                    │                   │                  │               │              │
    │                    │                   │ Valide URL       │               │              │
    │                    │                   ├─────────────────>│               │              │
    │                    │                   │<─────────────────┤               │              │
    │                    │                   │                  │               │              │
    │                    │                   │ showAdIfNeeded() │               │              │
    │                    │                   ├────────────────────────────────────────────────>│
    │                    │                   │                  │               │              │ load()
    │                    │                   │                  │               │              │
    │    [Timeout 6s]    │                   │                  │               │              │
    │                    │                   │<────────────────────────────────────────────────┤
    │                    │                   │ (onAdLoaded / Timeout)          │              │
    │                    │                   │                  │               │              │
    │                    │                   │ setMediaItem()   │               │              │
    │                    │                   ├─────────────────>│               │              │
    │                    │                   │                  ├─────────────>│              │
    │                    │                   │<─────────────────┤               │              │
    │                    │                   │<─────────────────────────────────┤              │
    │                    │                   │ (onPlayWhenReady, isLoading)     │              │
    │                    │                   │                  │               │              │
    │                    │                   │ collectAsState() │               │              │
    │                    │                   │ & Recompose      │               │              │
    │                    │                   │                  │               │              │
    │    Lecteur affiché │                   │                  │               │              │
    │<────────────────────────────────────────────────────────────────────────────────────────┤
    │                    │                   │                  │               │              │
    │    [Vidéo joue]    │                   │ Observer events  │               │              │
    │                    │                   ├─────────────────>│<──────────────┤              │
    │                    │                   │ UpdateStateFlow  │               │              │
    │                    │                   │<─────────────────┤               │              │
    │                    │                   │ Recompose UI     │               │              │
    │                    │                   │                  │               │              │
    │    Clique pause    │                   │                  │               │              │
    ├───────────────────────────────────────>│                  │               │              │
    │                    │                   ├─────────────────>│               │              │
    │                    │                   │                  ├─────────────>│ pause()      │
    │                    │                   │                  │<──────────────┤              │
    │                    │                   │<─────────────────┤               │              │
    │    Lecteur paused  │                   │                  │               │              │
    │<────────────────────────────────────────────────────────────────────────────────────────┤
    │                    │                   │                  │               │              │
    │    Ferme l'app     │                   │                  │               │              │
    ├───────────────────────────────────────>│ onDestroy()      │               │              │
    │                    │                   ├─────────────────>│               │              │
    │                    │                   │                  │ release()     │              │
    │                    │                   │                  ├─────────────>│ release()    │
    │                    │                   │                  │<──────────────┤              │
    │                    │                   │<─────────────────┤               │              │
    │    App fermée      │                   │                  │               │              │
    │                    │                   │                  │               │              │
```

---

## 7. STRUCTURE DES DONNÉES

```
┌─────────────────────────────────────────────────────────┐
│               MODÈLES DE DONNÉES                         │
└─────────────────────────────────────────────────────────┘

VideoItem
├─ title: String         ("Sky News Arabia")
└─ url: String           ("https://...")

PlayerUiState
├─ Loading()             (initial)
├─ Error(message: String)
├─ Ready(                (normal play)
│   exoPlayer: ExoPlayer
│   position: Long
│   duration: Long
│   isPlaying: Boolean
│   selectedQuality: Track?
│   selectedSubtitle: Track?
│   isControlsVisible: Boolean
│   pipMode: Boolean
│  )
└─ AdBlock()             (ad blocker detected)

AdsController.AdResult
├─ AdShowed
├─ AdDismissed
├─ FallbackBanner
├─ Timeout
└─ AdBlockDetected

Track (Media3)
├─ trackId: String
├─ mimeType: String      ("video/avc", "audio/mp4a-latm")
├─ language: String      ("fr", "en")
├─ channelCount: Int
├─ sampleRate: Int
└─ bitrate: Int

MediaItem (Media3)
├─ mediaId: String
├─ mediaMetadata
│   ├─ title
│   ├─ subtitle
│   ├─ artworkUri
│   └─ ...
├─ localConfiguration: LocalConfiguration?
│   ├─ uri: Uri
│   ├─ mimeType: String  ("application/x-mpegurl", "video/mp4")
│   ├─ advertiserInfo: AdvertiserInfo?
│   └─ ...
└─ ...
```

---

## 8. ARBRE DES PERMISSIONS

```
AndroidManifest.xml
│
├─ <uses-permission>
│  ├─ android.permission.INTERNET
│  │   └─ Requis pour: Streaming vidéo, Chargement ads
│  │
│  └─ android.permission.ACCESS_NETWORK_STATE
│      └─ Requis pour: Vérifier connectivité
│
├─ <application>
│  │
│  ├─ AdMob Meta-data
│  │  └─ com.google.android.gms.ads.APPLICATION_ID
│  │     └─ "ca-app-pub-3940256099942544~3347511713"
│  │
│  ├─ <activity android:name=".MainActivity">
│  │  ├─ android:exported="true"
│  │  ├─ android:theme="@style/Theme.STV"
│  │  │
│  │  └─ <intent-filter>
│  │     ├─ <action android:name="android.intent.action.MAIN" />
│  │     └─ <category android:name="android.intent.category.LAUNCHER" />
│  │
│  ├─ <activity android:name=".PlayerActivity">
│  │  ├─ android:exported="true"
│  │  ├─ android:screenOrientation="sensorLandscape"
│  │  ├─ android:supportsPictureInPicture="true"
│  │  │
│  │  └─ <intent-filter> × 4
│  │     ├─ Filter 1: Action custom (SoukiTV)
│  │     ├─ Filter 2: Deep Link (stv://)
│  │     ├─ Filter 3: HTTP + MIME (streaming)
│  │     └─ Filter 4: Vidéo générique (video/*)
│  │
│  └─ <activity android:name=".PrivacyPolicyActivity">
│     └─ android:exported="false"
│
└─ Network Security Config (@xml/network_security_config)
   ├─ <domain-config cleartextTrafficPermitted="true">
   │  └─ Permet HTTP (pour certains flux)
   │
   └─ <pin-set>
      └─ (Optional: pinning de certificats)
```

---

## 9. CACHE ET ÉTAT PERSISTANT

```
┌───────────────────────────────────────┐
│      PERSISTENT DATA STORAGE           │
└───────────────────────────────────────┘

SharedPreferences ("ad_prefs")
├─ failed_attempts: Int
│   ├─ Incrémenté à chaque échec ad
│   └─ Reset si succès
│
└─ (Extensible pour):
   ├─ user_quality_preference
   ├─ last_played_position
   ├─ user_language
   └─ ...

ExoPlayer Cache (Optionnel)
├─ Buffering temporaire
├─ Segment cache (HLS/DASH)
└─ [Non utilisé actuellement]

Compose State
├─ Remember {} → Local state (mort avec composition)
├─ RememberSaveable {} → Survive config change
└─ ViewModel StateFlow → Survive activity recreation
```

---

## 10. DÉPENDANCES ENTRE MODULES

```
            ┌──────────────────────┐
            │  app/build.gradle    │
            └──────────┬───────────┘
                       │
     ┌─────────────────┼─────────────────┐
     │                 │                 │
     ▼                 ▼                 ▼
  Kotlin          Android Core      Compose
  Coroutines      Libraries         Framework
     │                 │                 │
     │                 │                 │
     ├─────────────────┼─────────────────┤
     │                                    │
     ▼                                    ▼
 Jetpack                           Material 3
 Navigation                        Design System
     │                                    │
     │                                    │
     ├────────────────┬────────────────────┤
     │                │                    │
     ▼                ▼                    ▼
 Media3           Google Play      ExoPlayer
(Streaming)       Services (Ads)    (Video)
     │                │                    │
     └────────────────┼────────────────────┘
                      │
              ┌───────▼────────┐
              │   STV APP      │
              │   (Integrated) │
              └────────────────┘
```

---

## 11. CONFIGURATION PAR FLAVOR

```
productFlavors {
    │
    ├─ "dev" (Développement)
    │  ├─ applicationIdSuffix = ".dev"
    │  │  └─ Package: "com.example.stv.dev"
    │  │
    │  ├─ ADMOB_INTERSTITIAL_ID = Test ID
    │  ├─ ADMOB_BANNER_ID = Test ID
    │  │  └─ Pas de revenus, mais pubs testées
    │  │
    │  └─ BuildConfig.DEBUG = true
    │     └─ Logs détaillés
    │
    └─ "prod" (Production)
       ├─ applicationIdSuffix = "" (none)
       │  └─ Package: "com.example.stv"
       │
       ├─ ADMOB_INTERSTITIAL_ID = Production ID (à configurer)
       ├─ ADMOB_BANNER_ID = Production ID (à configurer)
       │  └─ Génère des revenus
       │
       └─ BuildConfig.DEBUG = false
          └─ Logs minimisés
```

---

## 12. GRAPH DES RESPONSABILITÉS

```
                    MainActivity
                    /          \
                   /            \
              MainScreen      MainViewModel
                   │              │
                   │              └─ Gère streamUrl
                   │                      │
                   │                      ▼
                   │              URL Validation
                   │                      │
                   │                      ▼
                   └─────────────> startActivity()
                                       │
                                       ▼
                                 PlayerActivity
                                 /      |       \
                                /       |        \
                   PlayerScreen    PlayerView   PlayerViewModel
                       │              │             │
                       │              │             ├─ ExoPlayer instance
                       │              │             ├─ Play state
                       │              │             ├─ Position tracking
                       │              │             └─ Error handling
                       │              │
                       │              ├─ Media3 UI
                       │              ├─ Controls rendering
                       │              └─ Video display
                       │
                       ├──────────────> AdsController
                       │                     │
                       │                     ├─ showAdIfNeeded()
                       │                     └─ Timeout logic
                       │
                       ├──────────────> AdManager
                       │                     │
                       │                     ├─ Load interstitial
                       │                     ├─ Show/dismiss
                       │                     └─ Error handling
                       │
                       └──────────────> PlayerController
                                             │
                                             └─ URL validation
```

---

## 13. ÉTATS POSSIBLES DE L'INTERFACE

```
BEFORE PLAYING
│
├─ [LOADING] State
│  ├─ Spinner rotatif
│  ├─ "Chargement..."
│  └─ Pas de contrôles
│
├─ [READY] State
│  ├─ PlayerView visible
│  ├─ Contrôles disponibles
│  ├─ PlayButton ready
│  └─ Slider enabled
│
├─ [ERROR] State
│  ├─ Dialog overlay
│  ├─ "Erreur: [message]"
│  ├─ Bouton retry
│  └─ Bouton close
│
└─ [ADBLOCK] State
   ├─ Dialog strict
   ├─ "Publi bloquée"
   ├─ Bouton "Continuer"
   └─ Bouton "Quitter"

DURING PLAYING
│
├─ [PLAYING] State
│  ├─ PlayerView en lecture
│  ├─ PlayPause → "▮▮" icon
│  ├─ Position updating
│  └─ Auto-hide controls après 3s
│
├─ [PAUSED] State
│  ├─ PlayerView gelée
│  ├─ PlayPause → "▶" icon
│  ├─ Position fixed
│  └─ Controls always visible
│
└─ [ENDED] State
   ├─ PlayerView dernier frame
   ├─ PlayButton clickable
   └─ Option replay video

CONFIGURATIONS
│
├─ [NORMAL] Mode
│  └─ Plein écran portrait/paysage
│
├─ [PIP] Mode
│  ├─ Fenêtre flottante
│  ├─ App peut continuer en bg
│  └─ Retour full-screen possible
│
└─ [MINIMIZE] Mode
   ├─ Controls minimisés
   ├─ Petite UI
   └─ Moins de ressources
```

---

**Fin des diagrammes d'architecture ✅**  
**Documenté et visualisé entièrement** 🎯

