# Enterprise Architecture & Engineering Guidelines — STV Ecosystem

> **Author:** Senior Android Architect & Media3 System Expert
> **Target:** Monorepo `:app` (STV Player) & `:soukitv` (Streaming Catalog)
> **Status:** Production-Ready / Google Play Compliant

---

## 1. Monorepo & Multi-Module Architecture Charter

Le projet est un monorepo structuré en deux modules applicatifs strictement isolés. Aucun couplage direct (dépendance Gradle circulaire ou import direct) n'est toléré entre `:app` et `:soukitv`.

- **Module `:app` (STV Video Player) :** Publié sur le Google Play Store (Coquille blanche neutre).
- **Module `:soukitv` (Catalogue Streaming) :** Distribué hors Play Store (Émetteur de flux).

### 1.1. Module `:app` (Le Moteur — STV Player)
- **Rôle :** Lecteur vidéo universel haute performance, 100% conforme aux règles Google Play.
- **Identifiants officiels (`applicationId`) :**
    - Production : `com.stv.videoplayer`
    - Développement : `com.stv.videoplayer.dev` (via `applicationIdSuffix = ".dev"`)
    - **Règle absolue :** Ne JAMAIS utiliser `com.example` en production (rejet Play Store immédiat).
- **Contenu :** Zéro chaîne intégrée, zéro liste M3U en dur. Outil de lecture passif pur.
- **Stack :** Media3 ExoPlayer, Clean Architecture (Package-by-Feature), MVI, AdMob (Interstitiel preloaded).

### 1.2. Module `:soukitv` (La Vitrine — SoukiTV)
- **Rôle :** Catalogue de chaînes TV et contenus VOD avec navigation fluide.
- **Identifiant :** `com.example.soukitv` (ou identifiant de distribution directe).
- **Stack :** Jetpack Compose, Material 3, MVVM, Coil, Kotlin Coroutines & StateFlow.
- **Comportement :** Émetteur exclusif. N'embarque aucun lecteur vidéo lourd ; délègue 100% de la lecture à `:app`.

---

## 2. Le Contrat d'Intent Immuable (IPC Inter-Applications)

La communication entre `:soukitv` et `:app` est régie par un contrat d'interface immuable. Toute modification de ce contrat briserait la rétrocompatibilité.

Flux d'appel : `[soukitv]` émet un Intent explicite `PLAY_STREAM` avec package et extras vers `[STV Player]`.

### 2.1. Spécification de l'Émetteur (`:soukitv`)
1. **Package Visibility (Android 11+) :** Le fichier `soukitv/src/main/AndroidManifest.xml` DOIT obligatoirement déclarer les packages :
    - `com.stv.videoplayer`
    - `com.stv.videoplayer.dev`
    - `com.example.stv`
    - `com.example.stv.dev`
2. **Lancement Exclusif :** L'Intent doit être verrouillé via `intent.setPackage(targetPackage)` pour interdire l'ouverture de menus système ou de lecteurs tiers.

### 2.2. Spécification du Récepteur (`:app`)
- **Action officielle :** `com.stv.videoplayer.action.PLAY_STREAM` (alias maintenu : `com.example.stv.action.PLAY_STREAM`).
- **Action standard de secours :** `android.intent.action.VIEW`.
- **Payload & Extras reconnus :**
    - Données : `Uri` via `setDataAndType(uri, "video/*")` ou extra `"VIDEO_URL"` / `"url"`.
    - Titre : Extra `"title"` ou `Intent.EXTRA_TITLE` (String).
    - Headers HTTP personnalisés : Extra `"headers"` (Bundle, Map ou String array) et `"referer"`.

---

## 3. Clean Architecture & MVI (Model-View-Intent)

### 3.1. Règles Architecturales
- **Couche Domain (`core/domain/`) :** Modèles purs (`VideoItem`, `VideoTrackInfo`). Zéro dépendance Android (`android.*`).
- **Couche Security (`core/security/`) :** Validation statique et textuelle indépendante de l'OS.
- **Couche Presentation (`features/*/presentation/`) :**
    - **Flux de données unidirectionnel (UDF) :** UI (Activity / Compose) -> PlayerUiAction -> ViewModel -> StateFlow<PlayerUiState> -> UI.
    - **Interdiction formelle :** Aucune logique métier, calcul de temps ou manipulation de lecteur dans les Composables ou les Activités.

---

## 4. Media3 (ExoPlayer) — Normes Système & Mémoire

### 4.1. Protocole Zéro Fuite Mémoire (Zero Memory Leak)
1. **Propriété Unique :** `PlayerViewModel` est le seul détenteur de l'instance `Player`.
2. **Isolation du Contexte :** L'ExoPlayer DOIT impérativement être instancié avec le `ApplicationContext`. Ne JAMAIS passer de contexte d'Activity.
3. **Libération Absolue :** `PlayerViewModel.onCleared()` DOIT appeler `player.removeListener()` puis `player.release()`.
4. **Cycle de Vie UI :** La vue s'attache dans `ON_START` (`playerView.player = viewModel.player`) et se détache obligatoirement dans `ON_STOP` (`playerView.player = null`).

### 4.2. Configuration "Pro Engine" (Flux Externes)
Toute instance de production doit maintenir les optimisations suivantes dans `createDefaultExoPlayer` :
- **Fast Startup Buffer :** `bufferForPlaybackMs = 800`, `bufferForPlaybackAfterRebufferMs = 1500`, `setPrioritizeTimeOverSizeThresholds(true)`.
- **Tolérance Réseau :** `DefaultHttpDataSource.Factory` avec `setAllowCrossProtocolRedirects(true)`, timeouts à 8000 ms, User-Agent moderne.
- **Tolérance Matérielle :** `DefaultRenderersFactory` avec `setEnableDecoderFallback(true)` (bascule logicielle automatique en cas d'échec de codec GPU).

---

## 5. Sécurité & Conformité Google Play (Priorité Absolue)

### 5.1. Sécurité des Intents (Anti-Hijacking)
- Toute URL entrante DOIT être validée par `IntentSecurityManager` avant d'atteindre Media3.
- Seuls les schémas `http` et `https` sont autorisés. Rejet strict et immédiat de `file://`, `content://`, `javascript:`, `intent://`.
- Tout Intent malformé ou extra corrompu doit être capté sans crash (Safe-Call / Result pattern).

### 5.2. Minification & R8 / ProGuard
- Le fichier `app/proguard-rules.pro` doit préserver les classes réflexives de Media3/ExoPlayer et les interfaces du SDK Google Mobile Ads.
- Toujours tester la compilation minifiée via `assembleProdDebug` ou `assembleProdRelease`.

---

## 6. Monétisation (AdMob) & Gestion de la Fréquence

1. **Format :** Publicité Interstitielle Plein Écran (`InterstitialAd`).
2. **Boucle de Pre-loading :** L'interstitiel doit être chargé en tâche de fond dès le démarrage et automatiquement rechargé dès qu'une pub se termine ou échoue (`onAdDismissedFullScreenContent` / `onAdFailedToLoad`).
3. **Anti-Zapping Cooldown :** Un délai de grâce de 90 secondes (`AdFrequencyManager`) est strictement appliqué. Si l'utilisateur zappe avant 90s, la pub est ignorée (SKIP) et la vidéo démarre immédiatement.
4. **Anti-AdBlock :** Si un bloqueur est détecté, émettre `PlayerUiState.AdBlockerBlocked` et présenter l'écran dédié (`BlockedScreen`) avec l'action MVI `RetryAdCheck`.

---

## 7. Règles Intouchables d'Interface Utilisateur (UI/UX)

Sauf demande explicite de l'utilisateur, l'Agent Copilot a interdiction formelle de modifier :
- Le mode Strictement Paysage (`android:screenOrientation="sensorLandscape"`).
- Le mode Plein Écran Immersif (masquage automatique des barres système).
- La structure graphique existante : disposition des boutons de contrôle, sliders, dialogues de sélection de qualité et `BlockedScreen`.

---

## 8. Standards de Code & Qualité (Engineering Excellence)

- **Langage :** Kotlin 100% idiomatique (Coroutines, StateFlow, sealed interfaces).
- **Asynchronisme :** Utiliser explicitement `Dispatchers.IO` pour le réseau/disque et `Dispatchers.Default` pour les calculs lourds. Ne jamais bloquer le Main Thread.
- **Tests Unitaires :** Tout nouveau composant de logique (sécurité, validation, cooldown, MVI) doit être accompagné de tests unitaires exécutables sur JVM pure (sans émulateur).
- **Validation avant livraison :** Tout refactoring doit être validé par :
  `./gradlew testDevDebugUnitTest assembleDevDebug`