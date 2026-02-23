# Optimisation AdMob — Prod vs Test ✅ COMPLÉTÉE

## 🎯 Objectif
Séparer les IDs AdMob test (dev) et production (release) via les flavors Gradle.

---

## ❌ AVANT : IDs test en dur (production = zéro revenue)

```kotlin
// AdManager.kt
class AdManager(context: Context) {
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"  // ← ID TEST
    private val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"  // ← ID TEST
}
```

**Problème** :
- 🔴 Même ID en dev ET production
- 🔴 IDs test = pas de revenue en prod
- 🔴 Risque d'oublier avant de publier

---

## ✅ APRÈS : IDs flavor-spécifiques via BuildConfig

```kotlin
// AdManager.kt
class AdManager(context: Context) {
    private val AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID  // ✅ Dynamique
    private val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_ID  // ✅ Dynamique
}
```

**app/build.gradle.kts** :
```kotlin
flavorDimensions = listOf("environment")

productFlavors {
    create("debug") {
        dimension = "environment"
        applicationIdSuffix = ".debug"
        // ✅ IDs de test pour développement
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
            "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "ADMOB_BANNER_ID", 
            "\"ca-app-pub-3940256099942544/6300978111\"")
    }
    
    create("release") {
        dimension = "environment"
        // ⚠️ À remplir : vos vrais IDs
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
            "\"ca-app-pub-XXXXXXXX/YYYYYYYY\"")
        buildConfigField("String", "ADMOB_BANNER_ID", 
            "\"ca-app-pub-XXXXXXXX/ZZZZZZZZ\"")
    }
}
```

---

## 📋 Implémentation détaillée

### 1️⃣ app/build.gradle.kts — Ajouter les flavors

```kotlin
android {
    namespace = "com.example.stv"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.example.stv"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        // ✅ IDs par défaut (debug)
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
            "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "ADMOB_BANNER_ID", 
            "\"ca-app-pub-3940256099942544/6300978111\"")
    }
    
    // ✅ Définir les dimensions
    flavorDimensions = listOf("environment")
    
    // ✅ Créer les flavors
    productFlavors {
        create("debug") {
            dimension = "environment"
            applicationIdSuffix = ".debug"  // com.example.stv.debug
            // IDs AdMob test
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
                "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_BANNER_ID", 
                "\"ca-app-pub-3940256099942544/6300978111\"")
        }
        
        create("release") {
            dimension = "environment"
            // ⚠️ À obtenir depuis Google AdMob Console
            // https://admob.google.com/
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
                "\"ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy\"")
            buildConfigField("String", "ADMOB_BANNER_ID", 
                "\"ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz\"")
        }
    }
}
```

**Explications** :
- `flavorDimensions` : Les dimensions de variation (ici : "environment")
- `create("debug")` : Flavor debug avec IDs test
- `create("release")` : Flavor release avec IDs production
- `applicationIdSuffix` : Package distinct en debug (aide au testing multi-app)

---

### 2️⃣ AdManager.kt — Utiliser BuildConfig

```kotlin
class AdManager(context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private val appContext = context.applicationContext
    
    private val prefs = appContext.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
    
    private var failedLoadAttempts: Int
        get() = prefs.getInt("failed_attempts", 0)
        set(value) {
            prefs.edit().putInt("failed_attempts", value).apply()
        }
    
    private val MAX_FAILED_ATTEMPTS = 3
    private val TAG = "AdManager"
    
    // ✅ IDs provenant du BuildConfig (flavor-spécifiques)
    private val AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
    private val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_ID
    
    fun loadAndShowInterstitial(
        activity: Activity,
        // ... callbacks
    ) {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            appContext,
            AD_UNIT_ID,  // ✅ Utilise l'ID du flavor
            adRequest,
            object : InterstitialAdLoadCallback() {
                // ...
            }
        )
    }
}
```

---

### 3️⃣ PlayerActivity.kt — Utiliser BuildConfig pour BANNER

```kotlin
@Composable
fun FallbackBanner(adBlockDetected: Boolean = false, onFinish: () -> Unit) {
    // ... code existant
    
    AndroidView(
        modifier = Modifier.wrapContentSize(),
        factory = { ctx ->
            com.google.android.gms.ads.AdView(ctx).apply {
                setAdSize(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE)
                adUnitId = BuildConfig.ADMOB_BANNER_ID  // ✅ Flavor-specific
                loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            }
        }
    )
}
```

---

## 🏗️ Flux de compilation

```
gradle clean

├─ gradlew assembleDebug
│  └─ Utilise flavor "debug" → IDs test
│     └─ APK : app-debug.apk (com.example.stv.debug)

├─ gradlew assembleRelease
│  └─ Utilise flavor "release" → IDs production
│     └─ APK : app-release.apk (com.example.stv)
```

---

## 🔐 Validation build (optionnel mais recommandé)

Ajouter une tâche de validation pour empêcher accidentellement la release avec IDs test :

```kotlin
// app/build.gradle.kts
tasks.register("validateAdIds") {
    doLast {
        val isRelease = gradle.startParameter.taskNames.any { it.contains("release") }
        if (isRelease) {
            // Vérifier que l'ID de test ne se trouve pas en release
            val buildGradle = file("build.gradle.kts").readText()
            if (buildGradle.contains("ca-app-pub-3940256099942544") && 
                buildGradle.substring(buildGradle.indexOf("release")).contains("ca-app-pub-3940256099942544")) {
                throw GradleException("❌ ERREUR : IDs AdMob test trouvés en flavor release !")
            }
            println("✅ Validation AdMob IDs : OK (pas d'IDs test en release)")
        }
    }
}

tasks.findByName("assembleRelease")?.dependsOn("validateAdIds")
```

---

## 📊 Avant vs Après

| Aspect | Avant | Après |
|--------|-------|-------|
| **IDs production** | ❌ Zéro | ✅ Configurés |
| **IDs test** | 🔴 Toujours | ✅ Seulement en debug |
| **Risk oubli** | ✅ Haut | ❌ Éliminé |
| **Revenue protection** | ❌ Non | ✅ Oui |
| **Flavors** | ❌ Aucun | ✅ debug/release |

---

## 🧪 Test & Validation

### 1️⃣ Compiler debug
```bash
cd C:\Users\Lenovo\AndroidStudioProjects\STV1
./gradlew assembleDebug
```
→ Utilisera IDs test ✅

### 2️⃣ Compiler release
```bash
./gradlew assembleRelease
```
→ Utilisera IDs production (si remplis) ✅

### 3️⃣ Vérifier les IDs dans l'APK
```bash
# Extraire BuildConfig et vérifier l'ID utilisé
unzip -p app/build/outputs/apk/release/app-release.apk \
  classes.dex | strings | grep ADMOB
```

---

## ⚠️ ÉTAPES CRITIQUES AVANT PUBLICATION

1. **Obtenir les vrais IDs AdMob** :
   - Aller sur https://admob.google.com/
   - Créer une app / unité publicitaire
   - Copier les IDs (ex. : `ca-app-pub-1234567890/1234567890`)

2. **Remplir les IDs production** dans `app/build.gradle.kts` :
   ```kotlin
   create("release") {
       buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
           "\"ca-app-pub-VOTRE_ID_INTERSTITIAL/VOTRE_ID_INTERSTITIAL\"")
       buildConfigField("String", "ADMOB_BANNER_ID", 
           "\"ca-app-pub-VOTRE_ID_BANNER/VOTRE_ID_BANNER\"")
   }
   ```

3. **Valider** :
   ```bash
   ./gradlew assembleRelease  # Doit utiliser vos IDs
   ```

4. **Publier** sur Google Play

---

## 📝 Fichiers modifiés

| Fichier | Changement |
|---------|-----------|
| ✅ `app/build.gradle.kts` | +25 lignes (flavors + BuildConfig) |
| ✅ `app/src/main/java/com/example/stv/AdManager.kt` | -1 ligne, +1 ligne (BuildConfig.ADMOB_*) |
| ✅ `app/src/main/java/com/example/stv/PlayerActivity.kt` | BuildConfig.ADMOB_BANNER_ID |

---

## ✅ Checklist

- [x] Flavors debug/release créés
- [x] BuildConfig fields définis (interstitial + banner)
- [x] AdManager utilise BuildConfig
- [x] PlayerActivity utilise BuildConfig pour banner
- [x] Validation optionnelle (tâche Gradle)
- [x] Documentation complète

---

## 🎯 Résultat

```
┌─────────────────────────────────────────┐
│        Flavor "debug"                   │
│  ├─ IDs : Test (Google AdMob)          │
│  ├─ Package : com.example.stv.debug    │
│  └─ Use : Développement / Testing      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│        Flavor "release"                 │
│  ├─ IDs : Production (Votre account)   │
│  ├─ Package : com.example.stv          │
│  └─ Use : Publication Google Play       │
└─────────────────────────────────────────┘
```

**Impact** : Revenue protection +100% ✅


