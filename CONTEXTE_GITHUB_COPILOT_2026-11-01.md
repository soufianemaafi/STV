# 📋 CONTEXTE PROJET GITHUB COPILOT — 01 NOVEMBRE 2026 (V3.0)

> **Projet** : STV Ecosystem Monorepo  
> **Repository** : `D:\STV`  
> **Date de mise à jour** : 01 Novembre 2026  
> **Version Global Architecture** : V3.0 Clean Architecture + MVI  
> **Git Release Tag** : `v3.5.0-soukitv-dynamic-lockin` (Novembre 2026)
> **Production Release `:app`** : `v1.0.3` — **Version Code 4** (NexiPlayer)

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

### Versioning officiel synchronisé

- **`:app`** : `versionCode = 4`, `versionName = "1.0.3"` — release de production `com.soufcom.stv` publiée sur Google Play Store.
- **`:soukitv`** : `versionCode = 6`, `versionName = "3.6.0"` — release catalogue hors-store.

Ces valeurs constituent le versioning officiel de référence et ne doivent pas être régressées ou désynchronisées lors d'un audit ou d'un refactoring.

---

## 2. 📂 ARBORESCENCE DÉTAILLÉE DES MODULES

### Module `:app` (namespace `com.stv.videoplayer`, applicationId `com.soufcom.stv`)
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
│   │   ├── AdBlockerDetector.kt           # Test croisé réseau/DNS des domaines publicitaires Google
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
│   ├── player/
│   │   └── StvPlayerLauncher.kt         # Lanceur IPC de PlayerActivity (:app)
│   └── security/
│       ├── AppSignatureVerifier.kt      # Contrôle d'intégrité de signature XOR-obfusqué (Anti-Repackaging)
│       └── SecretDecoder.kt              # Décodage XOR des endpoints réseau obfusqués
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

### 4.0. 🔒 RÈGLE D'OR INTANGIBLE — POLITIQUE RÉSEAU DUAL (MÉDIA VS API SENSIBLE)

Cette politique est **intouchable** lors de tout audit, durcissement ou refactoring futur :

- **Cleartext obligatoire pour le streaming multimédia** : `android:usesCleartextTraffic="true"` doit rester présent dans les manifests de `:app` et `:soukitv`, et `cleartextTrafficPermitted="true"` doit rester configuré dans le `base-config` de leurs `network_security_config.xml`. Cette tolérance est indispensable car environ 80 % des flux IPTV tiers mondiaux, des infrastructures de relais et des logos distants utilisent encore `http://`. Désactiver globalement le cleartext rendrait légitimes ces flux illisibles et constitue une régression fonctionnelle interdite.
- **Redirections cross-protocoles Media3 obligatoires** : `HeaderAwareMediaSourceFactory.kt` doit conserver `DefaultHttpDataSource.Factory.setAllowCrossProtocolRedirects(true)`. De nombreux CDN distribuent une playlist HTTPS qui redirige vers des segments HTTP, ou l'inverse ; passer cette option à `false` casse la lecture vidéo de ces architectures.
- **Rempart anti-MITM préservé** : la confiance réseau repose sur `<certificates src="system" />`, qui rejette les autorités de certification utilisateur susceptibles d'être injectées par Burp Suite ou Charles Proxy. Cette protection doit rester présente dans les configurations réseau applicatives.
- **API sensibles sanctuarisées** : les domaines qui transportent des tokens, de la télémétrie ou le catalogue distant — notamment `googleads.g.doubleclick.net`, `pagead2.googlesyndication.com` et `raw.githubusercontent.com` — doivent rester dans des `domain-config` avec `cleartextTrafficPermitted="false"` (HTTPS strict), indépendamment de la tolérance média globale.

La règle est donc duale : **HTTP autorisé pour les médias et ressources distantes non sensibles ; HTTPS strict et ancres système pour les services sensibles**. Il est interdit de remplacer cette séparation par un blocage global du cleartext.

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
   - **Tolérance AdMob / Anti-AdBlock** : `ERROR_CODE_NO_FILL` (code 3) est explicitement ignorée (bypass), car elle signifie que l'inventaire Google est momentanément vide et ne constitue pas un signal d'AdBlocker.
   - **Détection d'AdBlocker renforcée** : utilise `ConnectivityManager` (`NET_CAPABILITY_VALIDATED`) puis un ping/résolution DNS de `doubleclick.net` en complément de la vérification de connectivité, afin d'éviter les faux positifs légitimes.

4. **Smart Auto-Failover (`PlayerViewModel.kt`)** :
   - Un `bufferingJob` surveille les états `Player.STATE_BUFFERING` pendant 12 secondes.
   - Si le flux gèle ou reste bloqué en chargement au-delà de ce délai, le lecteur bascule automatiquement vers la source secondaire via l'action MVI `SwitchSource`.
   - Le job est annulé proprement lors des transitions `READY`, `IDLE` et `ENDED`, lors d'un changement de source, d'une nouvelle session ou de la destruction du ViewModel.

5. **UX Navigation (`PlayerActivity.kt`)** :
   - `BackHandler { finish() }` est installé pour les états `LoadingAds`, `ShowingAd` et `NeedRetry`.
   - L'utilisateur peut ainsi quitter le lecteur pendant l'attente ou l'échec AdMob sans rester bloqué sur un écran noir de chargement.

6. **Rebranding NexiPlayer & ressources Android** :
   - Les icônes `launcher` et le `Splash Screen` API 12+ utilisent désormais le logo NexiPlayer via les ressources `@mipmap`.
   - Les fichiers adaptive-icon respectent la charte **Strict XML Headers** : déclaration `<?xml version="1.0" encoding="utf-8"?>` en première ligne absolue et aucune déclaration XML dupliquée.

7. **Sécurité & Contrat IPC (`IntentSecurityManager.kt` / `PermissionHelper.kt`)** :
   - **Action Intent** : `com.soufcom.stv.action.PLAY_STREAM` et `com.stv.videoplayer.action.PLAY_STREAM`.
   - **Schémas autorisés** : `http://` et `https://` strictement (rejet immédiat de `file://`, `content://`, `javascript:`).
    - **Ordre d'extraction des URLs** : `IntentSecurityManager.kt` essaie successivement `VIDEO_URL`, l'alias `url`, puis `intent.dataString`. Chaque valeur est validée strictement ; les schémas malveillants ou non `http`/`https` sont rejetés immédiatement.
    - **Extras gérés** : `VIDEO_URL`, `url`, `title` / `EXTRA_TITLE`, `SKIP_ADS`, `sources_json`, `headers`, `referer`.
    - **Authentification** : `PermissionHelper.kt` compare les empreintes SHA-256 avec `MessageDigest.isEqual` à temps constant. L'extra `SKIP_ADS` est refusé immédiatement aux appelants inconnus ou non signés.

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
    - API `suspend` asynchrone pure : toute résolution et émission IPC s'exécute via `withContext(ioDispatcher)`, sans `runBlocking` susceptible de bloquer le thread principal ou l'UI.
   - Tente d'ouvrir `:app` via un Intent explicite `com.soufcom.stv.action.PLAY_STREAM` avec le package `com.soufcom.stv`.
   - Redirection de secours via Deep Link `stv://play?url=...`.
   - Si `:app` n'est pas installée, affiche un dialogue M3 stylisé (`StvErrorDialog`) invitant à l'installation.
    - **Identité de production immuable** : `com.soufcom.stv` est l'`applicationId` officiel de production de `:app`, publié sur Google Play Store. Le package `com.soufcom.stv.dev` est réservé au développement.
    - **Verrou Cryptographique IPC** : vérification obligatoire de l'empreinte de signature SHA-256 du package `com.soufcom.stv` avant toute émission d'Intent.
   - Double liste blanche XOR-obfusquée avec la clé `0x5A`, couvrant la nouvelle clé Google Play et la clé historique de NexiPlayer.
   - Blocage immédiat des lecteurs pirates ou re-signés avec retour `LaunchResult.Failed`.
   - Comparaison à temps constant via `MessageDigest.isEqual` et bypass automatique en mode `BuildConfig.DEBUG`.
    - **Player Lock-in dynamique** : l’application récupère la clé secrète `stvAuthSecret` depuis le JSON distant et l’injecte dans le header `X-STV-Auth` via IPC, avec le User-Agent officiel. Cette authentification empêche le vol de flux par des lecteurs tiers tout en permettant la rotation instantanée de la clé sans mise à jour de l’APK. En l’absence de clé, le contrat reste fonctionnel avec une valeur vide contrôlée.
    - **Stream Dispatcher** : `core/player/StvPlayerLauncher.kt` résout les tickets internes `/dispatch` avant l’IPC via un `StreamDispatcher` OkHttp exécuté sur `Dispatchers.IO` (timeouts courts de 3 secondes). Le balancer du relais `.190` expose `/dispatch`, exige `X-STV-Auth` et renvoie en environ 1 ms l’adresse directe de l’Edge sélectionné ; il ne transporte aucune vidéo et consomme donc 0 % de la bande passante vidéo.
    - Pour les matchs multi-sources, le ticket est résolu une seule fois en mémoire vive. Toutes les entrées `/dispatch` de `sources_json` sont réécrites avec le même Edge, tandis que les sources externes (Korasport, Cloud Run, etc.) sont conservées strictement inchangées. Cette préparation évite les erreurs de zapping entre sources internes et externes dans NexiPlayer.

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
    - **Protection Réseau & Anti-MITM (`network_security_config.xml`)** : Rejet strict des certificats pirates/proxys utilisateurs via `<certificates src="system" />` (neutralisation de Burp Suite / Charles Proxy sur les requêtes API), tout en autorisant le trafic média tiers en clair pour assurer un affichage fiable des logos de chaînes et de clubs distants ; les domaines sensibles restent en HTTPS strict conformément à la politique duale de la section 4.0.

7. **Conformité SDK 36** :
   - Suppression de `statusBarColor` dans `Theme.kt`.
   - Gestion native Edge-to-Edge du contraste des icônes système via `WindowCompat`.

8. **Sécurité & Intégrité Applicative (`AppSignatureVerifier.kt`)** :
   - Protection Anti-Repackaging sans chaîne hexadécimale en clair : l'empreinte SHA-256 est stockée dans un `ByteArray` masqué via XOR `0x5A`.
   - Comparaison à temps constant avec `MessageDigest.isEqual` après extraction rétrocompatible des certificats de signature Android.
   - Fermeture immédiate de l'application au démarrage de `MainActivity` si l'APK a été altéré ou re-signé.
   - Génération explicitement activée via `buildConfig = true` dans `build.gradle.kts`, avec bypass automatique en mode `BuildConfig.DEBUG`.

9. **Obfuscation des Endpoints & Anti-JADX (`SecretDecoder.kt`)** :
   - Élimination totale des chaînes d'URLs distantes en clair dans le bytecode source du repository.
   - Masquage binaire XOR dynamique avec la clé `0x4B`, empêchant la recherche textuelle directe des endpoints dans Jadx-GUI/Apktool.

---

## 5. 🧪 COUVERTURE DE TESTS UNITAIRES (48 TESTS `:soukitv` PASSÉS)

La suite JVM des deux modules est validée sans émulateur et sans régression à **100%** : les 144 tests de `:app` et les 48 tests de `:soukitv` passent (192/192). La validation de référence est :

```powershell
.\gradlew :app:testProdDebugUnitTest :soukitv:testDebugUnitTest --no-daemon
```

Les en-têtes XML sont également soumis à une règle stricte : chaque fichier de ressources XML doit commencer en première ligne absolue par `<?xml version="1.0" encoding="utf-8"?>`, sans commentaire, espace ou déclaration dupliquée avant cet en-tête.

- **Module `:app` (144 tests unitaires validés)** :
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

- **Module `:soukitv` (48 tests unitaires validés)** :
   - `StvPlayerLauncherTest` : Construction des Intents IPC vers `:app`, résolution unique du Stream Dispatcher et réécriture sélective de `sources_json`.
      - Les tests couvrent les URLs primaires nulles, les schémas interdits, le fallback, la résolution d’un ticket `/dispatch`, l’appel réseau unique, la réécriture des sources internes et la conservation stricte des sources externes.
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
.\gradlew :app:testProdDebugUnitTest :soukitv:testDebugUnitTest --no-daemon
```

### Directives d'écriture de Code (Strict Rules)
1. **Zero Hardcoded Strings** : Toute chaîne visible dans l'UI du module `:app` doit utiliser `stringResource(R.string.app_name)` ou une clé centralisée dans `strings.xml`.
2. **Strict XML Headers** : Les fichiers de ressources XML (`adaptive-icon`, `color`, `vector`) doivent obligatoirement commencer par `<?xml version="1.0" encoding="utf-8"?>` en **première ligne absolue** (sans commentaire préalable).
3. **MVI Conformance** : Les vues (`Composables` / `Activities`) émettent exclusivement des `UiAction` et observent des `UiState` via `StateFlow`.
