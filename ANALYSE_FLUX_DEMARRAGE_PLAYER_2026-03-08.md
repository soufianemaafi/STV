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

### 🔴 Problème 2 : `onStart()` appelle `viewModel.play()` AVANT que le player soit initialisé

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

---

### 🟡 Problème 3 : `uiState` est local au composable, pas dans le ViewModel

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

---

### 🟡 Problème 4 : Fallback bannière = 5 secondes FIXES (pas adaptatif)

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

---

### 🟡 Problème 5 : Pas de préchargement du flux vidéo pendant la pub

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

---

### 🟡 Problème 6 : `AdManager` crée un nouvel objet à chaque `PlayerActivity`

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

## 8. CONCLUSION

Le flux de démarrage actuel est **fonctionnel et sécurisé** :
- ✅ State machine robuste et exhaustive
- ✅ Timeout anti-blocage (6s)
- ✅ Fallback progressif (interstitiel → bannière → blocage)
- ✅ Le player ne démarre pas pendant la pub (pas de son parasite)

Les **améliorations prioritaires** sont :
1. **AdManager singleton** → élimine le doublon d'initialisation et permet le préchargement réel
2. **Préchargement vidéo pendant la pub** → lecture instantanée post-pub (gain de 1.5-3s)
3. **Vérification réseau** → évite 11s de latence si offline

Ces 3 améliorations combinées réduiraient le temps avant lecture de **~8.5s à ~5s** dans le cas idéal, et de **~12.5s à ~5s** dans le pire cas.

---

*Rapport généré le 8 mars 2026 — GitHub Copilot*

