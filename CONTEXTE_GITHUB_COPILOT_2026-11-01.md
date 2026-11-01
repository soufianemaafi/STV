# 📋 CONTEXTE PROJET GITHUB COPILOT — 01 NOVEMBRE 2026 (V3.0)

> **Projet** : STV Ecosystem Monorepo  
> **Repository** : `D:\STV`  
> **Date de mise à jour** : 01 Novembre 2026  
> **Version Global Architecture** : V3.0 Clean Architecture + MVI  
> **Git Release Tag** : `v3.0.0-soukitv-offline-resilience` (Novembre 2026)

---

## 1. 🏗️ ARCHITECTURE NATIVE & MOYENS D'ACTION

Le projet est un monorepo Android construit sous **Gradle 9.2.1 / AGP 9.0.1** avec **Kotlin 2.3.10** et **Target/Compile SDK 36**. Il se divise en deux modules Android distincts :

1. **`:app` (Lecteur vidéo / Le Moteur)** :
   - **Application ID** : `com.soufcom.stv`
   - **Rôle** : Lecteur multimédia autonome haute performance (Media3 ExoPlayer 1.9.2), monétisé avec AdMob Interstitial (v25.0.0), publié sur le Google Play Store.
   - **Architecture** : Clean Architecture + MVI (StateFlow / UiAction / UiState).
   - **Flavors** : `dev` (`com.soufcom.stv.dev`) et `prod` (`com.soufcom.stv`).

2. **`:soukitv` (Catalogue de streaming / La Vitrine)** :
   - **Application ID** : `com.example.soukitv`
   - **Rôle** : Application catalogue interactive hors-store (Retrofit 3.0.0, Coil 2.7.0), affichant les matchs en direct, les chaînes IPTV par pays/catégorie et déléguant la lecture vidéo au module `:app` via IPC sécurisé.

---

## 2. 📂 ARBORESCENCE DÉTAILLÉE DES MODULES

### Module `:app` (`com.stv.videoplayer`)
```
app/src/main/java/com/stv/videoplayer/
├── STVApplication.kt                    # Initialisation SDK (MobileAds, AdManager)
├── AdManager.kt                         # Singleton gestionnaire d'interstitiels AdMob
├── core/
│   ├── domain/model/
│   │   ├── MatchItem.kt                 # Entité Match
│   │   ├── VideoItem.kt                 # Entité Vidéo sauvegardée
│   │   └── VideoTrackInfo.kt            # Métadonnées des pistes vidéo/qualités
│   └── security/
│       └── IntentSecurityManager.kt     # Validation stricte http/https & anti-hijacking
├── data/
│   └── VideoRepository.kt               # Persistance locale (JSON SharedPreferences)
├── features/
│   ├── ads/
│   │   ├── AdFlowController.kt          # Orchestration MVI du flux publicitaire
│   │   └── AdFrequencyManager.kt        # Cooldown anti-zapping (DEFAULT_COOLDOWN_MS = 0)
│   ├── home/presentation/
│   │   ├── MainActivity.kt              # Hôte NavGraph Jetpack Compose (exported=true)
│   │   └── VideoListViewModel.kt        # ViewModel partagé CRUD vidéos
│   └── player/presentation/
│       ├── PlayerActivity.kt            # Activité paysagiste dédiée (PiP, singleTask)
│       ├── PlayerUiAction.kt            # Actions MVI de lecture
│       ├── PlayerUiState.kt             # États MVI fermés du lecteur
│       └── PlayerViewModel.kt           # Propriétaire unique d'ExoPlayer (AndroidViewModel)
├── navigation/
│   ├── NavGraph.kt                      # NavHost Jetpack Compose
│   └── Screen.kt                        # Routes (Home, Videos, AddVideo)
├── player/
│   ├── HeaderAwareMediaSourceFactory.kt # Injection dynamique des headers HTTP par MediaItem
│   └── HeaderExtractor.kt              # Extraction et assainissement des headers
├── security/
│   └── PermissionHelper.kt              # Vérification de signature de l'appelant IPC
├── ui/
│   ├── components/
│   │   ├── BlockedScreen.kt             # Écran de blocage (adblocker/réseau)
│   │   ├── ErrorScreen.kt               # Écran d'erreur générique
│   │   ├── PlayerControls.kt            # Overlay de contrôle (seek, qualities, sources)
│   │   ├── QualitySelectionDialog.kt    # Sélecteur de résolution (1080p, 720p, Auto)
│   │   └── VideoPlayer.kt               # Composable hôte AndroidView(PlayerView)
│   ├── screens/
│   │   ├── AddVideoScreen.kt            # Formulaire d'ajout de flux
│   │   ├── HomeScreen.kt                # Accueil (Drawer, NexiPlayer stylisé, Rate Us direct)
│   │   └── VideoListScreen.kt           # Liste des flux
│   └── theme/
│       ├── Color.kt                     # Palette YouTube Red (#C41E3A)
│       ├── Theme.kt                     # Définition MaterialTheme
│       └── Type.kt                      # Typographie Compose
└── util/
    └── NetworkUtils.kt                  # Vérification état réseau
```

### Module `:soukitv` (`com.example.soukitv`)
```
soukitv/src/main/java/com/example/soukitv/
├── MainActivity.kt                      # Point d'entrée catalogue
├── core/
│   └── player/
│       └── StvPlayerLauncher.kt         # Lanceur IPC de PlayerActivity (:app)
├── features/home/
│   ├── data/
│   │   ├── model/
│   │   │   ├── ChannelDto.kt            # DTO Chaîne
│   │   │   ├── MatchItemDto.kt          # DTO Match & Sources
│   │   │   └── RemoteCatalogConfigDto.kt# DTO Configuration distante
│   │   ├── mapper/
│   │   │   ├── ChannelMapper.kt         # Mapping DTO -> Domain
│   │   │   └── MatchItemMapper.kt       # Mapping DTO -> Domain
│   │   ├── remote/
│   │   │   └── ChannelApiService.kt     # Interface Retrofit 3.0.0
│   │   ├── ChannelRepositoryImpl.kt     # Implémentation Repository réseau
│   │   └── SharedPrefsFavoritesStore.kt # Persistance favoris
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Category.kt              # Modèle Catégorie
│   │   │   ├── Channel.kt               # Modèle Chaîne
│   │   │   └── MatchItem.kt             # Modèle Match
│   │   └── repository/
│   │       ├── ChannelRepository.kt     # Interface Repository
│   │       └── FavoritesStore.kt        # Interface Favoris
│   └── presentation/
│       ├── components/
│       │   ├── AutoCropTransparentTransformation.kt # Rogne les bordures transparentes des logos
│       │   ├── CategoryCard.kt          # Carte catégorie
│       │   ├── ChannelTile.kt           # Tuile chaîne
│       │   ├── ChannelsTab.kt           # Onglet chaînes par pays/catégorie
│       │   ├── CountryCard.kt           # Carte pays
│       │   ├── InfoCard.kt              # Carte info
│       │   ├── LogoImage.kt             # Image Coil avec transformation
│       │   ├── MatchCard.kt             # Carte match en direct
│       │   ├── MatchesTab.kt            # Onglet matchs du jour
│       │   ├── SearchAndFilterBar.kt    # Barre de recherche & filtres
│       │   ├── Shimmer.kt               # Placeholders de chargement
│       │   ├── SoukiBottomBar.kt        # Navigation inférieure
│       │   ├── OfflineErrorView.kt       # Écran d'erreur hors-ligne natif M3 avec bouton Réessayer
│       │   └── StvErrorDialog.kt        # Dialogue erreur d'installation :app
│       ├── HomeUiAction.kt              # Actions MVI catalogue
│       ├── HomeUiState.kt               # États MVI catalogue
│       └── HomeViewModel.kt             # ViewModel MVI du catalogue
└── ui/home/
    └── HomeScreen.kt                    # Écran principal SoukiTV
```

---

## 3. 📦 DÉPENDANCES ET SPÉCIFICATIONS TECHNIQUES

| Composant / Lib | Version | Usage principal |
| :--- | :--- | :--- |
| **Android Gradle Plugin** | `9.0.1` | Build System Android |
| **Kotlin** | `2.3.10` | Langage principal (JVM 17) |
| **Compile / Target SDK** | `36` (minSdk 24) | Android SDK 36 (Android 15/16 Ready) |
| **Jetpack Compose BOM** | `2026.02.01` | Framework UI Déclaratif |
| **Compose UI / Material3** | `1.10.4` / `1.4.0` | Composants graphiques M3 |
| **Media3 ExoPlayer** | `1.9.2` | Moteur vidéo (HLS, DASH, RTSP, SmoothStreaming) |
| **Google Play Services Ads** | `25.0.0` | AdMob Interstitial Ads |
| **Retrofit** | `3.0.0` | Client HTTP REST (module `:soukitv`) |
| **Coil Compose** | `2.7.0` | Chargement d'images d'équipes/chaînes (module `:soukitv`) |
| **MockK / Coroutines Test** | `1.13.13` / `1.9.0` | Suite de tests unitaires JVM |

---

## 4. ⚙️ ANALYSE DÉTAILLÉE DES MODULES

### Module `:app` (Le Moteur de Lecture)
1. **Gestion Mémoire & Isolation ExoPlayer (`PlayerViewModel.kt`)** :
   - L'instance d'ExoPlayer est instanciée **exclusivement** avec le contexte `Application` (`AndroidViewModel`), éliminant toute fuite mémoire d'Activity.
   - `PlayerViewModel` survit aux rotations d'écran (pas de re-création du lecteur au zapping ou changement d'orientation).
   - Libération explicite dans `onCleared()` (`_player.release()`).
   - Configuration `DefaultLoadControl` optimisée pour **Fast-Startup** : `minBufferMs=15000`, `maxBufferMs=50000`, `bufferForPlaybackMs=800`, `bufferForPlaybackAfterRebufferMs=3000`.

2. **Passage de Headers HTTP Dynamiques (`HeaderAwareMediaSourceFactory.kt`)** :
   - Les headers spécifiques à un flux (ex. `Referer`, `User-Agent`) sont encapsulés dans `MediaItem.RequestMetadata.extras` à la construction de la session (`MediaItemHeaders.write()`).
   - `HeaderAwareMediaSourceFactory` lit ces extras par item à la volée, évitant tout état statique global et toute fuite de headers entre deux chaînes.

3. **Monétisation & Publicité (`AdFlowController.kt` / `AdFrequencyManager.kt`)** :
   - `DEFAULT_COOLDOWN_MS = 0L` : Zapping fluide instantané sans spam.
   - Timeout de chargement `adLoadTimeoutMs = 10_000L` (10s max avant reprise/fallback).
   - Détection d'AdBlocker : en cas d'échec réseau répété sur l'annonceur, l'état bascule sur `AdResult.AdBlockerBlocked`.

4. **Sécurité & Contrat IPC (`IntentSecurityManager.kt` / `PermissionHelper.kt`)** :
   - **Action Intent** : `com.soufcom.stv.action.PLAY_STREAM` et `com.stv.videoplayer.action.PLAY_STREAM`.
   - **Schémas autorisés** : `http://` et `https://` strictement (rejet immédiat de `file://`, `content://`, `javascript:`).
   - **Extras gérés** : `VIDEO_URL`, `title` / `EXTRA_TITLE`, `SKIP_ADS`, `sources_json`, `headers`, `referer`.
   - **Authentification** : `PermissionHelper` vérifie la signature de l'application appelante avant d'accorder les privilèges `SKIP_ADS`.

---

### Module `:soukitv` (Le Catalogue de Streaming)
1. **Récupération de Données Réseau** :
   - `ChannelApiService` effectue des requêtes REST vers la configuration distante JSON GitHub.
   - Robustesse null-safe : `ChannelDto`, `MatchItemDto` et `RemoteCatalogConfigDto` tolèrent les champs absents ou malformés.
   - Mapping strict vers les entités de domaine (`Channel`, `MatchItem`, `Category`).

2. **Interface Utilisateur & Expérience Visuelle** :
   - Composants Material 3 avec onglets Matchs du jour et Chaînes IPTV par catégorie/pays.
   - `AutoCropTransparentTransformation` : Transformation Coil personnalisée analysant la matrice de pixels pour rogner les zones transparentes autour des logos de chaînes/clubs.
   - Navigation avec `SoukiBottomBar` et barre de recherche/filtrage en temps réel (`SearchAndFilterBar`).

3. **Lancement de Lecteur Distant (`StvPlayerLauncher.kt`)** :
   - Tente d'ouvrir `:app` via un Intent explicite `com.soufcom.stv.action.PLAY_STREAM` avec le package `com.soufcom.stv`.
   - Redirection de secours via Deep Link `stv://play?url=...`.
   - Si `:app` n'est pas installée, affiche un dialogue M3 stylisé (`StvErrorDialog`) invitant à l'installation.

4. **Résilience MVI & Réseau** :
   - Suppression des faux fallbacks en dur dans `ChannelRepositoryImpl` afin de propager les erreurs réseau et JSON GitHub réelles.
   - Timeouts explicites sur `OkHttpClient` : connexion `10s`, lecture `15s`, écriture `15s`, avec retry réseau activé.
   - Pull-to-Refresh universel dans `HomeScreen`, actif sur les états `Loading`, `Error` et `Success`.
   - `StvErrorDialog` strictement découplé des erreurs réseau et réservé aux prompts d'installation ou de mise à jour du lecteur STV.

5. **Performances & Mémoire** :
   - Mémorisation avec `remember` de `AutoCropTransparentTransformation` et de `ImageRequest` dans `LogoImage`.
   - Clés légères sans concaténation de chaînes dans `MatchesTab` pour limiter les allocations lors du défilement.

6. **Sécurité IPC & Robustesse** :
   - `StvPlayerLauncher` filtre strictement les schémas `http` et `https`.
   - En l'absence d'URL primaire valide, bascule automatiquement sur la première source secondaire valide.

7. **Conformité SDK 36** :
   - Suppression de `statusBarColor` dans `Theme.kt`.
   - Gestion native Edge-to-Edge du contraste des icônes système via `WindowCompat`.

---

## 5. 🧪 COUVERTURE DE TESTS UNITAIRES (184 TESTS PASSÉS)

La suite de tests unitaires JVM s'exécute sans émulateur avec un taux de réussite de **100% (184/184 tests)** :

- **Module `:app` (142 tests unitaires validés)** :
  - `VideoItemTest` : Validation des entités vidéo.
  - `IntentSecurityManagerTest` : Validation de sécurité des URIs et filtrage anti-hijacking.
  - `AdFlowControllerTest` : Tests MVI du flux publicitaire, timeouts et retries.
  - `AdFrequencyManagerTest` : Validation des règles de cooldown et adblocker.
  - `AdManagerPreloadTest` : Tests de préchargement des interstitiels AdMob.
  - `PlayerViewModelTest` : Validation MVI du cycle de vie du lecteur et du choix de pistes.
  - `HeaderExtractorTest` : Assainissement et parsing des headers HTTP.
  - `MediaItemHeadersTest` : Transport bundle d'extras HTTP.
  - `PermissionHelperTest` : Validation des signatures d'appelants IPC.
  - `PlayerUiStateTest` : Machine à états du lecteur.

- **Module `:soukitv` (42 tests unitaires validés)** :
  - `StvPlayerLauncherTest` : Construction des Intents IPC vers `:app`.
     - Trois tests de sécurité couvrent les URLs primaires nulles, les schémas interdits et la sélection de sources secondaires valides.
  - `MappersTest` : Mapping DTO -> Domain (`ChannelMapper`, `MatchItemMapper`).
  - `HomeViewModelTest` : MVI du catalogue, filtres, recherche et gestion des favoris.

---

## 6. 🛠️ COMMANDES DE COMPILATION & MIGRATION

### Compilation de Production (Release App Bundle)
```powershell
.\gradlew clean :app:bundleProdRelease --no-daemon
```

### Exécution de la Suite de Tests Unitaires
```powershell
.\gradlew :app:testDevDebugUnitTest :soukitv:testDebugUnitTest
```

### Directives d'écriture de Code (Strict Rules)
1. **Zero Hardcoded Strings** : Toute chaîne visible dans l'UI du module `:app` doit utiliser `stringResource(R.string.app_name)` ou une clé centralisée dans `strings.xml`.
2. **Strict XML Headers** : Les fichiers de ressources XML (`adaptive-icon`, `color`, `vector`) doivent obligatoirement commencer par `<?xml version="1.0" encoding="utf-8"?>` en **première ligne absolue** (sans commentaire préalable).
3. **MVI Conformance** : Les vues (`Composables` / `Activities`) émettent exclusivement des `UiAction` et observent des `UiState` via `StateFlow`.
