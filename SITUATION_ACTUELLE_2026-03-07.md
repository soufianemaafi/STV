# 🚀 SITUATION ACTUELLE — STV Player v1.0

**Date** : 7 mars 2026  
**Objectif** : Résumé complet du progrès pour reprise rapide du développement  
**À lire en premier lors de la prochaine session de travail**

---

## 📌 EN UN COUP D'ŒIL

| Critère | Status |
|---------|--------|
| **Code** | ✅ Compilable, fonctionnel |
| **Dernière compilation réussie** | 1er mars 2026 (BUILD SUCCESS) |
| **Architecture** | ✅ MVVM + Jetpack Compose |
| **Player vidéo** | ✅ ExoPlayer/Media3 complet |
| **Monétisation** | ⚠️ Fonctionnelle mais IDs de test |
| **Sécurité** | ✅ Signature vérifiée, keystore externalisé |
| **Publication Play Store** | 🔴 4 blocages restants |
| **Tests** | ❌ Aucun test unitaire |

---

## 🏗️ CE QUI EST FAIT (100% Terminé)

### Fonctionnalités complètes

- [x] **Lecteur vidéo** : HLS, DASH, MP4, WebM, RTSP, SmoothStreaming
- [x] **Contrôles player** : play/pause, seek ±10s, barre progression + buffer
- [x] **Sélection qualité** : Auto + choix manuel (720p, 1080p, etc.)
- [x] **Picture-in-Picture** : Fonctionnel Android O+
- [x] **Resize vidéo** : Fit / Fill toggle
- [x] **Gestion vidéos** : Ajout, suppression, recherche temps réel, persistance locale
- [x] **Formulaire ajout** : Validation temps réel titre + URL, feedback visuel
- [x] **Splash screen** : Android 12+ SplashScreen API (500ms)
- [x] **Navigation drawer** : 3 sections, style YouTube/NewPipe
- [x] **Politique confidentialité** : Complète (RGPD + CCPA)
- [x] **Conditions utilisation** : Complètes (anti-piratage, monétisation)
- [x] **AdMob interstitiel** : Chargement + affichage + timeout 6s
- [x] **AdMob bannière fallback** : Si interstitiel échoue
- [x] **Détection adblock** : 3 échecs → dialogue + blocage
- [x] **Anti-spam** : Debounce 1s sur boutons navigation
- [x] **Deep links** : `stv://play?url=...`
- [x] **Intents SoukiTV** : Action `com.example.stv.action.PLAY_STREAM`
- [x] **Intents vidéo génériques** : MIME types HLS + vidéo
- [x] **Sécurité appelants** : Vérification signature PKI
- [x] **Mode immersif player** : Barres système cachées en lecture
- [x] **Thème** : Dark theme YouTube-style, couleurs centralisées
- [x] **Typographie** : 10 styles personnalisés Material 3
- [x] **ID unique vidéos** : UUID (résout bug URLs identiques)
- [x] **Buffer optimisé** : Démarrage 1.5s, max 50s
- [x] **Audio focus** : Pause sur déconnexion écouteurs
- [x] **Build release** : APK signée, R8, shrink resources
- [x] **Multi-module** : `:app` (STV) + `:soukitv` (catalogue)

### Corrections appliquées (Février-Mars 2026)

- [x] Network API modernisée (`NetworkCapabilities`)
- [x] Icons `AutoMirrored` pour support RTL
- [x] `TopAppBarDefaults.topAppBarColors` (remplacement API dépréciée)
- [x] Imports inutilisés supprimés
- [x] `@OptIn(UnstableApi::class)` correctement appliqué
- [x] `BuildConfig` importé pour IDs AdMob flavor-specific
- [x] Bug URLs identiques corrigé (UUID)
- [x] Java 17 configuré
- [x] AGP 9.0.1 + Kotlin 2.3.10 à jour
- [x] 18 erreurs de compilation → 0

### Corrections appliquées (7 mars 2026 — session actuelle)

- [x] IDs AdMob prod lus depuis `local.properties` (fallback IDs test)
- [x] AdMob App ID dans Manifest → placeholder `${admobAppId}` dynamique
- [x] ID bannière en dur dans `FallbackBanner` → `BuildConfig.ADMOB_BANNER_ID`
- [x] 7 textes français en dur dans `PlayerActivity.kt` → `stringResource()`
- [x] `centerAlignedTopAppBarColors` déprécié → `topAppBarColors` dans `AddVideoActivity.kt`
- [x] `removeVideo()` filtre par `it.id` au lieu de `title+url` dans `VideoListViewModel.kt`
- [x] `PrivacyPolicyActivity.kt` et `TermsOfServiceActivity.kt` supprimées → liens web externes
- [x] `TermsOfServiceActivity` `exported="true"` supprimé (surface d'attaque éliminée)
- [x] 3 nouvelles strings ajoutées dans `strings.xml` (fallback_blocker_active, fallback_preparing_stream, fallback_launching_stream)
- [x] Persistance JSON manuelle → migration vers **Kotlin Serialization** (`kotlinx.serialization`)
- [x] `VideoItem` annoté `@Serializable`, `loadVideos()`/`saveVideos()` utilisent `Json.decodeFromString`/`Json.encodeToString`
- [x] Plugin `kotlin-serialization` ajouté + dépendance `kotlinx-serialization-json:1.10.0`
- [x] Commentaire mort "Method removed temporarily" supprimé dans `PlayerViewModel.kt`
- [x] Commentaire mort `// val selector = trackSelector ?: return` supprimé
- [x] Imports inutilisés supprimés (`android.net.Uri`, `DefaultAllocator`)
- [x] **Badge LIVE** : détection automatique flux live via `isCurrentMediaItemLive` + badge rouge "● LIVE" en haut à droite du player
- [x] 2 nouvelles strings ajoutées (`live_badge`, `live_badge_cd`)
- [x] Fichier `values-fr/strings.xml` créé — **49 clés traduites** en français, 7 clés héritées intentionnellement (URLs, noms techniques, LIVE)

---

## 🔴 CE QUI RESTE À FAIRE (Avant publication Play Store)

### Blocages critiques

| # | Tâche | Fichier(s) | Priorité |
|---|-------|-----------|----------|
| 1 | ~~Remplacer IDs AdMob test → production~~ | `app/build.gradle.kts` | ✅ **CORRIGÉ** — Flavor prod lit depuis `local.properties`. Renseigner IDs quand disponibles. |
| 2 | ~~Remplacer AdMob App ID test dans Manifest~~ | `AndroidManifest.xml` | ✅ **CORRIGÉ** — Placeholder `${admobAppId}` dynamique. |
| 3 | **Changer le package name** (`com.example.stv` → nom réel) | `build.gradle.kts`, tout le code | 🔴 CRITIQUE — À faire |
| 4 | ~~Mettre un email de contact réel~~ | `TermsOfServiceActivity.kt` | ✅ **CORRIGÉ** — Activities supprimées, liens web externes. |

### Améliorations prioritaires

| # | Tâche | Fichier(s) | Priorité |
|---|-------|-----------|----------|
| 5 | ~~Extraire textes français en dur → strings.xml~~ | `PlayerActivity.kt` | ✅ **CORRIGÉ** |
| 6 | ~~Créer `values-fr/strings.xml` pour multi-langues~~ | `res/values-fr/` | ✅ **CORRIGÉ** — 49 clés traduites, 7 héritées. |
| 7 | ~~Corriger `removeVideo()` pour utiliser l'ID~~ | `VideoListViewModel.kt` | ✅ **CORRIGÉ** |
| 8 | ~~Utiliser `BuildConfig.ADMOB_BANNER_ID` dans `FallbackBanner`~~ | `PlayerActivity.kt` | ✅ **CORRIGÉ** |
| 9 | ~~Corriger `centerAlignedTopAppBarColors` déprécié~~ | `AddVideoActivity.kt` | ✅ **CORRIGÉ** |
| 10 | ~~Passer `TermsOfServiceActivity` en `exported="false"`~~ | `AndroidManifest.xml` | ✅ **CORRIGÉ** — Activity supprimée entièrement. |

### Fonctionnalités v1.1 (Non bloquant)

| # | Tâche | Description |
|---|-------|-------------|
| 11 | Implémenter Historique | Section drawer "History" |
| 12 | Implémenter Favoris | Section drawer "Favorites" |
| 13 | Implémenter Paramètres | Section drawer "Settings" (langue, thème, etc.) |
| 14 | Implémenter Cast (Chromecast) | Bouton Cast dans PlayerControls |
| 15 | Ajouter tests unitaires | ViewModels, PlayerController, AdManager |
| 16 | Utiliser `isNetworkAvailable()` | Vérifier connexion avant streaming |
| 17 | Utiliser `_adManager` dans MainScreen | Afficher pubs sur écran principal |
| 18 | Migration SharedPreferences → Room | Persistance plus robuste |
| 19 | Confirmation avant suppression vidéo | UX améliorée |
| 20 | Supprimer commentaire "Method removed temporarily" | `PlayerViewModel.kt` L106 |

---

## 📁 FICHIERS CLÉS À CONNAÎTRE

### Code source principal

| Fichier | Lignes | Rôle |
|---------|--------|------|
| `app/src/main/java/com/example/stv/MainActivity.kt` | 422 | Écran d'accueil + drawer |
| `app/src/main/java/com/example/stv/PlayerActivity.kt` | 812 | Lecteur vidéo complet |
| `app/src/main/java/com/example/stv/PlayerViewModel.kt` | 309 | ViewModel ExoPlayer |
| `app/src/main/java/com/example/stv/VideoListActivity.kt` | 322 | Liste vidéos |
| `app/src/main/java/com/example/stv/VideoListViewModel.kt` | 94 | CRUD vidéos + persistance |
| `app/src/main/java/com/example/stv/AddVideoActivity.kt` | 350 | Formulaire ajout vidéo |
| `app/src/main/java/com/example/stv/AdManager.kt` | 180 | Gestion AdMob |
| `app/src/main/java/com/example/stv/ads/AdsController.kt` | 78 | Orchestration pubs |
| `app/src/main/java/com/example/stv/security/PermissionHelper.kt` | 172 | Sécurité signatures |
| `app/src/main/java/com/example/stv/player/PlayerController.kt` | 67 | Validation URLs |
| `app/src/main/java/com/example/stv/ui/PlayerUiState.kt` | 35 | State machine player |
| `app/src/main/java/com/example/stv/ui/theme/Color.kt` | 40 | Palette couleurs |
| `app/src/main/java/com/example/stv/ui/theme/Theme.kt` | 91 | Thème Material 3 |

### Configuration

| Fichier | Rôle |
|---------|------|
| `app/build.gradle.kts` | Config app (flavors, signing, dépendances) |
| `build.gradle.kts` | Config racine (plugins) |
| `settings.gradle.kts` | Modules (:app, :soukitv) |
| `gradle/libs.versions.toml` | Catalogue versions dépendances |
| `gradle.properties` | Options Gradle/Android |
| `app/src/main/AndroidManifest.xml` | Activities, permissions, intents |
| `keystore.properties` | Config signature (NON versionné) |

---

## 🏛️ ARCHITECTURE RÉSUMÉE

```
MVVM + Jetpack Compose + ExoPlayer/Media3
│
├── UI Layer (Compose)
│   ├── MainActivity → MainScreen (drawer, hero, FAB)
│   ├── PlayerActivity → VideoPlayer + PlayerControls
│   ├── VideoListActivity → VideoListScreen + SearchBar
│   ├── AddVideoActivity → AddVideoScreen + ValidationIndicator
│   └── Privacy/Terms Activities
│
├── ViewModel Layer
│   ├── MainViewModel (URL par défaut)
│   ├── PlayerViewModel (ExoPlayer, tracks, seek, buffer)
│   └── VideoListViewModel (CRUD vidéos, SharedPreferences)
│
├── Service Layer
│   ├── AdManager (AdMob interstitiel + bannière)
│   ├── AdsController (coroutines + timeout)
│   ├── PlayerController (validation URL)
│   └── PermissionHelper (sécurité signatures)
│
└── Theme Layer
    ├── Color.kt (palette YouTube-style)
    ├── Theme.kt (dark/light schemes)
    └── Type.kt (typographie)
```

---

## 🔧 STACK TECHNIQUE

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Kotlin | 2.3.10 |
| Build | AGP (Gradle KTS) | 9.0.1 |
| UI | Jetpack Compose | BOM 2026.02.01 |
| Design | Material 3 | 1.4.0 |
| Vidéo | Media3 ExoPlayer | 1.9.2 |
| Ads | Google Play Services Ads | 25.0.0 |
| Navigation | Navigation Compose | 2.9.7 |
| Splash | Core Splashscreen | 1.2.0 |
| Lifecycle | ViewModel Compose | 2.10.0 |
| JVM | Java 17 | - |
| SDK | Android 24-36 | - |

---

## 🔗 INTÉGRATION SOUKITV

```kotlin
// SoukiTV lance STV :
val intent = Intent("com.example.stv.action.PLAY_STREAM").apply {
    setPackage("com.example.stv")
    putExtra("VIDEO_URL", streamUrl)
    putExtra("SKIP_ADS", false) // true pour partenaires premium
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
startActivity(intent)
```

```
// STV reçoit et vérifie :
1. PermissionHelper.isCallerAuthorizedForPlayer() → vérifie signature
2. resolveVideoUrl() → extrait l'URL
3. PlayerUiState machine → LoadingAds → ShowingAd → Ready
4. ExoPlayer.setMediaItem() → lecture
```

---

## 💰 MONÉTISATION (AdMob)

### État actuel
- **Interstitiel** : Affiché avant chaque vidéo (max 6s d'attente)
- **Bannière fallback** : Si interstitiel échoue (5s compte à rebours)
- **Anti-adblock** : 3 échecs → blocage strict + dialogue + fermeture app
- **IDs** : ⚠️ IDs de test uniquement — 0€ de revenus

### Flavors AdMob
```
dev  → IDs test Google (développement)
prod → IDs test Google ⚠️ (À REMPLACER par IDs production)
```

---

## 📊 HISTORIQUE DES SESSIONS

| Date | Actions principales |
|------|-------------------|
| Janv-Fév 2026 | Développement initial STV Player v1.0 |
| 27 Fév 2026 | Corrections finales, build release APK, 135 docs MD |
| 1er Mars 2026 | Fix 18 erreurs compilation, modernisation APIs, bug URLs identiques |
| 7 Mars 2026 | Analyse complète du code, correction 13/15 problèmes identifiés (AdMob, i18n, sécurité, APIs dépréciées, suppression Activities Privacy/Terms → liens web, Kotlin Serialization, badge LIVE, traduction française) |

---

## ⚡ COMMANDES UTILES

```powershell
# Build debug (dev flavor)
./gradlew assembleDevDebug

# Build release (prod flavor)
./gradlew assembleProdRelease

# APK release générée
app/build/outputs/apk/prod/release/app-prod-release.apk

# Installer sur device
adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk

# Clean + rebuild
./gradlew clean assembleProdRelease
```

---

## 🎯 PROCHAINE SESSION — PAR OÙ COMMENCER

### Option A : Préparer la publication Play Store
1. Obtenir un compte Google AdMob → créer les IDs production
2. Remplacer les 3 occurrences d'IDs dans `app/build.gradle.kts`
3. Remplacer l'App ID dans `AndroidManifest.xml`
4. Changer le package name (`com.example.stv` → nom définitif)
5. Mettre un email de contact réel dans les CGU
6. Tester le build prod release
7. Soumettre sur Play Console

### Option B : Améliorations fonctionnelles
1. Implémenter Historique, Favoris, Paramètres
2. Ajouter le Cast (Chromecast)
3. Créer `values-fr/strings.xml` complet
4. Nettoyer les textes en dur
5. Ajouter des tests unitaires

### Option C : Corrections techniques
1. Extraire textes français en dur → strings.xml
2. Corriger `removeVideo()` pour utiliser l'ID
3. Utiliser `BuildConfig` pour l'ID bannière dans `FallbackBanner`
4. Corriger `centerAlignedTopAppBarColors` dans AddVideoActivity
5. Passer `TermsOfServiceActivity` en `exported="false"`

---

*Document de référence — Mis à jour le 7 mars 2026 — GitHub Copilot*

