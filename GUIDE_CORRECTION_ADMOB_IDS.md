# 🔴 GUIDE CORRECTION — IDs AdMob de Test → Production

**Date** : 7 mars 2026  
**Problème** : Toutes les publicités utilisent des IDs de test Google → **0€ de revenus**  
**Impact** : CRITIQUE — L'app fonctionne mais ne génère aucun revenu publicitaire  
**Temps estimé** : 15 minutes (une fois les IDs obtenus)

---

## 📌 COMPRENDRE LE PROBLÈME

### Qu'est-ce qu'un ID AdMob ?

Google AdMob utilise **3 types d'identifiants** :

| Type | Format | Rôle |
|------|--------|------|
| **App ID** | `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY` | Identifie ton APPLICATION dans AdMob |
| **Interstitial Ad Unit ID** | `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY` | Identifie un bloc pub INTERSTITIEL (plein écran) |
| **Banner Ad Unit ID** | `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ` | Identifie un bloc pub BANNIÈRE |

### Qu'est-ce qu'un ID de test ?

Google fournit des IDs de test **universels** pour le développement :

```
App ID test        : ca-app-pub-3940256099942544~3347511713
Interstitial test  : ca-app-pub-3940256099942544/1033173712
Banner test        : ca-app-pub-3940256099942544/6300978111
```

Ces IDs affichent de **fausses publicités** (marquées "Test Ad") et ne génèrent **jamais** de revenus. Ils sont faits pour tester sans violer les règles Google.

### Situation actuelle de STV Player

**TOUS les IDs sont des IDs de test** — dans les 3 endroits suivants :

| # | Fichier | Ligne(s) | ID de test utilisé | Type |
|---|---------|----------|---------------------|------|
| 1 | `app/build.gradle.kts` | L31-32 | `ca-app-pub-3940256099942544/1033173712` | Interstitiel (defaultConfig) |
| 2 | `app/build.gradle.kts` | L31-32 | `ca-app-pub-3940256099942544/6300978111` | Bannière (defaultConfig) |
| 3 | `app/build.gradle.kts` | L40-41 | `ca-app-pub-3940256099942544/1033173712` | Interstitiel (flavor dev) |
| 4 | `app/build.gradle.kts` | L40-41 | `ca-app-pub-3940256099942544/6300978111` | Bannière (flavor dev) |
| 5 | `app/build.gradle.kts` | L48-49 | `ca-app-pub-3940256099942544/1033173712` | Interstitiel (flavor **prod**) ⚠️ |
| 6 | `app/build.gradle.kts` | L48-49 | `ca-app-pub-3940256099942544/6300978111` | Bannière (flavor **prod**) ⚠️ |
| 7 | `AndroidManifest.xml` | L25 | `ca-app-pub-3940256099942544~3347511713` | **App ID** ⚠️ |
| 8 | `PlayerActivity.kt` | L786 | `ca-app-pub-3940256099942544/6300978111` | Bannière en dur (FallbackBanner) ⚠️ |

**Résultat** : Les pubs s'affichent mais ce sont de fausses pubs → **0€ de revenus**.

---

## 🔧 ÉTAPE 1 : CRÉER UN COMPTE ADMOB ET OBTENIR LES IDS

### 1.1 Créer un compte AdMob

1. Aller sur **https://admob.google.com**
2. Se connecter avec ton compte Google
3. Accepter les conditions d'utilisation
4. Configurer les informations de paiement (compte bancaire / AdSense)

### 1.2 Enregistrer l'application STV

1. Dans AdMob, cliquer sur **"Apps"** → **"Ajouter une application"**
2. Choisir **"Android"**
3. Si l'app n'est pas encore sur le Play Store → sélectionner **"Non"**
4. Nom de l'app : **"STV Player"**
5. **Noter l'App ID** généré (format : `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`)

### 1.3 Créer les blocs publicitaires (Ad Units)

#### Bloc 1 : Interstitiel

1. Aller dans ton app → **"Blocs d'annonces"** → **"Ajouter un bloc"**
2. Type : **"Interstitiel"**
3. Nom : `STV_Interstitial_PreVideo`
4. **Noter l'Ad Unit ID** (format : `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`)

#### Bloc 2 : Bannière

1. Ajouter un autre bloc
2. Type : **"Bannière"**
3. Nom : `STV_Banner_Fallback`
4. **Noter l'Ad Unit ID** (format : `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`)

### 1.4 Résumé — Tu devrais avoir 3 IDs

```
📋 Mes IDs AdMob Production :

App ID           : ca-app-pub-________________~__________
Interstitial ID  : ca-app-pub-________________/__________
Banner ID        : ca-app-pub-________________/__________
```

> ⚠️ **IMPORTANT** : Ne jamais partager ces IDs publiquement (GitHub, forums, etc.)

---

## 🔧 ÉTAPE 2 : SÉCURISER LES IDS (BONNE PRATIQUE)

Au lieu de mettre les IDs directement dans le code (risque de fuite sur GitHub), on les met dans un fichier local **non versionné**.

### 2.1 Ajouter les IDs dans `local.properties`

Le fichier `local.properties` est **déjà ignoré par Git** (`.gitignore`). C'est l'endroit idéal.

Ouvrir `C:\Users\soufi\AndroidStudioProjects\STV7\local.properties` et ajouter à la fin :

```properties
# === AdMob IDs Production ===
admob.app.id=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.interstitial.id=ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
admob.banner.id=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

> Remplacer les `X`, `Y`, `Z` par tes vrais IDs.

### 2.2 Vérifier que `local.properties` est dans `.gitignore`

Le fichier `.gitignore` doit contenir `local.properties`. C'est normalement le cas par défaut dans les projets Android Studio.

---

## 🔧 ÉTAPE 3 : MODIFIER `app/build.gradle.kts`

### Code actuel (PROBLÈME) :

```kotlin
// defaultConfig (L31-32)
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")

// dev flavor (L40-41) — OK, garde les IDs test
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")

// prod flavor (L48-49) — ⚠️ PROBLÈME : IDs test aussi !
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
```

### Code corrigé (SOLUTION) :

```kotlin
// En haut du fichier, charger les propriétés locales :
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// defaultConfig — IDs test par défaut (fallback sécurisé)
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")

// dev flavor — Toujours IDs test (développement) ✅ Ne pas changer
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")

// prod flavor — IDs PRODUCTION lus depuis local.properties ✅
val prodInterstitialId = localProperties.getProperty("admob.interstitial.id", "ca-app-pub-3940256099942544/1033173712")
val prodBannerId = localProperties.getProperty("admob.banner.id", "ca-app-pub-3940256099942544/6300978111")
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$prodInterstitialId\"")
buildConfigField("String", "ADMOB_BANNER_ID", "\"$prodBannerId\"")
```

### Pourquoi ce design ?

| Situation | Comportement |
|-----------|-------------|
| Dev/Debug sans `local.properties` | IDs test → pubs factices, pas de violation Google |
| Dev/Debug avec `local.properties` | IDs test (flavor dev) → sûr |
| **Prod Release avec `local.properties`** | **IDs production → vrais revenus** ✅ |
| Prod Release sans `local.properties` | IDs test (fallback) → pas de crash, pas de revenus |

---

## 🔧 ÉTAPE 4 : MODIFIER `AndroidManifest.xml`

### L'App ID AdMob dans le Manifest

L'App ID AdMob est **obligatoire** dans le `AndroidManifest.xml`. Il identifie ton app auprès du SDK Google.

### Code actuel (PROBLÈME) :

```xml
<!-- L25 -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>
```

### Solution : Utiliser un placeholder résolu par le build

On peut utiliser `manifestPlaceholders` dans `build.gradle.kts` pour injecter l'App ID dynamiquement :

```kotlin
// Dans build.gradle.kts, flavor prod :
val prodAppId = localProperties.getProperty("admob.app.id", "ca-app-pub-3940256099942544~3347511713")
manifestPlaceholders["admobAppId"] = prodAppId

// Dans build.gradle.kts, flavor dev :
manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
```

Et dans `AndroidManifest.xml` :

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${admobAppId}"/>
```

---

## 🔧 ÉTAPE 5 : CORRIGER L'ID EN DUR DANS `PlayerActivity.kt`

### Code actuel (PROBLÈME, ligne 786) :

```kotlin
com.google.android.gms.ads.AdView(ctx).apply {
    setAdSize(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE)
    adUnitId = "ca-app-pub-3940256099942544/6300978111"  // ⚠️ ID EN DUR
    loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
}
```

### Code corrigé :

```kotlin
com.google.android.gms.ads.AdView(ctx).apply {
    setAdSize(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE)
    adUnitId = BuildConfig.ADMOB_BANNER_ID  // ✅ Utilise le BuildConfig (flavor-specific)
    loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
}
```

---

## 📋 RÉSUMÉ DES 4 FICHIERS À MODIFIER

| # | Fichier | Modification | Risque si oublié |
|---|---------|-------------|-----------------|
| 1 | `local.properties` | Ajouter 3 IDs AdMob production | Pas d'IDs disponibles |
| 2 | `app/build.gradle.kts` | Lire IDs depuis `local.properties` pour flavor prod | 0€ revenus |
| 3 | `AndroidManifest.xml` | Utiliser `${admobAppId}` placeholder | SDK AdMob refuse de fonctionner |
| 4 | `PlayerActivity.kt` L786 | Remplacer ID en dur → `BuildConfig.ADMOB_BANNER_ID` | Bannière fallback = fausse pub |

---

## ⚠️ RÈGLES GOOGLE ADMOB À RESPECTER

| Règle | Détail |
|-------|--------|
| **Ne JAMAIS cliquer sur tes propres pubs** | Google bannit le compte immédiatement |
| **Ne JAMAIS utiliser IDs production en debug** | Risque de ban pour "invalid traffic" |
| **Toujours tester avec IDs test** | Flavor `dev` garde les IDs test ✅ |
| **Ne JAMAIS publier les IDs sur GitHub** | Utiliser `local.properties` (ignoré par Git) ✅ |
| **Attendre 24-48h** après création du compte | Les premières pubs mettent du temps à apparaître |

---

## 🧪 COMMENT VÉRIFIER QUE ÇA FONCTIONNE

### Test 1 : Vérifier le BuildConfig

Après le build, vérifier le fichier généré :
```
app/build/generated/source/buildConfig/prod/release/com/example/stv/BuildConfig.java
```

Il doit contenir :
```java
public static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-VOTRE_VRAI_ID";
public static final String ADMOB_BANNER_ID = "ca-app-pub-VOTRE_VRAI_ID";
```

### Test 2 : Logcat

En exécutant l'APK prod release sur un device :
```
adb logcat -s AdManager AdsController
```

Tu dois voir :
```
D/AdManager: Ad was loaded.
D/AdManager: Ad showed fullscreen content.
D/AdsController: Ad showed successfully
```

Si tu vois `Ad failed to load` avec un code d'erreur, vérifier :
- Code 0 : Erreur interne → attendre et réessayer
- Code 1 : ID invalide → vérifier l'ID
- Code 2 : Erreur réseau → vérifier la connexion
- Code 3 : No Fill → normal les premières 24-48h

### Test 3 : Dashboard AdMob

Après 24-48h, vérifier sur https://admob.google.com :
- Impressions > 0
- Estimated revenue > 0€

---

## 🚀 PRÊT ? COMMANDE POUR APPLIQUER

Quand tu as tes 3 IDs AdMob production, dis-moi :

```
Mon App ID           : ca-app-pub-________________~__________
Mon Interstitial ID  : ca-app-pub-________________/__________
Mon Banner ID        : ca-app-pub-________________/__________
```

Et j'appliquerai les modifications dans les 4 fichiers automatiquement.

> ⚠️ Si tu n'as **pas encore** de compte AdMob, je peux appliquer la correction de structure maintenant (lecture depuis `local.properties`) et tu renseigneras les IDs plus tard.

---

*Guide rédigé le 7 mars 2026 — GitHub Copilot*

