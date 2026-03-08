# 🎴 QUICK REFERENCE CARD - STV Player

**Format** : Une page (À imprimer)  
**Utilité** : Référence rapide pendant développement  
**Date** : 26/02/2026

---

## 📱 ACTIVITÉS PRINCIPALES

```
┌────────────────────────────────┐
│    MainActivity                │
│  (Écran d'accueil)             │
│  ├─ Sélect. vidéos             │
│  └─ URL personnalisée          │
│    startActivity(PlayerActivity)
│                 │
│                 ▼
│  ┌────────────────────────────┐
│  │  PlayerActivity            │
│  │  (Lecteur)                 │
│  │  ├─ ExoPlayer              │
│  │  ├─ AdMob                  │
│  │  └─ Contrôles              │
│  └────────────────────────────┘
└────────────────────────────────┘
```

---

## 🗂️ STRUCTURE FICHIERS

```
MainActivity.kt                   ExoPlayer + UI
  ├─ MainScreen()
  ├─ MainViewModel.kt            PlayerActivity.kt
  │  ├─ streamUrl                  ├─ PlayerScreen()
  │  └─ validateUrl()              ├─ PlayerViewModel.kt
  └─ Menu drawer                   │  ├─ exoPlayer
                                   │  ├─ isPlaying
                                   │  ├─ position
     AdManager.kt                  │  └─ duration
  ├─ loadInterstitial()            │
  ├─ loadAndShow()                 AdManager
  └─ showStrictBlockerDialog()
                                   AdsController
     PlayerController.kt              ├─ showAdIfNeeded()
  └─ validateStreamUrl()              └─ 6s timeout

     PermissionHelper.kt
  └─ isCallerAuthorized()
```

---

## ⚡ COMMANDES BUILD RAPIDES

```powershell
# Clean
.\gradlew.bat clean

# Dev Debug
.\gradlew.bat assembleDevDebug
# → app\build\outputs\apk\dev\debug\*.apk

# Prod Debug
.\gradlew.bat assembleProdDebug
# → app\build\outputs\apk\prod\debug\*.apk

# Prod Release
.\gradlew.bat assembleProdRelease
# → app\build\outputs\apk\prod\release\*.apk

# Install & Run
.\gradlew.bat installDevDebug runDevDebug

# Tests
.\gradlew.bat testDevDebug

# Logs
adb logcat PlayerActivity
adb logcat -c  # clear
```

---

## 🔗 INTENT FILTERS

| N° | Type | Filter | Exemple |
|----|------|--------|---------|
| 1 | Custom | `com.example.stv.action.PLAY_STREAM` | SoukiTV |
| 2 | Deep Link | `stv://play?url=...` | SMS, Web |
| 3 | HTTP + MIME | `video/*` + `http/https` | Navigateur |
| 4 | Streaming | `.m3u8`, `.mpd` | DASH, HLS |

---

## 📊 STATE FLOWS

```
MainViewModel
├─ streamUrl: String
└─ isError: Boolean

PlayerViewModel
├─ exoPlayer: ExoPlayer?
├─ isPlaying: Boolean
├─ currentPosition: Long
├─ duration: Long
├─ bufferedPosition: Long
├─ isLoading: Boolean
└─ errorMessage: String?

Pattern:
_state = MutableStateFlow(initial)
state: StateFlow = _state.asStateFlow()

Composable:
val value by viewModel.state.collectAsState()
```

---

## 🎬 FORMATS VIDÉO SUPPORTÉS

| Format | Extension | Status |
|--------|-----------|--------|
| HLS | .m3u8 | ✅ |
| DASH | .mpd | ✅ |
| MP4 | .mp4 | ✅ |
| MKV | .mkv | ✅ |
| Smooth Stream | .ism | ✅ |
| RTSP | rtsp:// | ✅ |

---

## 💰 ADMOB CONFIGURATION

```kotlin
// app/build.gradle.kts

productFlavors {
    create("dev") {
        buildConfigField("String", 
            "ADMOB_INTERSTITIAL_ID",
            "\"ca-app-pub-3940256099942544/1033173712\"")
    }
    
    create("prod") {
        buildConfigField("String",
            "ADMOB_INTERSTITIAL_ID",
            "\"YOUR_PRODUCTION_ID\"")
    }
}

// Usage
val adId = BuildConfig.ADMOB_INTERSTITIAL_ID
```

---

## 🔐 PERMISSIONS & SECURITY

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Network Security -->
android:networkSecurityConfig="@xml/network_security_config"
android:usesCleartextTraffic="true"
android:hardwareAccelerated="true"

<!-- Exported Activities -->
<activity android:exported="true">
    <!-- Intent filters requis -->
</activity>
```

---

## 🎨 THEME & COLORS

```kotlin
// app/src/main/java/com/example/stv/ui/theme/

// Material 3 Colors
val primaryColor = Color(0xFF...)
val secondaryColor = Color(0xFF...)
val tertiaryColor = Color(0xFF...)

// Dark Mode
@Composable
fun STVTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() 
                     else lightColorScheme()
    )
}
```

---

## 🏗️ ARCHITECTURE LAYERS

```
PRESENTATION LAYER
├─ MainActivity (Compose)
├─ PlayerActivity (Compose)
├─ UI Components
└─ Theme

BUSINESS LOGIC LAYER
├─ MainViewModel (State)
├─ PlayerViewModel (State)
├─ PlayerController (Validation)
├─ AdManager (Ads)
├─ AdsController (Ads Logic)
└─ PermissionHelper (Security)

EXTERNAL LAYER
├─ Media3 ExoPlayer
├─ Google AdMob
├─ Jetpack Compose
└─ Kotlin Coroutines
```

---

## 🚀 DEVELOPER FLOWS

```
Ajouter Feature:
1. Create ViewModel (if needed)
2. Define StateFlow
3. Create Composable UI
4. Bind data (collectAsState)
5. Test
6. Build & Deploy

Déboguer:
1. adb logcat
2. Vérifier logs Cat
3. Breakpoint (Android Studio)
4. Vérifier state dans Device Explorer
5. Lire code du ViewModel

Configurer AdMob:
1. Obtenir IDs Google
2. Mettre à jour build.gradle
3. Rebuild prod flavor
4. Tester avec APK real
```

---

## 📋 CHECKLIST RELEASE

- [ ] AdMob IDs configured
- [ ] App signing keystore exists
- [ ] ProGuard rules OK
- [ ] Minification tested
- [ ] Tests passent
- [ ] Version code/name updated
- [ ] AndroidManifest reviewed
- [ ] Permissions minimal
- [ ] No debug logging
- [ ] Screenshots pour PlayStore
- [ ] Description complète
- [ ] Privacy policy link OK
- [ ] APK size < 100MB

---

## 🐛 TROUBLESHOOTING RAPIDE

| Erreur | Solution |
|--------|----------|
| APK ne compile | `clean && build` |
| Gradle timeout | Augmenter gradle.properties |
| App crash | Voir adb logcat |
| Vidéo ne joue pas | Vérifier URL + internet |
| AdMob ne charge pas | Vérifier IDs dans BuildConfig |
| Signing error | Vérifier release.keystore |
| Import failed | File → Sync Now |

---

## 🎯 KEY CONCEPTS

```
MVVM Pattern
View ←→ ViewModel ←→ State
(Compose)  (StateFlow)

Intent Filters
Intent créé
  ↓
Android cherche app
  ↓
Si match → Lance activité
  ↓
Intent data reçu

ExoPlayer
Load URL
  ↓
Buffer media
  ↓
Play on user action
  ↓
Observe state changes

AdMob
Request ad
  ↓
Timeout 6s
  ↓
Show if loaded
  ↓
Fallback if failed
```

---

## 📞 RESSOURCES

**Docs Officielles** :
- androidx.media3.exoplayer (ExoPlayer)
- androidx.compose.material3 (Material Design 3)
- Google AdMob docs
- Kotlin Coroutines docs

**Fichiers Essentiels** :
- `AndroidManifest.xml` - Déclarations
- `build.gradle.kts` - Config
- `MainActivity.kt` - Écran accueil
- `PlayerActivity.kt` - Lecteur

**Docs Internes** :
1. RESUME_ANALYSE_STV_FINAL.md
2. ANALYSE_ARCHITECTURE_STV.md
3. ARCHITECTURE_VISUELLE_DIAGRAMS.md
4. GUIDE_PRATIQUE_UTILISATION_STV.md
5. INDEX_DOCUMENTATION_STV.md ← Vous êtes ici

---

## ✨ TIPS & TRICKS

```kotlin
// Déboguer StateFlow
LaunchedEffect(key1 = state) {
    Log.d("TAG", "State changed: $state")
}

// Validation URL rapide
fun isValidUrl(url: String) = 
    url.startsWith("http") && url.length < 2048

// ExoPlayer quick init
val player = ExoPlayer.Builder(context)
    .setLoadControl(defaultLoadControl)
    .build()

// Catch ads errors
try {
    loadAd()
} catch (e: Exception) {
    Log.e("AdError", e.message ?: "Unknown")
    showFallback()
}
```

---

## 🎓 LEARNING PATH

```
Day 1: Basics
├─ Read RESUME
├─ Understand MainActivity
└─ Understand PlayerActivity

Day 2: Components
├─ Study ViewModels
├─ Study AdManager
└─ Study Intent Filters

Day 3: Advanced
├─ Study MVVM pattern
├─ Study ExoPlayer integration
├─ Study Ad flow

Day 4+: Modifications
├─ Make your changes
├─ Test thoroughly
├─ Deploy
```

---

**QUICK REFERENCE CARD COMPLETE ✅**

*Print this page and keep on desk during development*

*Last updated: 26/02/2026*

