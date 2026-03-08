# 🎬 ANALYSE DU FLUX DE DÉMARRAGE DU PLAYER & LOGIQUE ADS

**Date** : 8 mars 2026  
**Auteur** : GitHub Copilot  
**Projet** : STV Player v1.0

---

## 1. DIAGRAMME COMPLET DU FLUX DE DÉMARRAGE

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DÉMARRAGE DE PlayerActivity                        │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
                        ┌─────────────────────────┐
                        │ 1. onCreate()            │
                        │    ┌───────────────────┐ │
                        │    │ Vérif. sécurité   │ │ ← PermissionHelper
                        │    │ (callerPackage)   │ │    vérifie signature
                        │    └────────┬──────────┘ │
                        └─────────────┤────────────┘
                               ┌──────┴──────┐
                          Non autorisé    Autorisé
                               │              │
                               ▼              ▼
                     ┌──────────────┐  ┌────────────────────────┐
                     │ Écran BLOCK  │  │ 2. Initialisation      │
                     │ + btn Close  │  │    MobileAds.init()    │
                     │ → finish()   │  │    AdManager(this)     │
                     └──────────────┘  │    AdsController()     │
                                       │    PlayerController()  │
                                       │    hideSystemUI()      │
                                       └───────────┬────────────┘
                                                   │
                                                   ▼
                                       ┌────────────────────────┐
                                       │ 3. Résolution URL      │
                                       │    resolveVideoUrl()   │
                                       │    + validateStreamUrl()│
                                       └───────────┬────────────┘
                                            ┌──────┴──────┐
                                     URL invalide      URL valide
                                            │              │
                                            ▼              ▼
                                ┌────────────────┐  ┌──────────────┐
                                │ Error(message) │  │ skipAds ?    │
                                │ → ErrorScreen  │  └──┬───────────┘
                                └────────────────┘     │
                                              ┌────────┴────────┐
                                          skipAds=true     skipAds=false
                                              │                 │
                                              ▼                 ▼
                                    ┌──────────────┐  ┌──────────────────┐
                                    │ Ready(url)   │  │ LoadingAds       │
                                    │ → Player     │  │ → Spinner rouge  │
                                    └──────────────┘  └────────┬─────────┘
                                                               │
                          ┌────────────────────────────────────┤
                          │          LaunchedEffect            │
                          │   adsController.showAdIfNeeded()   │
                          │        timeout: 6000ms             │
                          └────────────────┬───────────────────┘
                                           │
              ┌────────────────────────────┼────────────────────────────┐
              │                            │                            │
      ┌───────┴───────┐          ┌─────────┴─────────┐        ┌───────┴───────┐
      │ AdResult      │          │ AdResult           │        │ AdResult      │
      │ .AdShowed     │          │ .FallbackBanner    │        │ .AdBlockDetect│
      └───────┬───────┘          │ .Timeout           │        └───────┬───────┘
              │                  │ .AdDismissed(rare) │                │
              ▼                  └─────────┬─────────┘                ▼
    ┌──────────────────┐                   │               ┌──────────────────┐
    │ ShowingAd(url)   │                   ▼               │ Blocked          │
    │ → Écran noir     │         ┌──────────────────┐      │ → Icône Block   │
    │ + spinner discret│         │ Fallback(false)  │      │ → Message       │
    └────────┬─────────┘         │ → FallbackBanner │      │ → PAS de player │
             │                   │ → Bannière AdMob │      └──────────────────┘
             │                   │ → Countdown 5s   │
    onAdDismissed()              │ → Spinner progrès│
             │                   └────────┬─────────┘
             │                            │
             │                    onFinish() (5s)
             │                            │
             ▼                            ▼
    ┌─────────────────────────────────────────────┐
    │              Ready(url)                     │
    │                                             │
    │  LaunchedEffect(url) {                      │
    │    viewModel.initializePlayer(url) ←────────│── ExoPlayer créé ICI
    │  }                                          │
    │                                             │
    │  → VideoPlayer() composable                 │
    │  → PlayerControls() overlay                 │
    │  → QualitySelectionDialog()                 │
    └─────────────────────────────────────────────┘
```

---

## 2. CHRONOLOGIE DÉTAILLÉE (Timeline)

### Scénario A : Pub interstitielle réussie (cas idéal)

| Temps | Étape | Code | Écran utilisateur |
|-------|-------|------|-------------------|
| T+0ms | `onCreate()` | Vérif sécurité | Écran noir |
| T+10ms | `MobileAds.initialize()` | Init SDK AdMob | Écran noir |
| T+20ms | `resolveVideoUrl()` | Extraction URL intent | Écran noir |
| T+30ms | `setContent { ... }` | State = `LoadingAds` | **Spinner rouge** |
| T+50ms | `LaunchedEffect` | `showAdIfNeeded()` lance | Spinner rouge |
| T+50ms→2000ms | `AdManager.loadAndShowInterstitial()` | Requête réseau AdMob | Spinner rouge |
| T+2000ms | `onAdLoaded()` → `show()` | Pub plein écran | **Pub interstitielle** |
| T+2000ms | Callback `onAdShowed` | State = `ShowingAd(url)` | Pub (écran noir derrière) |
| T+7000ms | Utilisateur ferme la pub | Callback `onAdDismissed` | — |
| T+7000ms | State = `Ready(url)` | `initializePlayer()` | **Chargement vidéo** |
| T+8500ms | Buffer 1.5s atteint | `playWhenReady=true` | **🎬 Lecture vidéo** |

**⏱️ Temps total avant lecture : ~8.5 secondes** (dont ~5s de pub)

---

### Scénario B : Pub échoue → Fallback bannière

| Temps | Étape | Écran utilisateur |
|-------|-------|-------------------|
| T+0ms | `onCreate()` | Écran noir |
| T+30ms | State = `LoadingAds` | **Spinner rouge** |
| T+50ms→3000ms | `loadAndShowInterstitial()` échoue | Spinner rouge |
| T+3000ms | `onFallbackAd()` → State = `Fallback` | **Bannière AdMob + countdown 5s** |
| T+8000ms | `onFinish()` (5s écoulées) | — |
| T+8000ms | State = `Ready(url)` | **Chargement vidéo** |
| T+9500ms | Buffer 1.5s | **🎬 Lecture vidéo** |

**⏱️ Temps total avant lecture : ~9.5 secondes** (pire cas non bloquant)

---

### Scénario C : Timeout (6s) → Fallback bannière

| Temps | Étape | Écran utilisateur |
|-------|-------|-------------------|
| T+0ms→30ms | Init | Écran noir |
| T+30ms | State = `LoadingAds` | **Spinner rouge** |
| T+6030ms | `TimeoutCancellationException` | — |
| T+6030ms | State = `Fallback` | **Bannière + countdown 5s** |
| T+11030ms | `onFinish()` | — |
| T+11030ms | State = `Ready(url)` | **Chargement vidéo** |
| T+12530ms | Buffer 1.5s | **🎬 Lecture vidéo** |

**⏱️ Temps total avant lecture : ~12.5 secondes** ⚠️ (pire cas)

---

### Scénario D : Adblock détecté (3 échecs consécutifs)

| Temps | Étape | Écran utilisateur |
|-------|-------|-------------------|
| T+30ms | State = `LoadingAds` | **Spinner rouge** |
| T+3000ms | `failedLoadAttempts >= 3` | — |
| T+3000ms | `onAdBlockDetected` | — |
| T+3000ms | State = `Blocked` | **Icône ⛔ + message bloquant** |
| ∞ | **PAS DE LECTURE** | Utilisateur doit désactiver l'adblock |

---

## 3. ANALYSE DE LA STATE MACHINE

### 3.1 Diagramme d'états

```
                  ┌──────────┐
                  │   Error  │ (état terminal — URL invalide)
                  └──────────┘

     ┌───────────────────────────────────────────────┐
     │                                               │
     │  ┌────────────┐    ┌────────────┐             │
     │  │ LoadingAds │───►│ ShowingAd  │─── onAdDismissed ──►┌───────┐
     │  └────┬───────┘    └────────────┘                     │ Ready │
     │       │                                               └───────┘
     │       ├─── FallbackBanner/Timeout ──►┌──────────┐         ▲
     │       │                              │ Fallback │── 5s ───┘
     │       │                              └──────────┘
     │       │
     │       └─── AdBlockDetected ──►┌─────────┐
     │                               │ Blocked │ (état terminal)
     │                               └─────────┘
     │                                               │
     └───────────────────────────────────────────────┘
```

### 3.2 Évaluation de la state machine

| Critère | Note | Commentaire |
|---------|------|-------------|
| Exhaustivité | ✅ 9/10 | Couvre tous les cas : succès, échec, timeout, adblock, erreur |
| Transitions | ✅ 8/10 | Claires et unidirectionnelles |
| Immutabilité | ✅ | `sealed class` garantit l'exhaustivité du `when` |
| Pas de boucle | ✅ | Pas de retour en arrière (pas de LoadingAds → Fallback → LoadingAds) |
| Thread-safety | ⚠️ 6/10 | `uiState` est un `mutableStateOf` local, pas un `StateFlow` dans le ViewModel |

---

## 4. ANALYSE DE LA LOGIQUE ADS (AdManager + AdsController)

### 4.1 Architecture actuelle

```
PlayerActivity                    AdsController                     AdManager
     │                                │                                │
     │  showAdIfNeeded()              │                                │
     │──────────────────────────────►│                                │
     │                                │  loadAndShowInterstitial()    │
     │                                │──────────────────────────────►│
     │                                │                                │
     │                                │           ┌── onAdShowed      │
     │                                │◄──────────┤── onAdDismissed   │
     │                                │           ├── onFallbackAd    │
     │                                │           └── onAdBlockDetected│
     │                                │                                │
     │  ◄── AdResult (sealed class)  │                                │
     │                                │                                │
```

### 4.2 Points forts ✅

| # | Point fort | Détail |
|---|------------|--------|
| 1 | **State machine sealed** | `PlayerUiState` est exhaustive, pas de state incohérent possible |
| 2 | **Timeout avec coroutines** | `withTimeout(6000)` empêche un blocage infini |
| 3 | **Découplage** | `AdsController` isole la logique ads de l'UI |
| 4 | **Fallback intelligent** | "No Fill" (code 3) ne compte pas comme échec (pas de faux positif adblock) |
| 5 | **Seuil progressif** | 3 échecs avant de bloquer (tolérance) |
| 6 | **Persistance compteur** | `SharedPreferences` pour le compteur d'échecs (survit aux kills) |
| 7 | **Player non démarré pendant pub** | Pas de son en arrière-plan (état `ShowingAd` = écran noir) |
| 8 | **skipAds flag** | Permet de bypass les ads pour les intents internes |

---

## 5. PROBLÈMES IDENTIFIÉS & AMÉLIORATIONS PROPOSÉES

### 🔴 Problème 1 : `MobileAds.initialize()` est appelé DEUX FOIS (doublon) — ✅ CORRIGÉ

**Constat** :
- `MainActivity.onCreate()` ligne 106 : `MobileAds.initialize(this) {}`
- `PlayerActivity.onCreate()` ligne 119 : `MobileAds.initialize(this) {}`

**Impact** : Le SDK AdMob est initialisé une première fois dans `MainActivity`, puis **re-initialisé** dans `PlayerActivity`. Bien que Google garantisse que les appels suivants sont ignorés (no-op), c'est :
- Un travail inutile (vérification interne du SDK)
- Un risque si l'utilisateur arrive directement via deep link (PlayerActivity en premier) — ici ça marche, mais c'est accidentel

**Recommandation** : Déplacer `MobileAds.initialize()` dans la classe `Application` pour garantir une **seule initialisation globale** au démarrage de l'app.

```
AVANT :  MainActivity.init() → PlayerActivity.init() (doublon)
APRÈS :  Application.init() → utilisé partout
```

**✅ Correction appliquée le 8 mars 2026** :
- Créé `STVApplication.kt` avec `MobileAds.initialize()` dans `onCreate()`
- Déclaré `android:name=".STVApplication"` dans `AndroidManifest.xml`
- Retiré `MobileAds.initialize()` de `MainActivity.kt` et `PlayerActivity.kt`
- Retiré les imports `MobileAds` inutilisés des deux Activities
- La logique d'affichage des ads (AdsController → AdManager) reste inchangée
- Fonctionne pour tous les points d'entrée : launcher, deep link, intent catalogue

---

### 🔴 Problème 2 : `onStart()` appelle `viewModel.play()` AVANT que le player soit initialisé — ✅ CORRIGÉ

**Constat** :
```kotlin
override fun onStart() {
    super.onStart()
    viewModel.play()  // ← ExoPlayer est NULL ici ! (pas encore Ready)
}
```

**Chronologie du problème** :
```
onCreate() → setContent() → LoadingAds → [pub en cours]
   │
   └── onStart() appelé IMMÉDIATEMENT après onCreate()
       └── viewModel.play() → _exoPlayer?.play() → null?.play() → NOP (silencieux)
```

Le player n'est initialisé que dans `LaunchedEffect(url) { viewModel.initializePlayer(url) }` qui se déclenche seulement quand `uiState = Ready(url)`. Mais `onStart()` est appelé **bien avant**.

**Impact actuel** : Aucun crash (le `?.` protège), mais c'est un appel inutile et un signe de mauvaise gestion du lifecycle.

**Impact potentiel** : Si l'écran tourne ou si l'activity est recréée pendant la phase ads, `onResume()` appellera aussi `play()` inutilement.

**Recommandation** : Conditionner `play()` dans `onStart()`/`onResume()` à l'état `Ready` :

```kotlin
override fun onStart() {
    super.onStart()
    if (viewModel.exoPlayer != null) viewModel.play()
}
```

**✅ Correction appliquée le 8 mars 2026** :
- `onStart()` : `play()` conditionné à `viewModel.exoPlayer != null`
- `onResume()` : `play()` conditionné à `viewModel.exoPlayer != null`
- `onPause()` : `pause()` conditionné à `viewModel.exoPlayer != null`
- `onStop()` : `pause()` conditionné à `viewModel.exoPlayer != null`
- Pendant la phase ads (LoadingAds/ShowingAd/Fallback), le player est null → aucun appel inutile

---

### 🟡 Problème 3 : `uiState` est local au composable, pas dans le ViewModel — ✅ CORRIGÉ

**Constat** :
```kotlin
// PlayerActivity.kt, dans setContent { }
var uiState by remember { mutableStateOf<PlayerUiState>(...) }
```

L'état UI est un `mutableStateOf` **local** dans le composable. Il n'est **pas** dans le `PlayerViewModel`.

**Impact** :
- Si l'Activity est recréée (rotation, mais `sensorLandscape` l'empêche en pratique), l'état est perdu
- Pas testable unitairement (l'état est lié au composable, pas au ViewModel)
- Incohérence avec le pattern MVVM du reste de l'app

**Recommandation** : Déplacer `uiState` dans `PlayerViewModel` comme `StateFlow<PlayerUiState>`.

**✅ Correction appliquée le 8 mars 2026** :
- Ajouté `_uiState: MutableStateFlow<PlayerUiState>` dans `PlayerViewModel`
- Ajouté `initializeUiState()` pour l'initialisation (URL, erreur, skipAds)
- Ajouté `setUiState()` pour les transitions (callbacks ads, fallback)
- `PlayerActivity` utilise maintenant `viewModel.uiState.collectAsState()` au lieu de `remember { mutableStateOf }` 
- Tous les `uiState = ...` remplacés par `viewModel.setUiState(...)`
- L'état survit aux recompositions et est testable unitairement

---

### 🟡 Problème 4 : Fallback bannière = 5 secondes FIXES (pas adaptatif) — ✅ CORRIGÉ

**Constat** :
```kotlin
// FallbackBanner.kt
var timeLeft by remember { mutableLongStateOf(5L) }
```

Le countdown est **toujours 5 secondes**, même si :
- La bannière se charge en 0.5s → l'utilisateur attend 4.5s inutilement
- La bannière ne se charge jamais → l'utilisateur voit un espace vide pendant 5s

**Impact UX** : L'utilisateur perçoit 5s de "rien" comme une latence inutile. C'est frustrant sur des connexions rapides.

**Recommandation** : Rendre le countdown adaptatif :
- **Minimum** : 3 secondes (pour laisser la bannière apparaître et être vue)
- **Fin anticipée** : Si la bannière a été affichée pendant au moins 3s, terminer dès que l'utilisateur tape l'écran
- **Skip button** : Ajouter un bouton "Passer" qui apparaît après 3s (comme YouTube)

**✅ Correction appliquée le 8 mars 2026** — Approche gagnante (Ads payées + UX + connexions lentes) :
- Ajouté `AdListener` sur la bannière pour détecter `onAdLoaded()` / `onAdFailedToLoad()`
- Ajouté **timeout global 5s** pour le chargement de la bannière (protège contre les connexions lentes)
- **Bannière charge en < 5s** → countdown 5s supplémentaires (impression AdMob comptée = revenus)
- **Bannière échoue rapidement** → skip immédiat vers le player (pas d'attente vide)
- **Bannière trop lente (> 5s)** → skip automatique (connexion trop mauvaise, pas d'attente infinie)
- **Pendant le chargement** → spinner + message d'attente (feedback visuel)
- Countdown et texte "Lancement dans Xs" visibles seulement si bannière affichée
- **Aucun risque de blocage infini** : timeout global garantit une sortie dans tous les cas

---

### 🔴 Problème 10b : Player démarre en arrière-plan pendant la pub — ✅ CORRIGÉ

**Constat** : Après l'ajout du préchargement (Problème 5), le player pouvait démarrer en arrière-plan pendant que la pub était affichée.

**Cause racine** : `preloadPlayer()` crée l'ExoPlayer (volume=0, playWhenReady=false). Mais quand l'utilisateur ferme la pub interstitielle, Android appelle `onResume()` sur PlayerActivity. La vérification `viewModel.exoPlayer != null` retournait `true` (car le player existe en préchargement), ce qui déclenchait `viewModel.play()` → le player démarrait en lecture réelle pendant la pub.

**Correction appliquée le 8 mars 2026** :
- Ajouté `val isPreloading: Boolean` dans PlayerViewModel (expose le flag `isPreloaded` en lecture seule)
- `onStart()`, `onResume()`, `onPause()`, `onStop()` vérifient maintenant `!viewModel.isPreloading` en plus de `exoPlayer != null`
- Le player en préchargement (mute+pause) n'est jamais démarré accidentellement par le lifecycle

---

### 🟡 Problème 5 : Pas de préchargement du flux vidéo pendant la pub — ✅ CORRIGÉ

**Constat** : Pendant que la pub interstitielle est affichée (`ShowingAd`), **rien ne se passe côté vidéo**. Le player est initialisé seulement **après** la fermeture de la pub.

```
ShowingAd  →  Ready  →  initializePlayer()  →  prepare()  →  buffer 1.5s  →  lecture
              ▲                                    │
              │                              Requête réseau
              └────── Le temps d'attente ici est PERDU
```

**Impact UX** : L'utilisateur attend la pub (5-30s) + le buffer (1.5-3s). Le temps de la pub est "perdu".

**Recommandation** : Précharger le flux vidéo en arrière-plan (mute + pause) **pendant** la pub :

```
ShowingAd + initializePlayer(muted, paused)  →  Ready  →  play() instantané
```

Cela permettrait une **lecture instantanée** dès la fermeture de la pub (0s de buffer au lieu de 1.5-3s).

**Mise en garde** : Cela consomme de la bande passante pendant la pub. Acceptable sur WiFi, à limiter sur données mobiles.

**✅ Correction appliquée le 8 mars 2026** :
- Ajouté `preloadPlayer(url)` dans `PlayerViewModel` : crée ExoPlayer en mode mute (volume=0) + pause (playWhenReady=false), lance `prepare()` pour télécharger le manifeste et bufferiser
- Modifié `initializePlayer(url)` : si le player est déjà préchargé avec la même URL, fait simplement `volume=1f` + `playWhenReady=true` → **lecture instantanée**
- Dans `PlayerActivity`, l'état `ShowingAd` lance `preloadPlayer()` via `LaunchedEffect` → le buffer se remplit pendant que la pub est affichée
- Ajouté flag `isPreloaded` pour distinguer préchargement vs lecture normale, réinitialisé dans `releasePlayer()`
- **Gain estimé : 1.5 à 3 secondes** de temps de chargement éliminé après fermeture de la pub

---

### 🟡 Problème 6 : `AdManager` crée un nouvel objet à chaque `PlayerActivity` — ✅ CORRIGÉ

**Constat** :
```kotlin
// MainActivity.kt
adManager = AdManager(this)
adManager.loadInterstitialAd()  // Précharge dans MainActivity

// PlayerActivity.kt
adManager = AdManager(this)     // ← NOUVEL objet ! Le préchargement est perdu !
```

`MainActivity` précharge l'interstitiel, mais `PlayerActivity` crée un **nouvel** `AdManager`. Le préchargement fait dans `MainActivity` est donc **complètement inutile**.

**Impact** : 
- Le préchargement dans `MainActivity` est un gaspillage de bande passante
- Le player doit toujours charger la pub from scratch (pas de cache)
- Temps de chargement plus long

**Recommandation** : Utiliser un `AdManager` **singleton** (ou injecté via DI) partagé entre les Activities :

```kotlin
object AdManager {  // ou class avec @Singleton si Hilt
    // ...
}
```

**✅ Correction appliquée le 8 mars 2026** :
- Transformé `AdManager` en **singleton thread-safe** (constructeur privé + companion object + double-checked locking)
- Ajouté `AdManager.initialize(context)` dans `STVApplication.onCreate()` (une seule initialisation)
- Ajouté `AdManager.instance.loadInterstitialAd()` dans `STVApplication.onCreate()` (préchargement au démarrage)
- `MainActivity` utilise `AdManager.instance` au lieu de `AdManager(this)`, retire le `loadInterstitialAd()` redondant
- `PlayerActivity` utilise `AdManager.instance` → la pub préchargée depuis `STVApplication` est **réutilisée**
- `AdsController` reçoit toujours `adManager` en paramètre → aucun changement nécessaire
- **Gain** : La pub est potentiellement déjà prête quand `PlayerActivity` démarre → affichage immédiat (0ms de chargement au lieu de 2000ms)

---

### 🟢 Problème 7 : `validateStreamUrl()` renvoie des messages en français en dur

**Constat** (dans `PlayerController.kt`) :
```kotlin
fun validateStreamUrl(url: String?): String? {
    if (url.isNullOrBlank()) return "URL de flux manquante"           // ← FR en dur
    if (!trimmedUrl.startsWith("http://")) return "Schéma invalide..." // ← FR en dur
    if (trimmedUrl.length > 2048) return "URL trop longue..."          // ← FR en dur
    if (!Patterns.WEB_URL.matcher...) return "Format d'URL invalide"   // ← FR en dur
}
```

**Impact** : Ces messages sont visibles dans `ErrorScreen` mais ne sont pas traduits (anglais ni aucune autre langue).

**Recommandation** : `PlayerController` n'a pas accès au `Context`, donc deux options :
1. Retourner un **code d'erreur** (enum) et résoudre le message dans le composable avec `stringResource()`
2. Passer le `Context` au `PlayerController`

---

### 🟢 Problème 8 : Pas d'animation de transition entre les états

**Constat** : Les transitions entre `LoadingAds → ShowingAd → Ready` sont **brutales** (coupure instantanée).

**Impact UX** : L'utilisateur perçoit des "flashs" entre les écrans (noir → spinner → pub → noir → vidéo).

**Recommandation** : Ajouter des `AnimatedContent` ou `Crossfade` pour des transitions douces entre les états.

---

### 🟢 Problème 9 : Pas de vérification réseau avant de lancer les ads

---

### 🔴 Problème 10 : Pubs superposées lors du changement de flux en PiP — ✅ CORRIGÉ

**Constat** : Quand le player est en mode PiP et que l'utilisateur clique sur un autre lien :
1. `onNewIntent()` appelle `recreate()` → nouveau cycle `onCreate()` complet
2. Le singleton `AdManager` charge une pub ET en a potentiellement une déjà prête
3. `onAdDismissedFullScreenContent()` précharge immédiatement la pub suivante via `loadInterstitialAd()`
4. La pub suivante est prête instantanément → s'affiche avant que le player ne démarre
5. L'utilisateur voit 2 pubs ou plus se superposer

**3 causes racines identifiées** :
- `onNewIntent()` → `recreate()` relance tout le cycle ads (au lieu de réinitialiser)
- `loadAndShowInterstitial()` charge toujours une nouvelle pub (ignore la préchargée)
- `onAdDismissed` précharge immédiatement la suivante → conflit de timing

**Corrections appliquées le 8 mars 2026** :

1. **`onNewIntent()`** : Remplacé `recreate()` par une réinitialisation propre :
   - `viewModel.releasePlayer()` → libère l'ExoPlayer actuel
   - `viewModel.initializeUiState()` → réinitialise la state machine avec la nouvelle URL
   - Pas de recréation d'Activity → pas de double cycle ads

2. **`loadAndShowInterstitial()`** : Ajouté vérification `if (interstitialAd != null)` en début de méthode → réutilise la pub déjà prête au lieu d'en charger une nouvelle

3. **`onAdDismissedFullScreenContent()`** : Le préchargement de la prochaine pub est maintenant **différé de 2 secondes** via `Handler.postDelayed()` → laisse le temps au player de démarrer avant de précharger

4. **`PlayerUiState.LoadingAds` et `Fallback`** : Ajouté le champ `videoUrl` pour que l'URL soit toujours portée par l'état (pas capturée dans une variable locale de `onCreate()`) → supporte correctement `onNewIntent()`

**Constat** : `PlayerActivity` ne vérifie **jamais** si le réseau est disponible avant de tenter de charger les ads. `MainActivity` a `isNetworkAvailable()` mais `PlayerActivity` ne l'utilise pas.

**Impact** : Si pas de réseau → timeout de 6s → Fallback 5s → tentative de lecture → erreur ExoPlayer. Total : **11 secondes d'attente inutile**.

**Recommandation** : Vérifier le réseau dès l'entrée dans `PlayerActivity` :
- Si pas de réseau → `skipAds = true` + afficher un avertissement toast
- Évite 11 secondes de latence inutile

---

## 6. SYNTHÈSE DES RECOMMANDATIONS

### Par priorité d'impact UX :

| # | Amélioration | Impact UX | Complexité | Priorité |
|---|-------------|-----------|------------|----------|
| 1 | **AdManager singleton** (préchargement partagé) | ⭐⭐⭐⭐⭐ | Moyenne | 🔴 Haute |
| 2 | **Préchargement vidéo pendant la pub** | ⭐⭐⭐⭐⭐ | Moyenne | 🔴 Haute |
| 3 | **Vérif réseau avant ads** (skip si offline) | ⭐⭐⭐⭐ | Facile | 🔴 Haute |
| 4 | **Fallback adaptatif** (3s min + skip) | ⭐⭐⭐⭐ | Facile | 🟡 Moyenne |
| 5 | **Fix onStart/onResume** (pas de play() avant Ready) | ⭐⭐⭐ | Facile | 🟡 Moyenne |
| 6 | **MobileAds.init() dans Application** | ⭐⭐ | Facile | 🟡 Moyenne |
| 7 | **uiState dans ViewModel** | ⭐⭐ | Moyenne | 🟢 Basse |
| 8 | **Messages validation i18n** (PlayerController) | ⭐⭐ | Facile | 🟢 Basse |
| 9 | **Animations de transition** | ⭐ | Facile | 🟢 Basse |

### Estimation du gain avec toutes les améliorations :

```
AVANT (scénario A - pub réussie) :
  Init(30ms) + LoadAds(2000ms) + Pub(5000ms) + Init player(100ms) + Buffer(1500ms) = ~8.5s

APRÈS (optimisé) :
  Init(30ms) + LoadAds(0ms préchargé!) + Pub(5000ms) + Play instantané(0ms) = ~5s
                                                        ▲
                                        Préchargement pendant la pub
                                        → Buffer déjà rempli → 0ms

GAIN : ~3.5 secondes (41% plus rapide)
```

```
AVANT (scénario C - timeout) :
  Init(30ms) + Timeout(6000ms) + Fallback(5000ms) + Init(100ms) + Buffer(1500ms) = ~12.5s

APRÈS (avec vérif réseau + AdManager singleton) :
  Init(30ms) + LoadAds(0ms préchargé!) + Pub affichée(5000ms) + Play(0ms) = ~5s
  OU si pas de réseau : Init(30ms) + Skip ads + Init player(100ms) + Erreur immédiate

GAIN : ~7.5 secondes (60% plus rapide)
```

---

## 7. ARCHITECTURE CIBLE RECOMMANDÉE

```
┌─────────────────────────────────────────────────────────────────────┐
│                         STVApplication                             │
│   onCreate() {                                                      │
│     MobileAds.initialize(this)  ← UNE SEULE FOIS                  │
│     AdManager.init(this)        ← Singleton partagé                │
│   }                                                                 │
└───────────────────────────────────┬─────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
              ┌─────┴─────┐                   ┌─────┴─────┐
              │ MainActivity│                 │PlayerActivity│
              │             │                 │             │
              │ adManager   │──── même ────►│ adManager   │
              │ .preload()  │   instance     │ .show()     │
              └─────────────┘                └─────────────┘

                                    ┌──────────────────────┐
              PlayerViewModel       │                      │
              ┌─────────────┐       │    AdsController     │
              │ uiState     │◄──────│    (inchangé)        │
              │ exoPlayer   │       │                      │
              │ adResult    │       └──────────────────────┘
              └─────────────┘
```

---

## 8. REFONTE RADICALE DU FLUX ADS — 8 mars 2026

### Problèmes persistants avant cette refonte :
Malgré plusieurs corrections itératives, 3 bugs persistaient :
1. **2 pubs superposées** : le préchargement dans `STVApplication` + le chargement dans `AdsController` créaient 2 pubs
2. **Spinner infini** : quand 2 pubs s'affichaient, la continuation `suspendCancellableCoroutine` n'était jamais resumée correctement
3. **Player en arrière-plan pendant la pub** : `preloadPlayer()` créait ExoPlayer, et `onResume()` après la pub déclenchait `play()`

### Décision : Architecture minimaliste, ZÉRO cache, ZÉRO préchargement

Tous les mécanismes complexes (préchargement pub, préchargement vidéo, cache singleton, postDelayed) ont été **supprimés**. Un seul flux linéaire reste.

### Code supprimé :

| Élément supprimé | Fichier | Pourquoi |
|-----------------|---------|----------|
| `loadInterstitialAd()` (préchargement) | AdManager.kt | Source de la 2e pub |
| `interstitialAd` (variable cache) | AdManager.kt | Plus de cache |
| `isAdCurrentlyShowing` (guard) | AdManager.kt | Plus nécessaire (un seul flux) |
| `postDelayed(loadInterstitialAd)` | AdManager.kt | Source de la 2e pub |
| `preloadPlayer()` | PlayerViewModel.kt | Source du player en arrière-plan |
| `isPreloaded` / `isPreloading` | PlayerViewModel.kt | Plus de préchargement |
| `FallbackBanner` composable (~110 lignes) | PlayerActivity.kt | Supprimé |
| `PlayerUiState.Fallback` | PlayerUiState.kt | Supprimé |
| `AdResult.AdShowed` / `FallbackBanner` | AdsController.kt | Simplifié |
| `AdManager.instance.loadInterstitialAd()` | STVApplication.kt | Plus de préchargement au démarrage |
| `adManager` dans MainActivity | MainActivity.kt | Plus utilisé |

### Nouvelle architecture (ultra-simple) :

```
STVApplication.onCreate()
  └── MobileAds.initialize() + AdManager.initialize()  (init seulement, PAS de préchargement)

PlayerActivity.onCreate()
  └── adsController = AdsController(AdManager.instance)

LaunchedEffect(LoadingAds)
  └── adsController.showAdIfNeeded(timeout=8s)
       └── AdManager.loadAndShow()
            ├── InterstitialAd.load()  ← UN SEUL chargement
            ├── ad.show()              ← UN SEUL affichage
            └── callbacks :
                 ├── onAdShowed → uiState = ShowingAd (UI seulement, pas de resume)
                 ├── onAdDismissed → resume(AdDismissed) → uiState = Ready → initializePlayer()
                 ├── onAdNotAvailable → resume(AdNotAvailable) → uiState = Ready → initializePlayer()
                 └── onAdBlockDetected → resume(AdBlockDetected) → uiState = Blocked
```

### Garanties :
- **0 variable d'état** dans AdManager (pas de `interstitialAd`, pas de `isAdCurrentlyShowing`)
- **0 préchargement** (ni pub, ni vidéo)
- **0 postDelayed** (pas de timer caché)
- **1 seul InterstitialAd.load()** par lancement de flux
- **1 seul continuation.resume()** par cycle (dans onAdDismissed, onAdNotAvailable, ou onAdBlockDetected)
- **ExoPlayer créé UNIQUEMENT dans l'état Ready** (jamais pendant ShowingAd)
- **onStart/onResume** vérifient `uiState is Ready` avant d'appeler `play()`

### State machine finale (4 états + null initial) :

```
null (ViewModel créé, pas encore initialisé)
  │
  └── initializeUiState() dans onCreate() AVANT setContent
       │
       ▼
LoadingAds(url) ──► ShowingAd(url) ──► Ready(url)
     │                                      ▲
     ├── AdNotAvailable ────────────────────┘
     ├── Timeout ───────────────────────────┘
     └── AdBlockDetected ──► Blocked

Error(message) ← URL invalide (état terminal)
```

### 🔴 Problème 11 : Player démarre AVANT la pub (bug de timing LaunchedEffect) — ✅ CORRIGÉ

**Constat** : Le player démarrait avant la pub, puis la pub se chargeait par-dessus, et après sa fermeture l'écran restait noir.

**Cause racine** : Double `LaunchedEffect` avec timing incorrect :
1. `_uiState` initialisé à `LoadingAds("")` (URL VIDE) dans le ViewModel
2. `LaunchedEffect(uiState)` se déclenchait immédiatement avec `LoadingAds("")`
3. `currentVideoUrl = ""` passait la condition `!= null` → `showAdIfNeeded()` lancé avec URL vide
4. `LaunchedEffect(Unit)` s'exécutait ensuite → `initializeUiState(vraiUrl)` → `LoadingAds(vraiUrl)`
5. `LaunchedEffect(uiState)` se re-déclenchait → DEUXIÈME appel avec la vraie URL
6. Les callbacks s'emmêlaient → la continuation n'était jamais resumée correctement → écran noir

**Corrections appliquées le 8 mars 2026** :
1. `_uiState` initialisé à `null` (pas `LoadingAds("")`) → pas de faux déclenchement
2. `initializeUiState()` appelé dans `onCreate()` AVANT `setContent` (pas dans un `LaunchedEffect`)
3. Le composable affiche un écran noir tant que `uiState == null`
4. UN SEUL `LaunchedEffect(currentState)` pour les ads → se déclenche avec la vraie URL
5. Ajouté `isReady()` dans ViewModel pour simplifier les vérifications lifecycle

---

### 🔴 Problème 12 : Player démarre sans que la pub soit vue — ✅ CORRIGÉ

**Constat** : Quand la pub n'était pas disponible (timeout, no fill), le player démarrait quand même (`AdNotAvailable → Ready`). L'utilisateur accédait au contenu sans avoir vu de pub.

**Exigence** : Le player ne doit démarrer **QUE** si l'utilisateur a vu et fermé la pub.

**Correction appliquée le 8 mars 2026** :
- `AdsController` : Ajouté boucle de retry (max 3 tentatives, délai 2s entre chaque)
- Si la pub n'est pas disponible → on réessaie (pas de passage en Ready)
- Si 3 tentatives échouent → `AdBlockDetected` → Blocked (pas de player)
- Seul `AdDismissed` (pub vue + fermée) permet de passer en `Ready`
- `AdResult.Timeout` supprimé → un timeout = `AdNotAvailable` = réessai
- Le `when` dans `PlayerActivity` ne connaît que 2 résultats : `AdDismissed → Ready` ou `AdBlockDetected → Blocked`

---

### 🔴 Problème 13 : Écran noir après fermeture de la pub (LaunchedEffect annulé) — ✅ CORRIGÉ

**Constat** : Après la fermeture de la pub, le player ne démarrait jamais — l'écran restait noir indéfiniment.

**Cause racine** : `LaunchedEffect(currentState)` utilisait `currentState` comme **clé de recomposition**. Quand `onAdShowed` changeait l'état de `LoadingAds` vers `ShowingAd`, Compose **annulait** le `LaunchedEffect` en cours (car la clé avait changé) et en relançait un nouveau. La coroutine `showAdIfNeeded()` était donc **annulée au milieu**, ce qui signifiait que plus personne n'écoutait le callback `onAdDismissed`.

```
AVANT (BUGUÉ) :
  LaunchedEffect(currentState = LoadingAds) → showAdIfNeeded() démarre
       │
  onAdShowed → setUiState(ShowingAd) → currentState change !
       │
  Compose ANNULE le LaunchedEffect (clé changée) → coroutine MORTE
       │
  Utilisateur ferme la pub → onAdDismissed → continuation déjà annulée → RIEN
       │
  Écran noir pour toujours ❌
```

**Correction appliquée le 8 mars 2026** :
- `LaunchedEffect(Unit)` au lieu de `LaunchedEffect(currentState)` → la coroutine n'est **jamais annulée** par un changement d'état
- La coroutine lit `viewModel.uiState.value` une fois au démarrage, lance le flux ads, et attend le résultat final
- Les transitions UI (`ShowingAd`) se font via `setUiState()` sans affecter la coroutine qui attend `onAdDismissed`

```
APRÈS (CORRIGÉ) :
  LaunchedEffect(Unit) → showAdIfNeeded() démarre
       │
  onAdShowed → setUiState(ShowingAd) → l'UI change mais la coroutine CONTINUE
       │
  Utilisateur ferme la pub → onAdDismissed → continuation.resume(AdDismissed) ✅
       │
  setUiState(Ready) → ExoPlayer créé → lecture 🎬
```

---

### 🔴 Problème 14 : Adblock bloque l'utilisateur pour toujours (même après désactivation) — ✅ CORRIGÉ

**Constat** : Le compteur `failedLoadAttempts` était persisté dans `SharedPreferences`. Une fois le seuil de 3 atteint (adblock actif), même après désactivation de l'adblock et relancement de l'app, l'utilisateur restait bloqué pour toujours. De plus, un problème de réseau temporaire (ex : mode avion) comptait aussi comme "adblock".

**Corrections appliquées le 8 mars 2026** :

#### 1. Compteur en mémoire (pas SharedPreferences)
- `failedLoadAttempts` est maintenant une simple variable `Int` (pas persistée)
- Reset automatique à chaque démarrage de l'app
- L'utilisateur qui désactive son adblock et relance l'app repart à zéro

#### 2. Distinction réseau vs adblock dans AdManager
- Nouveau callback `onNoNetwork` dans `loadAndShow()`
- Vérification `isNetworkAvailable()` avant de charger la pub
- Si `ConnectivityManager` dit pas de réseau → `onNoNetwork()` (ne compte PAS comme adblock)
- Si `ERROR_CODE_NETWORK_ERROR` dans `onAdFailedToLoad()` → `onNoNetwork()` (ne compte PAS)
- Si internet OK mais pub échoue → `failedLoadAttempts++` → adblock probable

#### 3. Nouvel état `NetworkError` dans la state machine
```
LoadingAds(url) ──► ShowingAd(url) ──► Ready(url)  (pub vue et fermée)
     │
     ├── AdNotAvailable → retry (max 3)
     │       └── 3 échecs → AdBlockDetected → Blocked ⛔ (internet OK, pub bloquée)
     │
     └── NoNetwork → NetworkError(url) 📡 (pas d'internet, pas d'adblock)
```

#### 4. Bouton "Réessayer" sur Blocked et NetworkError
- Composable `BlockedScreen` réutilisable (icône + titre + message + bouton)
- `Blocked` : icône ⛔, message "bloqueur détecté", bouton "Réessayer"
- `NetworkError` : icône 📡, message "pas de connexion", bouton "Réessayer"
- `retryAds()` : remet le compteur à 0, remet l'état à `LoadingAds`, relance `launchAdFlow()`

#### 5. onNewIntent corrigé
- Appelle `launchAdFlow(newVideoUrl)` après la réinitialisation de l'état
- Utilise `getString(R.string.url_not_provided)` au lieu de string en dur

| Fichier | Changement |
|---------|-----------|
| **AdManager.kt** | Compteur en mémoire, `isNetworkAvailable()`, callback `onNoNetwork`, `resetFailures()` |
| **AdsController.kt** | Gestion `NoNetwork`, `resetAndRetry()` |
| **PlayerUiState.kt** | Ajout état `NetworkError(url)` |
| **PlayerActivity.kt** | `BlockedScreen` composable, `retryAds()`, `extractCurrentUrl()`, `onNewIntent` corrigé |
| **strings.xml** (EN+FR) | `no_network_title`, `no_network_message`, `retry_button` |

---

*Rapport mis à jour le 8 mars 2026 — GitHub Copilot*

