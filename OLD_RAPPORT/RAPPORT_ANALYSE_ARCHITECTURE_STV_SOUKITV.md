# 📊 RAPPORT D'ANALYSE DÉTAILLÉE - Architecture STV & SoukiTV

**Date:** 26 Février 2026  
**Projet:** STV4 (STV Player + SoukiTV)  
**Type:** Analyse Architecture & Problèmes Critiques  
**Auteur:** GitHub Copilot

---

## 🎯 RÉSUMÉ EXÉCUTIF

### Situation Actuelle
Le projet contient **deux applications distinctes** :
1. **STV** (com.example.stv) - Player vidéo avec publicités AdMob
2. **SoukiTV** (com.example.soukitv) - Catalogue de chaînes IPTV

### Objectif Souhaité
- **STV** doit être un **lecteur universel** acceptant les URLs externes depuis n'importe quelle application catalogue (SoukiTV, futures apps)
- **SoukiTV** doit **forcer l'utilisation de STV** si installé, sinon rediriger vers Google Play Store
- **STV** sera publié sur le Play Store comme application standalone

### ⚠️ PROBLÈMES CRITIQUES IDENTIFIÉS

| Problème | Sévérité | Impact |
|----------|----------|--------|
| **Protection signature-level bloque SoukiTV** | 🔴 CRITIQUE | SoukiTV ne peut pas lancer STV |
| **Pas de Deep Link / Intent Filter** | 🔴 CRITIQUE | Impossible d'ouvrir STV depuis d'autres apps |
| **Pas de fallback Play Store** | 🟠 MAJEUR | Mauvaise UX si STV non installé |
| **Multiples flavors (dev/prod) compliquent la détection** | 🟡 MODÉRÉ | Code complexe dans SoukiTV |
| **PlayerActivity non conçue pour URLs externes** | 🟡 MODÉRÉ | Pas de validation côté UI pour appelants externes |

---

## 🏗️ ARCHITECTURE ACTUELLE

### 1️⃣ Application STV (Player)

#### **Package & Configuration**
```
Package Name: com.example.stv
Flavors: dev (com.example.stv.dev), prod (com.example.stv.prod)
Version: 1.0 (versionCode 1)
MinSDK: 24, TargetSDK: 35
Signing: release.keystore (password: android, alias: key0)
```

#### **Structure des Fichiers**
```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/stv/
│   ├── MainActivity.kt              # Écran principal (interface de test avec champ URL)
│   ├── PlayerActivity.kt            # Lecteur vidéo (Media3 ExoPlayer + AdMob)
│   ├── PlayerViewModel.kt           # Gestion état ExoPlayer (buffer, tracks, lifecycle)
│   ├── AdManager.kt                 # Gestion publicités interstitielles + bannières
│   ├── VideoListActivity.kt         # Liste vidéos favorites (Room DB)
│   ├── AddVideoActivity.kt          # Ajouter vidéo à favoris
│   │
│   ├── ads/
│   │   └── AdsController.kt         # Orchestration chargement/affichage pub
│   │
│   ├── player/
│   │   └── PlayerController.kt      # Validation URLs (schéma, longueur, format)
│   │
│   ├── security/
│   │   └── PermissionHelper.kt      # Vérification permissions + signatures
│   │
│   └── ui/
│       ├── PlayerUiState.kt         # Machine à états (LoadingAds, Ready, Error, Fallback)
│       └── theme/                   # Thème Material3
```

#### **Fonctionnalités Principales**
1. **Lecture vidéo streaming (HLS/DASH/RTSP)** via Media3 ExoPlayer
2. **Publicités AdMob** (Interstitiel avant lecture + Bannière fallback)
3. **Détection AdBlocker** (soft failover : 3 tentatives ratées → dialogue strict)
4. **Picture-in-Picture (PiP)**
5. **Gestion multi-qualités** (changement de track vidéo)
6. **Buffer optimisé** (démarrage 1.5s, robustesse 15s)
7. **Liste favoris** (Room Database)

#### **Sécurité Actuelle**
```xml
<!-- AndroidManifest.xml -->
<permission
    android:name="com.example.stv.PERMISSION_LAUNCH_PLAYER"
    android:protectionLevel="signature" />

<activity
    android:name=".PlayerActivity"
    android:exported="true"
    android:permission="com.example.stv.PERMISSION_LAUNCH_PLAYER" />
```

**Code de vérification dans PlayerActivity :**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    val permissionHelper = PermissionHelper(this)
    val callerPackage = callingPackage
    
    if (callerPackage != null && !permissionHelper.isCallerAuthorized(callerPackage, requiredPermission)) {
        Log.e(TAG, "Unauthorized caller. Finishing activity.")
        finish()
        return
    }
    // ...
}
```

**⚠️ PROBLÈME :** `protectionLevel="signature"` **BLOQUE** toutes les apps tierces (même SoukiTV) si elles ne sont pas signées avec la **même clé** que STV.

#### **AdMob Configuration**
```kotlin
// Flavors dans build.gradle.kts
dev {
    buildConfigField("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712") // Test ID
    buildConfigField("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/6300978111")
}

prod {
    // Actuellement utilise aussi Test IDs (à remplacer avant publication)
}
```

**Stratégie publicitaire :**
- **Interstitiel** affiché **AVANT** le démarrage de la vidéo (délai 10s max)
- Si échec 3 fois → **Dialogue strict AdBlock** (fermeture forcée)
- Si "No Fill" → **Fallback bannière** (pas de blocage)
- **SKIP_ADS** extra pour bypasser (usage interne)

---

### 2️⃣ Application SoukiTV (Catalogue)

#### **Package & Configuration**
```
Package Name: com.example.soukitv
Version: 1.0 (versionCode 1)
MinSDK: 24, TargetSDK: 35
Signing: ../app/release.keystore (réutilise le keystore de STV)
```

#### **Structure des Fichiers**
```
soukitv/src/main/
├── AndroidManifest.xml
├── java/com/example/soukitv/
│   ├── MainActivity.kt              # Activity principale
│   │
│   ├── data/
│   │   └── ChannelRepository.kt     # Source données (hardcodé pour l'instant)
│   │
│   ├── model/
│   │   ├── Channel.kt               # (id, name, logoUrl, streamUrl, category)
│   │   └── Category.kt              # (name, channels)
│   │
│   └── ui/
│       ├── home/
│       │   ├── HomeScreen.kt        # Interface catalogue (LazyColumn + catégories)
│       │   └── HomeViewModel.kt     # Gestion état UI
│       └── theme/
```

#### **Fonctionnalités**
1. **Catalogue de chaînes** groupées par catégorie (News, Sports, Science, Movies)
2. **Détection STV installé** (package name com.example.stv / .dev / .prod)
3. **Lancement STV** via Intent explicite (`setClassName()`)
4. **Dialogue d'installation** si STV absent (Toast actuel)

#### **Code de Lancement STV (HomeScreen.kt)**
```kotlin
onChannelClick = { channel ->
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

    if (isInstalled) {
        try {
            val actualPackageName = stvPackageNames.firstOrNull { /* vérif */ } ?: "com.example.stv"
            
            val intent = Intent()
            intent.setClassName(actualPackageName, "com.example.stv.PlayerActivity")
            intent.putExtra("VIDEO_URL", channel.streamUrl)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching STV Player: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        showInstallDialog = true // Actuellement juste un état booléen
    }
}
```

#### **Permissions Déclarées**
```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
<uses-permission android:name="com.example.stv.PERMISSION_LAUNCH_PLAYER" />

<queries>
    <package android:name="com.example.stv" />
</queries>
```

**⚠️ PROBLÈME :** 
- SoukiTV **demande** la permission `com.example.stv.PERMISSION_LAUNCH_PLAYER`
- Mais cette permission est `signature-level` dans STV
- **Résultat :** SoukiTV **NE PEUT PAS** lancer PlayerActivity sauf si signé avec la même clé ET si les deux apps sont installées simultanément

---

## 🔍 ANALYSE DES PROBLÈMES EN DÉTAIL

### 🔴 PROBLÈME #1 : Protection Signature Bloque SoukiTV

**Diagnostic :**
```
PlayerActivity.onCreate() :
    if (!permissionHelper.isCallerAuthorized(callerPackage, requiredPermission)) {
        finish() ← BLOQUE SOUKITV
    }
```

**Pourquoi c'est un problème :**
- `protectionLevel="signature"` = **même clé de signature obligatoire**
- SoukiTV et STV **partagent actuellement le même keystore**, donc **techniquement compatible**
- MAIS : `PermissionHelper.verifySignature()` vérifie la signature **au runtime**
- Si STV est publié sur Play Store avec une clé, puis SoukiTV avec la même clé → **ça devrait fonctionner**
- CEPENDANT : Si STV est installé via APK local (debug) et SoukiTV via Play Store (ou vice-versa) → **ÉCHEC**

**Conséquence :**
- En **production** (Play Store), si les deux apps utilisent le **même certificat**, ça peut fonctionner
- En **développement**, très complexe à tester (nécessite signature identique à chaque fois)

**Solution recommandée :**
- Utiliser `protectionLevel="dangerous"` ou simplement **retirer la permission**
- Créer une **whitelist de package names autorisés** dans `PermissionHelper`
- Exemple : `listOf("com.example.soukitv", "com.example.future_catalog_app")`

---

### 🔴 PROBLÈME #2 : Absence de Deep Link / Intent Filter

**Situation actuelle :**
```xml
<activity
    android:name=".PlayerActivity"
    android:exported="true" />
```

**Aucun Intent Filter défini !**

**Conséquence :**
- Les autres apps doivent utiliser un **Intent explicite** (`setClassName()`)
- Fragile : nécessite de connaître le nom exact du package et de l'activité
- **Impossible d'ouvrir STV** depuis un navigateur, un lien, ou via Intent implicite

**Solution recommandée :**
Ajouter un **Deep Link** et un **Intent Filter** pour les URLs de type `stv://play?url=...` ou `https://stv.app/play?url=...`

```xml
<activity
    android:name=".PlayerActivity"
    android:exported="true">
    
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- Deep Link stv://play -->
        <data
            android:scheme="stv"
            android:host="play" />
        
        <!-- Intent pour URLs m3u8, mp4, etc. -->
        <data android:scheme="http" android:mimeType="application/vnd.apple.mpegurl" />
        <data android:scheme="https" android:mimeType="application/vnd.apple.mpegurl" />
        <data android:scheme="http" android:pathPattern=".*\\.m3u8" />
        <data android:scheme="https" android:pathPattern=".*\\.m3u8" />
    </intent-filter>
</activity>
```

**Avantages :**
- ✅ SoukiTV peut utiliser un **Intent implicite** : `Intent(Intent.ACTION_VIEW, Uri.parse("stv://play?url=$streamUrl"))`
- ✅ Autres apps tierces peuvent aussi ouvrir STV
- ✅ Liens cliquables dans navigateur → ouvre STV automatiquement
- ✅ Plus robuste face aux changements de package name (flavors)

---

### 🔴 PROBLÈME #3 : Pas de Redirection Play Store dans SoukiTV

**Code actuel :**
```kotlin
if (isInstalled) {
    // Lancer STV
} else {
    showInstallDialog = true // État booléen, dialogue vide ?
}
```

**Manque :**
- Aucun **lien vers Play Store** pour installer STV
- Aucun message clair pour l'utilisateur

**Solution recommandée :**
```kotlin
if (!isInstalled) {
    // Dialogue avec bouton "Installer STV"
    AlertDialog.Builder(context)
        .setTitle("STV Player requis")
        .setMessage("STV Player est nécessaire pour lire cette chaîne. Voulez-vous l'installer ?")
        .setPositiveButton("Installer") { _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.example.stv"))
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback navigateur si Play Store indisponible
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.example.stv"))
                context.startActivity(intent)
            }
        }
        .setNegativeButton("Annuler", null)
        .show()
}
```

---

### 🟡 PROBLÈME #4 : Gestion Multi-Flavors Complexe

**Code actuel dans SoukiTV :**
```kotlin
val stvPackageNames = listOf(
    "com.example.stv",
    "com.example.stv.dev",
    "com.example.stv.prod"
)

val actualPackageName = stvPackageNames.firstOrNull { /* check */ } ?: "com.example.stv"
intent.setClassName(actualPackageName, "com.example.stv.PlayerActivity")
```

**Problèmes :**
- Logique complexe et répétitive
- Si on ajoute d'autres flavors → nécessite mise à jour de SoukiTV
- Fragile face aux refactorings

**Solutions :**

#### Option A : Utiliser un Intent Implicite avec Deep Link
```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stv://play?url=${channel.streamUrl}"))
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

if (intent.resolveActivity(context.packageManager) != null) {
    context.startActivity(intent)
} else {
    // Afficher dialogue installation
}
```

#### Option B : Package Name Unifié en Production
- Publier **uniquement** `com.example.stv` sur Play Store
- Les flavors `dev` et `prod` ne servent qu'en développement
- SoukiTV cherche **seulement** `com.example.stv` en production

**Recommandation :** **Option A (Deep Link)** = Plus flexible, standard Android

---

### 🟡 PROBLÈME #5 : Validation URL Côté SoukiTV Absente

**Situation :**
- SoukiTV envoie directement `channel.streamUrl` à STV
- Aucune vérification préalable (format, domaine, etc.)

**Risques :**
- URLs malformées → crash de STV
- URLs malveillantes → faille de sécurité

**Solution :**
Partager la logique de validation entre STV et SoukiTV :

1. **Créer une librairie partagée** (module commun)
2. Ou **dupliquer** `PlayerController.validateStreamUrl()` dans SoukiTV
3. Valider l'URL **avant** de lancer STV

```kotlin
// Dans SoukiTV
private fun validateUrl(url: String): Boolean {
    if (url.isBlank()) return false
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false
    if (url.length > 2048) return false
    return Patterns.WEB_URL.matcher(url).matches()
}

onChannelClick = { channel ->
    if (!validateUrl(channel.streamUrl)) {
        Toast.makeText(context, "URL invalide", Toast.LENGTH_SHORT).show()
        return@onChannelClick
    }
    
    // Lancer STV
}
```

---

## 📋 RECOMMANDATIONS & PLAN D'ACTION

### 🎯 Objectifs
1. **STV** = Player universel accessible depuis n'importe quelle app (Deep Link)
2. **SoukiTV** = Catalogue qui force l'utilisation de STV (avec fallback Play Store)
3. **Sécurité** = Validation stricte des URLs, mais pas de blocage des apps tierces légitimes

---

### ✅ PHASE 1 : Fixes Critiques (STV)

#### 1.1 — Retirer la Protection Signature (ou assouplir)

**Option A : Retirer complètement**
```xml
<!-- AndroidManifest.xml - STV -->
<activity
    android:name=".PlayerActivity"
    android:exported="true"
    android:launchMode="singleTask"
    <!-- RETIRER android:permission -->
>
```

**Option B : Whitelist de packages autorisés**
```kotlin
// PermissionHelper.kt
fun isCallerAuthorized(callerPackage: String?): Boolean {
    val allowedPackages = listOf(
        "com.example.soukitv",
        "com.example.stv", // Pour les appels internes
        "com.example.futurecatalog" // Futures apps
    )
    
    return callerPackage in allowedPackages
}
```

**Option C : Conserver signature-level + signer les deux apps avec la même clé**
- **Complexité** : Nécessite gestion stricte des clés de signature
- **Avantage** : Sécurité maximale
- **Inconvénient** : Difficile de collaborer avec des développeurs tiers

**Recommandation :** **Option B (Whitelist)** = Équilibre sécurité/flexibilité

---

#### 1.2 — Ajouter Deep Link + Intent Filter

**Fichier : `app/src/main/AndroidManifest.xml`**

```xml
<activity
    android:name=".PlayerActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:excludeFromRecents="true"
    android:supportsPictureInPicture="true"
    android:theme="@style/Theme.STV">
    
    <!-- Intent Filter pour Deep Link -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- stv://play?url=... -->
        <data
            android:scheme="stv"
            android:host="play" />
    </intent-filter>
    
    <!-- Intent Filter pour fichiers vidéo -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="http" />
        <data android:scheme="https" />
        <data android:mimeType="application/vnd.apple.mpegurl" />
    </intent-filter>
</activity>
```

**Modifier `PlayerActivity.onCreate()` pour récupérer l'URL du Deep Link :**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Récupérer l'URL depuis Intent extra OU depuis Deep Link
    val videoUrl = when {
        intent.getStringExtra("VIDEO_URL") != null -> intent.getStringExtra("VIDEO_URL")
        intent.data != null -> intent.data?.getQueryParameter("url") // Deep Link stv://play?url=...
        else -> null
    }
    
    // Validation...
}
```

**Avantages :**
- ✅ SoukiTV peut utiliser `Intent(ACTION_VIEW, Uri.parse("stv://play?url=$url"))`
- ✅ Fonctionne même si le package name change (flavors)
- ✅ Standard Android (robuste)

---

#### 1.3 — Améliorer le Dialogue AdBlock (UX)

**Problème actuel :**
- Dialogue "strict" avec seulement bouton "Fermer" → Frustrant
- Pas d'explication claire

**Solution :**
```kotlin
// AdManager.kt
private fun showStrictBlockerDialog(activity: Activity) {
    AlertDialog.Builder(activity)
        .setTitle("Publicité bloquée")
        .setMessage(
            "STV Player est gratuit et financé par la publicité. " +
            "Veuillez désactiver votre bloqueur de publicités pour continuer.\n\n" +
            "Si vous n'utilisez pas de bloqueur, vérifiez votre connexion internet."
        )
        .setPositiveButton("Réessayer") { dialog, _ ->
            dialog.dismiss()
            // Relancer le chargement de l'ad
            adManager.loadInterstitialAd()
        }
        .setNegativeButton("Fermer") { dialog, _ ->
            dialog.dismiss()
            activity.finish()
        }
        .setCancelable(false)
        .show()
}
```

---

### ✅ PHASE 2 : Améliorer SoukiTV

#### 2.1 — Implémenter la Redirection Play Store

**Fichier : `soukitv/src/main/java/com/example/soukitv/ui/home/HomeScreen.kt`**

Créer une fonction utilitaire :

```kotlin
fun openPlayStore(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback : navigateur web
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
```

Modifier le dialogue d'installation :

```kotlin
if (!isInstalled) {
    AlertDialog.Builder(context)
        .setTitle("STV Player requis")
        .setMessage("Cette application nécessite STV Player pour lire les chaînes.")
        .setPositiveButton("Installer") { _, _ ->
            openPlayStore(context, "com.example.stv")
        }
        .setNegativeButton("Annuler", null)
        .show()
}
```

---

#### 2.2 — Utiliser Intent Implicite avec Deep Link

**Remplacer le code de lancement actuel :**

```kotlin
onChannelClick = { channel ->
    // Validation de l'URL
    if (!validateUrl(channel.streamUrl)) {
        Toast.makeText(context, "URL de chaîne invalide", Toast.LENGTH_SHORT).show()
        return@onChannelClick
    }
    
    // Intent implicite avec Deep Link
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("stv://play?url=${Uri.encode(channel.streamUrl)}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    
    // Vérifier si STV est installé (peut gérer cet intent)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Afficher dialogue installation
        AlertDialog.Builder(context)
            .setTitle("STV Player requis")
            .setMessage("Voulez-vous installer STV Player depuis le Play Store ?")
            .setPositiveButton("Installer") { _, _ ->
                openPlayStore(context, "com.example.stv")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

private fun validateUrl(url: String): Boolean {
    return url.isNotBlank() &&
           (url.startsWith("http://") || url.startsWith("https://")) &&
           url.length <= 2048
}
```

**Avantages :**
- ✅ Plus besoin de gérer les flavors manuellement
- ✅ Code plus simple et robuste
- ✅ Standard Android

---

#### 2.3 — Améliorer le Catalogue (Optionnel)

**Suggestions UX :**
1. **Recherche de chaînes** (SearchBar Material3)
2. **Favoris** (Room Database dans SoukiTV)
3. **Thème sombre/clair** (suivre les paramètres système)
4. **Indicateur de qualité** (HD, 4K, SD) dans les cards de chaînes
5. **Section "Récemment regardé"** (SharedPreferences)

---

### ✅ PHASE 3 : Préparation Publication Play Store (STV)

#### 3.1 — Remplacer les IDs AdMob Test par les vrais

**Fichier : `app/build.gradle.kts`**

```kotlin
productFlavors {
    create("prod") {
        dimension = "environment"
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-XXXXXX/YYYYYYYY\"") // ← IDs réels
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-XXXXXX/ZZZZZZZZ\"")
    }
}
```

**⚠️ Important :** Garder les test IDs pour le flavor `dev` !

---

#### 3.2 — Créer les Assets Play Store

1. **Icône de l'application** (512x512 PNG, sans transparence)
2. **Feature Graphic** (1024x500 PNG, bannière en haut de la page)
3. **Screenshots** (minimum 2, recommandé 8) :
   - Interface principale (MainActivity)
   - Lecteur en lecture (PlayerActivity)
   - Sélection de qualité
   - Mode PiP (si pertinent)
4. **Vidéo de démo** (30s, optionnelle mais recommandée)

---

#### 3.3 — Rédiger la Description Play Store

**Exemple (Français) :**

```
🎬 STV Player - Lecteur de Streaming Universel

STV Player est un lecteur vidéo puissant et gratuit conçu pour lire vos flux streaming (HLS, DASH, RTSP).

✨ Fonctionnalités :
• Lecture haute performance avec Media3 ExoPlayer
• Support HLS, DASH, RTSP, MP4
• Changement de qualité vidéo en direct
• Mode Picture-in-Picture (PiP)
• Buffer optimisé pour un démarrage rapide
• Interface Material Design 3

📺 Compatible avec :
• SoukiTV (catalogue de chaînes IPTV)
• Vos propres URLs de streaming
• Applications tierces via Deep Link

🆓 Gratuit et financé par la publicité (AdMob)

🔗 Deep Link : stv://play?url=VOTRE_URL
```

**Exemple (Anglais) :**
```
🎬 STV Player - Universal Streaming Player

STV Player is a powerful, free video player designed to play your streaming content (HLS, DASH, RTSP).

✨ Features:
• High-performance playback with Media3 ExoPlayer
• Supports HLS, DASH, RTSP, MP4
• Live video quality switching
• Picture-in-Picture (PiP) mode
• Optimized buffering for fast startup
• Material Design 3 interface

📺 Compatible with:
• SoukiTV (IPTV channel catalog)
• Your own streaming URLs
• Third-party apps via Deep Link

🆓 Free and ad-supported (AdMob)

🔗 Deep Link: stv://play?url=YOUR_URL
```

---

#### 3.4 — Configuration ProGuard (Release)

**Fichier : `app/proguard-rules.pro`**

Vérifier que les règles incluent :

```proguard
# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Compose
-keep class androidx.compose.** { *; }
```

---

#### 3.5 — Tester l'APK Release

```powershell
# Build Release (flavor prod)
.\gradlew assembleProdRelease

# Vérifier la signature
jarsigner -verify -verbose -certs app\build\outputs\apk\prod\release\app-prod-release.apk

# Installer sur device
adb install app\build\outputs\apk\prod\release\app-prod-release.apk
```

**Tests à effectuer :**
1. Publicités s'affichent correctement
2. Deep Link fonctionne (`adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://..."`)
3. SoukiTV peut lancer STV
4. Pas de crash au lancement
5. ProGuard n'a pas cassé les classes (ExoPlayer, AdMob)

---

### ✅ PHASE 4 : Optimisations Avancées (Futur)

#### 4.1 — Librairie Partagée STV-SDK

**Objectif :** Permettre aux développeurs tiers d'intégrer facilement STV dans leurs apps

**Contenu :**
```kotlin
// Module stv-sdk
class STVPlayerLauncher(private val context: Context) {
    
    fun isSTVInstalled(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stv://play?url="))
        return intent.resolveActivity(context.packageManager) != null
    }
    
    fun launchSTV(streamUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stv://play?url=${Uri.encode(streamUrl)}"))
        context.startActivity(intent)
    }
    
    fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.example.stv"))
        context.startActivity(intent)
    }
}
```

**Publication :** Maven Central ou JitPack

---

#### 4.2 — Dashboard Admin pour SoukiTV

**Fonctionnalités :**
- API REST pour gérer les chaînes dynamiquement
- Backend Firebase ou custom (Node.js/Python)
- Notifications Push pour nouvelles chaînes
- Analytics (chaînes les plus regardées)

**Architecture :**
```
Firebase Realtime Database
    ├── categories/
    │   ├── news/
    │   │   └── channels/
    │   │       ├── channel1
    │   │       └── channel2
    │   └── sports/
    │       └── channels/
    └── featured/ (chaînes mises en avant)
```

---

#### 4.3 — Analytics & Crash Reporting

**Firebase Crashlytics + Analytics :**

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.1")
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
}
```

**Events à tracker :**
- `video_play_start` (channel_id, url)
- `video_play_complete` (duration_watched)
- `ad_impression` (ad_unit_id)
- `ad_click`
- `quality_change` (from, to)
- `pip_enter` / `pip_exit`

---

## 📊 RÉSUMÉ DES FICHIERS À MODIFIER

### STV (app)

| Fichier | Action | Priorité |
|---------|--------|----------|
| `AndroidManifest.xml` | Ajouter Intent Filter Deep Link | 🔴 CRITIQUE |
| `PlayerActivity.kt` | Récupérer URL depuis Deep Link | 🔴 CRITIQUE |
| `security/PermissionHelper.kt` | Assouplir vérification (whitelist) | 🔴 CRITIQUE |
| `AdManager.kt` | Améliorer dialogue AdBlock | 🟡 MODÉRÉ |
| `build.gradle.kts` | Remplacer IDs AdMob Test (prod) | 🟠 MAJEUR |

### SoukiTV (soukitv)

| Fichier | Action | Priorité |
|---------|--------|----------|
| `ui/home/HomeScreen.kt` | Utiliser Intent implicite + Dialogue Play Store | 🔴 CRITIQUE |
| `data/ChannelRepository.kt` | Ajouter validation URL | 🟡 MODÉRÉ |

---

## 🚀 TIMELINE ESTIMÉE

| Phase | Durée | Effort |
|-------|-------|--------|
| **Phase 1 : Fixes Critiques STV** | 2-3 heures | Moyen |
| **Phase 2 : Améliorer SoukiTV** | 1-2 heures | Faible |
| **Phase 3 : Prépa Play Store** | 4-6 heures | Élevé (assets + tests) |
| **Phase 4 : Optimisations** | 1-2 semaines | Élevé (optionnel) |

---

## 🔍 TESTS DE VALIDATION

### Test Scénario 1 : SoukiTV → STV (STV installé)
1. Ouvrir SoukiTV
2. Cliquer sur une chaîne (ex: "NASA TV")
3. **Résultat attendu :** STV s'ouvre, publicité interstitielle affichée, puis vidéo démarre

### Test Scénario 2 : SoukiTV → STV (STV non installé)
1. Désinstaller STV
2. Ouvrir SoukiTV
3. Cliquer sur une chaîne
4. **Résultat attendu :** Dialogue "STV Player requis" avec bouton "Installer"
5. Cliquer "Installer"
6. **Résultat attendu :** Play Store s'ouvre sur la page de STV

### Test Scénario 3 : Deep Link depuis Navigateur
1. Ouvrir un lien : `stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`
2. **Résultat attendu :** Android propose d'ouvrir avec STV, puis vidéo démarre

### Test Scénario 4 : URL Invalide
1. Dans SoukiTV, modifier manuellement une URL de chaîne pour la rendre invalide
2. Cliquer sur cette chaîne
3. **Résultat attendu :** Toast "URL invalide" dans SoukiTV, STV ne s'ouvre pas

### Test Scénario 5 : AdBlock Détecté
1. Activer un AdBlocker (ex: Blokada, DNS66)
2. Lancer une vidéo depuis STV
3. Après 3 tentatives ratées
4. **Résultat attendu :** Dialogue "Publicité bloquée" avec bouton "Réessayer"

---

## 📚 RESSOURCES & RÉFÉRENCES

### Documentation Android
- [Deep Links](https://developer.android.com/training/app-links/deep-linking)
- [Intent Filters](https://developer.android.com/guide/components/intents-filters)
- [Custom Permissions](https://developer.android.com/guide/topics/permissions/defining)

### AdMob
- [Interstitial Ads Guide](https://developers.google.com/admob/android/interstitial)
- [Test IDs](https://developers.google.com/admob/android/test-ads)

### Media3 ExoPlayer
- [ExoPlayer Guide](https://developer.android.com/guide/topics/media/exoplayer)
- [Buffering Strategies](https://exoplayer.dev/customization.html#loadcontrol)

---

## 🎯 CONCLUSION

### État Actuel
- ✅ **STV** : Player fonctionnel, publicités intégrées, performances optimisées
- ✅ **SoukiTV** : Catalogue basique fonctionnel
- ❌ **Intégration** : Bloquée par protection signature + absence de Deep Link

### Après Corrections (Phase 1-2)
- ✅ **STV** : Accessible universellement via Deep Link
- ✅ **SoukiTV** : Détection + redirection Play Store fonctionnelle
- ✅ **Sécurité** : Validation URL stricte, whitelist packages autorisés
- ✅ **UX** : Fluide, messages clairs, pas de frustration utilisateur

### Objectif Final (Phase 3-4)
- 🚀 **STV sur Play Store** (application standalone de qualité production)
- 🚀 **SoukiTV** comme premier catalogue partenaire
- 🚀 **Écosystème extensible** pour futures apps catalogue
- 🚀 **SDK public** pour développeurs tiers

---

**Prochaines étapes recommandées :**
1. Implémenter les fixes de la Phase 1 (Deep Link + assouplir sécurité)
2. Tester l'intégration SoukiTV ↔ STV
3. Préparer les assets Play Store
4. Publier STV en beta fermée sur Play Store (internal testing)
5. Itérer selon les retours utilisateurs

---

**📝 Note :** Ce rapport peut être utilisé comme cahier des charges pour l'implémentation. Chaque section contient du code prêt à l'emploi et des explications détaillées.

**Auteur :** GitHub Copilot  
**Version :** 1.0  
**Date :** 26 Février 2026

