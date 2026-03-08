# 📋 RAPPORT D'ANALYSE COMPLÈTE — STV Player v1.0

**Date** : 8 mars 2026  
**Auteur** : GitHub Copilot  
**Projet** : STV Player (package `com.example.stv`)  
**Build** : Kotlin + Jetpack Compose + Media3 ExoPlayer + AdMob  
**État** : ✅ BUILD SUCCESSFUL (dev + prod)

---

## 1. ARCHITECTURE GÉNÉRALE

### 1.1 Structure des fichiers (19 fichiers source Kotlin)

```
com.example.stv/
├── STVApplication.kt          (38 lignes)  — Application globale : init AdMob + AdManager
├── MainActivity.kt            (449 lignes) — Écran d'accueil : accès player, drawer, navigation
├── PlayerActivity.kt          (280 lignes) — Écran player : lifecycle, ads, sécurité
├── PlayerViewModel.kt         (340 lignes) — ViewModel player : ExoPlayer, tracks, état
├── VideoListActivity.kt       (352 lignes) — Liste des flux sauvegardés
├── VideoListViewModel.kt      (42 lignes)  — ViewModel liste : CRUD vidéos (délègue à Repository)
├── AddVideoActivity.kt        (357 lignes) — Formulaire ajout d'un flux
├── AdManager.kt               (~110 lignes) — Singleton AdMob : loadAndShow()
├── VideoItem.kt               (13 lignes)  — Data class vidéo (id UUID, title, url)
├── VideoTrackInfo.kt          (11 lignes)  — Data class piste vidéo (nom, groupe, index)
├── ads/
│   └── AdsController.kt       (113 lignes) — Orchestration pub : retry, timeout, state
├── data/
│   └── VideoRepository.kt     (67 lignes)  — Persistance vidéos (SharedPrefs + Kotlin Serialization)
├── player/
│   └── PlayerController.kt    (89 lignes)  — Résolution URL, validation flux (localisé)
├── security/
│   └── PermissionHelper.kt    (172 lignes) — Vérification signature appelant
└── ui/
    ├── PlayerUiState.kt        (24 lignes)  — State machine du player (6 états)
    ├── components/
    │   ├── VideoPlayer.kt      (145 lignes) — Composable player ExoPlayer + contrôles
    │   ├── PlayerControls.kt   (230 lignes) — Overlay contrôles + formatDuration()
    │   ├── QualitySelectionDialog.kt (65 lignes) — Dialog sélection qualité
    │   ├── ErrorScreen.kt      (24 lignes)  — Écran erreur simple
    │   └── BlockedScreen.kt    (70 lignes)  — Écran "Réessayer" (ads/réseau)
    └── theme/
        ├── Color.kt            (40 lignes)  — Palette YouTube-style
        ├── Theme.kt            (~90 lignes) — Thème Material3 dark
        └── Type.kt             (~15 lignes) — Typographie
```

**Total** : ~3 100 lignes de code Kotlin (hors tests)

### 1.2 Architecture pattern

| Couche | Pattern | Implémentation |
|--------|---------|----------------|
| UI | Jetpack Compose | `PlayerActivity`, `MainActivity`, composables |
| State | MVVM + StateFlow | `PlayerViewModel`, `VideoListViewModel` |
| Ads | Singleton + Controller | `AdManager` (singleton), `AdsController` (orchestration) |
| Sécurité | Vérification signature | `PermissionHelper` |
| Navigation | Multi-Activity | 4 Activities (pas Compose Navigation) |
| Persistance | Repository + SharedPrefs + JSON | `VideoRepository` avec Kotlin Serialization |
| Tests | JUnit + MockK + Coroutines Test | 38 tests unitaires (4 suites) |

### 1.3 Diagramme des composants

```
STVApplication
  └── MobileAds.initialize() + AdManager.initialize()

MainActivity ──── [bouton +] ───► AddVideoActivity (résultat)
     │                                    │
     └── [bouton Videos] ──► VideoListActivity ──── [clic item] ──► PlayerActivity
                                          │                              │
                                     [bouton +] ──► AddVideoActivity     │
                                                                         │
                                          PlayerActivity ◄─── Deep links / Intent externe
                                               │
                                    ┌──────────┴──────────┐
                                    │                     │
                              AdsController          PlayerViewModel
                                    │                     │
                              AdManager              ExoPlayer
                             (singleton)           (Media3)
```

---

## 2. POINTS FORTS ✅

### 2.1 Architecture ads — Robuste et conforme

| Point | Détail |
|-------|--------|
| **Flux linéaire** | `LoadingAds → ShowingAd → Ready` — un seul chemin, pas de race condition |
| **1 seule pub par flux** | `loadAndShow()` = charge + affiche + attend fermeture |
| **Pas de cache de pub** | Zéro variable d'état dans AdManager — chaque pub est fraîche |
| **Pas de préchargement** | La pub est chargée au moment où elle est nécessaire |
| **Timeout intelligent** | Le timeout couvre le chargement seulement — annulé dès que la pub est affichée |
| **Jamais de superposition** | Impossible d'avoir 2 pubs en même temps |
| **Player bloqué pendant pub** | ExoPlayer créé UNIQUEMENT dans l'état Ready (après fermeture pub) |
| **Retry automatique** | 3 tentatives transparentes avec délai 2s entre chaque |
| **Distinction réseau vs échec** | `isNetworkAvailable()` + `ERROR_CODE_NETWORK_ERROR` séparés |
| **Bouton Réessayer** | L'utilisateur peut retenter manuellement après échec |
| **Config flavors** | IDs test (dev) vs production (prod via local.properties) |
| **Pas de compteur persistant** | Reset au démarrage de l'app — pas de blocage permanent |

### 2.2 Player — Complet et performant

| Point | Détail |
|-------|--------|
| **Multi-format** | HLS, DASH, RTSP, Smooth Streaming, MP4 via Media3 |
| **Buffer optimisé** | 15s min / 50s max / 1.5s démarrage rapide |
| **Sélection qualité** | Auto + manuel avec résolution + bitrate affichés |
| **Badge LIVE** | Détection automatique `isCurrentMediaItemLive` + badge visuel |
| **PiP natif** | `supportsPictureInPicture` + `onUserLeaveHint` automatique |
| **Gestion audio** | Focus audio + pause sur déconnexion écouteurs |
| **Contrôles custom** | Overlay animé (fadeIn/fadeOut), auto-hide 3s |
| **Deep links** | `stv://play?url=...` + `video/*` + HLS MIME types |
| **singleTask** | Un seul PlayerActivity — `onNewIntent` gère les changements d'URL |

### 2.3 Sécurité

| Point | Détail |
|-------|--------|
| **Vérification signature** | `PermissionHelper` vérifie que l'appelant est signé avec la même clé |
| **Activities non exportées** | `VideoListActivity` et `AddVideoActivity` sont `exported=false` |
| **network_security_config** | Présent pour contrôler les connexions réseau |
| **Validation URL** | Schéma, longueur, format vérifiés par `PlayerController` |
| **IDs AdMob protégés** | Production lus depuis `local.properties` (hors Git) |

### 2.4 Internationalisation

| Point | Détail |
|-------|--------|
| **2 langues** | `values/strings.xml` (EN) + `values-fr/strings.xml` (FR) |
| **~110 strings** | Tous les textes UI sont dans les ressources |
| **Validation localisée** | Messages d'erreur AddVideoActivity via `stringResource()` |

### 2.5 Qualité de code

| Point | Détail |
|-------|--------|
| **State machine explicite** | `PlayerUiState` sealed class avec 6 états typés |
| **StateFlow partout** | Pas de LiveData — cohérent Compose |
| **Kotlin Serialization** | Pour la persistance JSON (typé, pas de JSONObject manuel) |
| **ID unique par vidéo** | `UUID.randomUUID()` pour éviter les conflits de suppression |
| **Coroutine Job géré** | `positionUpdateJob` annulé explicitement à chaque `releasePlayer()` |
| **Composables réutilisables** | `BlockedScreen`, `ErrorScreen`, `QualitySelectionDialog` |

---

## 3. POINTS FAIBLES ET AMÉLIORATIONS RESTANTES ⚠️

### 3.1 🔴 CRITIQUE — Bloquant pour le Play Store

| # | Problème | Fichier | Impact | État |
|---|----------|---------|--------|------|
| 1 | **Package `com.example.stv`** | `build.gradle.kts` L25 | **Refus publication Play Store** — `com.example.*` est interdit | ⚠️ À faire |
| 2 | **Strings FallbackBanner orphelines** | `strings.xml` | ~~3 strings inutilisées~~ → ✅ Corrigé (supprimées EN+FR) |
| 3 | **Strings AdBlock orphelines** | `strings.xml` | ~~7 strings inutilisées (`ad_block_strict_*`, `ad_block_retry/close`, `access_blocked`, `ad_blocker_detected`)~~ → ✅ Corrigé (supprimées EN+FR) |

### 3.2 🟡 MOYEN — Qualité / Maintenabilité

| # | Problème | Fichier | Impact |
|---|----------|---------|--------|
| 4 | **PlayerActivity.kt = 824 lignes** | PlayerActivity.kt | ~~Fichier trop gros~~ → ✅ Corrigé — **découpé en 6 fichiers** : PlayerActivity (280), VideoPlayer (145), PlayerControls (230), QualitySelectionDialog (65), ErrorScreen (24), BlockedScreen (70) |
| 5 | **MainViewModel presque inutile** | MainViewModel.kt | ~~35 lignes, URL en dur, inutilisé~~ → ✅ Corrigé — **fichier supprimé** (aucune référence dans le code actif) |
| 6 | **Persistance JSON/SharedPreferences** | VideoListViewModel.kt | ~~Logique persistance dans le ViewModel~~ → ✅ Corrigé — **VideoRepository** créé (`data/VideoRepository.kt`), séparation des responsabilités, prêt pour migration Room |
| 7 | **PlayerController.validateStreamUrl() — messages en français** | PlayerController.kt L44-63 | ~~Messages en dur en français~~ → ✅ Corrigé — utilise `context.getString(R.string.*)`, 4 strings ajoutées (EN+FR) |
| 8 | **ContentDescription "Back" en dur** | PlayerActivity.kt L529 | ~~`contentDescription = "Back"`~~ → ✅ Corrigé → `stringResource(R.string.back_button)` |
| 9 | **Bouton Cast non implémenté** | PlayerControls.kt | ~~`onClick = { }` — bouton visible mais inactif~~ → ✅ Corrigé — **bouton supprimé** (sera réajouté quand Chromecast sera implémenté) |
| 10 | **`width` inutilisé dans updateCurrentTrackName** | PlayerViewModel.kt L293 | ~~`val width` déclaré mais jamais utilisé~~ → ✅ Corrigé (variable supprimée) |
| 11 | **Pas de tests unitaires** | — | ~~Aucun test~~ → ✅ Corrigé — **38 tests** : PlayerControllerTest (15), AdsControllerTest (8), PlayerUiStateTest (8), VideoItemTest (6), ExampleUnitTest supprimé |
| 12 | **`onNewIntent` : string en dur "URL not provided"** | PlayerActivity.kt L239 | ~~string en dur~~ → ✅ Corrigé → `getString(R.string.url_not_provided)` |
| 12b | **`error_unknown` sans paramètre (bug visible)** | PlayerViewModel.kt L127-137 | ~~`getString(R.string.error_unknown)` sans argument → affichait `%1$s` littéralement~~ → ✅ Corrigé — passe `error.localizedMessage` comme paramètre |

### 3.3 🟢 MINEUR — Améliorations futures

| # | Problème | Fichier | Impact |
|---|----------|---------|--------|
| 13 | **Navigation multi-Activity** | Manifest | 4 Activities séparées — pourrait utiliser Compose Navigation pour unifier |
| 14 | **Pas de mode hors-ligne** | — | Si pas de réseau, aucune vidéo locale ne peut être lue |
| 15 | **Drawer avec éléments factices** | MainActivity.kt | ~~"History", "Favorites", "Settings" ne faisaient rien~~ → ✅ Corrigé — **éléments supprimés** (seront réajoutés quand les fonctionnalités seront implémentées) |
| 16 | **Pas de Chromecast** | PlayerControls.kt | Bouton Cast supprimé en attendant l'implémentation Chromecast |
| 17 | **Pas de gestion de l'orientation** | — | Le player est toujours en paysage (`sensorLandscape`) — pas de mode portrait |
| 18 | **Play/Pause contentDescription incorrecte** | PlayerActivity.kt L595 | ~~Utilisait `ad_block_close_button` pour Pause~~ → ✅ Corrigé → `pause_button` string dédiée ajoutée (EN+FR) |

---

## 4. STATE MACHINE DU PLAYER (DIAGRAMME FINAL)

```
              null (ViewModel créé)
               │
               └── initializeUiState() dans onCreate() AVANT setContent
                    │
                    ▼
┌──────── LoadingAds(url) ────────────────────────┐
│              │                                   │
│              │ showAdIfNeeded()                   │
│              │                                   │
│              ├── Tentative 1..3 automatique       │
│              │    ├── loadAndShow()               │
│              │    │    ├── onAdLoaded → show()    │
│              │    │    │    ├── onAdShowed         │
│              │    │    │    │   → ShowingAd(url)   │
│              │    │    │    │   timeout ANNULÉ     │
│              │    │    │    │                      │
│              │    │    │    └── onAdDismissed      │
│              │    │    │        → Ready(url) 🎬    │
│              │    │    │                          │
│              │    │    └── onAdFailedToLoad       │
│              │    │        ├── réseau? → NoNetwork │
│              │    │        └── autre? → Failed     │
│              │    │             → retry (delay 2s) │
│              │    │                               │
│              │    └── timeout 10s (chargement)    │
│              │         → Failed → retry           │
│              │                                   │
│              ├── 3 échecs → NeedRetry(url) 🔄     │
│              └── pas réseau → NetworkError(url) 📡 │
│                                                   │
│  [Réessayer] ──── retryAds() ──── ┘               │
│                                                   │
│  Error(message) ← URL invalide (état terminal)    │
└───────────────────────────────────────────────────┘

Ready(url) :
  └── LaunchedEffect(url) → initializePlayer(url)
       └── ExoPlayer créé → prepare() → play()
       
onStart/onResume : play() UNIQUEMENT si isReady()
onPause/onStop : pause()
onNewIntent : releasePlayer() → initializeUiState() → launchAdFlow()
```

---

## 5. FLUX ADS — ARCHITECTURE DÉTAILLÉE

```
STVApplication.onCreate()
  ├── MobileAds.initialize(this)     ← 1 seule fois au démarrage
  └── AdManager.initialize(this)     ← Singleton prêt (pas de préchargement)

PlayerActivity.onCreate()
  └── adsController = AdsController(AdManager.instance)
       │
       └── launchAdFlow(url)  ← via lifecycleScope.launch
            │
            └── adsController.showAdIfNeeded()
                 │
                 ├── tryOnce() :
                 │    ├── isNetworkAvailable() ? → Non → NoNetwork
                 │    ├── InterstitialAd.load()
                 │    │    ├── onAdLoaded → ad.show()
                 │    │    │    ├── onAdShowed → timeout ANNULÉ, attend dismiss
                 │    │    │    ├── onAdDismissed → ✅ AdDismissed
                 │    │    │    └── onAdFailedToShow → Failed
                 │    │    └── onAdFailedToLoad
                 │    │         ├── ERROR_CODE_NETWORK_ERROR → NoNetwork
                 │    │         └── autre → Failed
                 │    └── timeout 10s (chargement seulement) → Failed
                 │
                 ├── Résultat AdDismissed → return immédiat
                 ├── Résultat NoNetwork → return immédiat
                 ├── Résultat Failed → delay(2s) → tryOnce() suivant
                 └── 3 Failed → NeedRetry
```

### Garanties :
- **0 variable d'état** dans AdManager (pas de cache, pas de compteur)
- **0 préchargement** (ni pub, ni vidéo)
- **1 seul `InterstitialAd.load()`** par tentative
- **Timeout = chargement seulement** (annulé dès onAdShowed)
- **ExoPlayer créé UNIQUEMENT** dans l'état Ready
- **Jamais de superposition** de pubs

---

## 6. FICHIERS DE CONFIGURATION

### 6.1 build.gradle.kts (app)

| Config | Valeur |
|--------|--------|
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 36 |
| Java | 17 |
| Compose | Activé (BOM) |
| Flavors | `dev` (IDs test) / `prod` (IDs via local.properties) |
| Signing | `keystore.properties` (hors Git) |
| Minification | `isMinifyEnabled = true` + `isShrinkResources = true` en release |
| Serialization | `kotlinx-serialization-json` |

### 6.2 Dépendances principales

| Lib | Usage |
|-----|-------|
| `androidx.media3.exoplayer` | Lecteur vidéo (HLS, DASH, RTSP, SS) |
| `androidx.media3.ui` | PlayerView + contrôles |
| `androidx.compose.*` | UI déclarative |
| `androidx.lifecycle` | ViewModel + StateFlow |
| `play-services-ads` | AdMob interstitiel |
| `kotlinx-serialization-json` | Persistance vidéos |
| `androidx.core.splashscreen` | Splash screen natif |

### 6.3 Manifest

| Config | Valeur |
|--------|--------|
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE` |
| Application | `STVApplication` |
| PlayerActivity | `exported=true`, `singleTask`, `sensorLandscape`, `PiP` |
| VideoListActivity | `exported=false`, `singleTask` |
| AddVideoActivity | `exported=false` |
| Deep links | `stv://play`, `video/*`, `application/vnd.apple.mpegurl` |

### 6.4 Tests unitaires (38 tests — 4 suites)

**Emplacement** : `app/src/test/java/com/example/stv/`  
**Dépendances** : JUnit 4, MockK, kotlinx-coroutines-test  
**Commande** : `./gradlew testDevDebugUnitTest`

```
app/src/test/java/com/example/stv/
├── ads/
│   └── AdsControllerTest.kt       (8 tests) — Retry, timeout, NoNetwork, AdDismissed
├── player/
│   └── PlayerControllerTest.kt    (15 tests) — Validation URL, résolution Intent, deep links
├── ui/
│   └── PlayerUiStateTest.kt       (8 tests) — State machine, égalité, distinction des états
└── VideoItemTest.kt               (6 tests) — Sérialisation JSON, UUID unique, round-trip
```

| Suite | Tests | Ce qui est vérifié |
|-------|-------|--------------------|
| **AdsControllerTest** | 8 | Pub fermée → AdDismissed ; 3 échecs → NeedRetry ; NoNetwork immédiat (pas de retry) ; échec puis succès ; comptage des appels |
| **PlayerControllerTest** | 15 | URL null/vide/sans schéma/FTP/RTSP → erreur ; URL trop longue → erreur ; resolveVideoUrl depuis extra/data/deep link ; priorité extra sur data |
| **PlayerUiStateTest** | 8 | Chaque état contient la bonne URL/message ; états distincts avec même URL ; égalité de 2 états identiques |
| **VideoItemTest** | 6 | UUID unique ; sérialisation round-trip ; liste ; clés inconnues tolérées ; ID inclus dans le JSON |

---

## 7. MÉTRIQUES DE QUALITÉ

| Métrique | Valeur | Appréciation |
|----------|--------|--------------|
| Lignes totales | ~3 100 | ✅ Taille raisonnable pour un player complet |
| Fichier le plus gros | MainActivity.kt (449) | ✅ PlayerActivity découpé |
| Fichier le plus petit | VideoTrackInfo.kt (11) | ✅ |
| Warnings compilation | 4 (Theme.kt deprecated) | ✅ Non bloquants |
| Erreurs compilation | 0 | ✅ |
| Couverture i18n | ~99% | ✅ PlayerController corrigé |
| Couverture tests | 38 tests | ✅ Logique métier couverte |
| Packages | 4 (root, ads, player, security, ui) | ✅ Organisation claire |
| State machine | 6 états typés | ✅ Explicite et exhaustive |
| Strings orphelines | 0 | ✅ Nettoyé |

---

## 8. PLAN D'ACTION RECOMMANDÉ (PRIORITÉ)

### 🔴 P0 — Avant publication Play Store

1. **Changer le package** `com.example.stv` → `com.votredomaine.stvplayer` (build.gradle + dossiers)
2. ~~Nettoyer les strings orphelines~~ → ✅ Fait
3. ~~Corriger PlayerController~~ → ✅ Fait (string resources EN+FR)

### 🟡 P1 — Qualité

4. ~~Découper PlayerActivity.kt~~ → ✅ Fait — **6 fichiers** dans `ui/components/` (VideoPlayer, PlayerControls, QualitySelectionDialog, ErrorScreen, BlockedScreen)
5. ~~Supprimer ou fusionner MainViewModel~~ → ✅ Fait (fichier supprimé)
6. ~~Corriger le warning `width` inutilisé~~ → ✅ Fait
7. ~~Corriger `contentDescription = "Back"`~~ → ✅ Fait
8. ~~Corriger Play/Pause contentDescription~~ → ✅ Fait
9. ~~Corriger `onNewIntent` L239~~ → ✅ Fait

### 🟢 P2 — Améliorations futures

10. ~~Ajouter des tests unitaires~~ → ✅ Fait (38 tests : PlayerController, AdsController, PlayerUiState, VideoItem)
11. ~~Retirer bouton Cast non fonctionnel~~ → ✅ Fait (supprimé de PlayerControls)
12. ~~Retirer éléments factices du Drawer~~ → ✅ Fait (History, Favorites, Settings supprimés)
13. **Migrer vers Room DB** pour la persistance vidéos
14. **Migrer vers Compose Navigation** (single Activity)
15. **Mode portrait optionnel** pour le player

---

## 9. CONTEXTE POUR LA PROCHAINE SESSION

### Ce qui est fait et stable :
- ✅ Architecture ads propre : 1 pub → fermée → player démarre
- ✅ Pas de superposition de pubs possible
- ✅ Timeout intelligent (chargement seulement)
- ✅ Distinction réseau vs échec avec bouton Réessayer
- ✅ Player ExoPlayer complet (HLS, DASH, qualité, PiP, LIVE badge)
- ✅ Sécurité signature pour les appels externes
- ✅ Flavors dev/prod avec IDs AdMob séparés
- ✅ i18n EN/FR quasi complète
- ✅ Persistance vidéos avec Kotlin Serialization + UUID unique
- ✅ State machine 6 états typés dans le ViewModel

### Ce qui reste à faire :
- ⚠️ Changer le package `com.example.stv` avant publication
- 💡 Chromecast, Drawer fonctionnel, Room DB (V2)

### Fichiers clés à connaître :
- **Flux ads** : `AdManager.kt` → `AdsController.kt` → `PlayerActivity.launchAdFlow()`
- **State machine** : `PlayerUiState.kt` (6 états) — géré par `PlayerViewModel.uiState`
- **Player** : `PlayerViewModel.initializePlayer()` — ExoPlayer + buffer config
- **Sécurité** : `PermissionHelper.verifySignature()` + `PlayerActivity.onCreate()` guard
- **Config build** : `app/build.gradle.kts` (flavors, signing, AdMob IDs)
- **Tests** : `app/src/test/` — 38 tests (PlayerController, AdsController, PlayerUiState, VideoItem)
- **Persistance** : `VideoRepository.kt` (data/) — prêt pour migration Room

---

*Rapport généré le 8 mars 2026 — GitHub Copilot*

