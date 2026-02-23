# ✅ CORRECTIONS APPLIQUÉES

## 1️⃣ SoukiTV : Message "STV Required" malgré installation

### ❌ Problème
- Le dialog "STV Required" s'affichait même si STV était installé
- Raison 1 : Pas de permission `QUERY_ALL_PACKAGES`
- Raison 2 : Package name différent entre flavors (dev vs prod)

### ✅ Solutions appliquées

#### A) Ajouter permission QUERY_ALL_PACKAGES
Fichier : `soukitv/src/main/AndroidManifest.xml`
```xml
<!-- Permission requise pour vérifier si STV Player est installé -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

#### B) Supporter les deux flavors (dev et prod)
Fichier : `soukitv/src/main/java/com/example/soukitv/ui/home/HomeScreen.kt`

**Avant** :
```kotlin
val packageName = "com.example.stv"  // ❌ Ne trouve que la version prod
val isInstalled = try {
    context.packageManager.getPackageInfo(packageName, 0)
    true
} catch (e: Exception) {
    false
}
```

**Après** :
```kotlin
// ✅ Supporter à la fois dev et prod flavors
val stvPackageNames = listOf(
    "com.example.stv",      // Production
    "com.example.stv.dev",  // Debug/Dev flavor
    "com.example.stv.prod"  // Prod flavor
)

val isInstalled = stvPackageNames.any { packageName ->
    try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

// Trouver le package réellement installé
val actualPackageName = stvPackageNames.firstOrNull { packageName ->
    try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
} ?: "com.example.stv"
```

### 🎯 Résultat
✅ SoukiTV détectera maintenant correctement STV Player, peu importe le flavor installé.

---

## 2️⃣ AdMob IDs : Utiliser test IDs pour debug ET release

### ❌ Problème
- Configuration différente entre debug (test) et release (production)
- Vous voulez les IDs test pour testing en release sur device

### ✅ Solution appliquée

Fichier : `app/build.gradle.kts`

**Avant** :
```kotlin
create("prod") {
    dimension = "environment"
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy\"")  // ❌ Vrai ID (pas dispo)
}
```

**Après** :
```kotlin
create("prod") {
    dimension = "environment"
    // ✅ Utiliser les IDs test pour le moment (release test sur device)
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
    buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
}
```

### 🎯 Résultat
✅ Les deux flavors (dev et prod) utilisent maintenant les IDs test.

---

## 📋 Fichiers modifiés

| Fichier | Modifications |
|---------|---------------|
| `soukitv/src/main/AndroidManifest.xml` | +1 permission (QUERY_ALL_PACKAGES) |
| `soukitv/src/main/java/com/example/soukitv/ui/home/HomeScreen.kt` | +20 lignes (support multi-flavors) |
| `app/build.gradle.kts` | Flavor prod : test IDs au lieu de prod IDs |

---

## 🚀 Prochaines étapes

1. **Recompiler** les deux apps :
   ```bash
   ./gradlew clean assembleDevDebug
   ./gradlew clean assembleProdRelease
   ```

2. **Installer sur device** :
   ```bash
   adb install app/build/outputs/apk/dev/debug/app-dev-debug.apk
   adb install app/build/outputs/apk/prod/release/app-prod-release.apk
   adb install soukitv/build/outputs/apk/release/soukitv-release.apk
   ```

3. **Tester** :
   - Ouvrir SoukiTV
   - Cliquer sur une chaîne → Doit lancer STV Player sans dialog d'installation
   - Les publicités doivent s'afficher (test IDs)

---

## ⚠️ Notes

- **Avant publication Google Play** : Remplacer les IDs test par vos vrais IDs production dans le flavor `prod`
- **Format flavor prod release** : `com.example.stv` (sans suffixe)
- **Format flavor dev debug** : `com.example.stv.dev` (avec suffixe)


