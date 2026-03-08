# Guide de développement - Apps Catalogue STV

## Objectif
Ce guide explique comment développer des apps catalogue qui forcent l'utilisation de STV Player, tout en maintenant la sécurité.

---

## Principe de sécurité

**STV Player vérifie la signature** de l'app appelante pour autoriser l'accès :
- ✅ **Apps signées avec la MÊME clé** → Autorisées (tes catalogues)
- ✅ **Intents système** (deep links, fichiers) → Autorisés
- ❌ **Apps tierces** → Refusées

---

## Étapes pour créer une nouvelle app catalogue

### 1. Créer le projet Android
- Nom : ex. `SouKiTV2`, `SouKiTV3`, etc.
- Package : ex. `com.example.soukitv2`
- Language : Kotlin
- UI : Compose (recommandé)

### 2. **IMPORTANT** : Signer avec la MÊME clé

#### Option A : Via Android Studio (Release Build)
1. **Build** → **Generate Signed Bundle/APK**
2. **APK** → **Next**
3. **Choose existing...** → Sélectionner `C:\Users\soufi\AndroidStudioProjects\STV5\release.keystore`
4. Renseigner les mots de passe (ceux de `keystore.properties`)
5. Build Variant : **release**
6. **Finish**

#### Option B : Via Gradle (automatique)
Copier la configuration de `STV5/app/build.gradle.kts` :
```kotlin
// Load signing config
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

signingConfigs {
    create("release") {
        storeFile = File(rootProject.rootDir, keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(...)
    }
}
```

**Et créer un fichier local `keystore.properties` dans le nouveau projet** :
```properties
storeFile=../STV5/release.keystore
storePassword=TON_MOT_DE_PASSE
keyAlias=key0
keyPassword=TON_MOT_DE_PASSE
```

---

### 3. Lancer STV Player depuis ton catalogue

#### Intent explicite (recommandé)
```kotlin
fun playStream(context: Context, streamUrl: String) {
    val intent = Intent("com.example.stv.action.PLAY_STREAM")
    intent.putExtra("VIDEO_URL", streamUrl)
    intent.putExtra("SKIP_ADS", false) // Optionnel
    
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // STV Player pas installé
        Toast.makeText(context, "STV Player requis", Toast.LENGTH_SHORT).show()
        // Optionnel : rediriger vers Play Store
    }
}
```

#### Deep Link (alternative)
```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stv://play?url=$streamUrl"))
context.startActivity(intent)
```

---

### 4. Vérifier que STV Player est installé

```kotlin
fun isSTVPlayerInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.example.stv", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
```

---

### 5. Rediriger vers Play Store si absent

```kotlin
fun openPlayStore(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=com.example.stv")
    }
    context.startActivity(intent)
}
```

---

## Checklist avant publication

- [ ] App signée avec `release.keystore` (même clé que STV Player)
- [ ] Test : lancer STV Player depuis ton app → doit fonctionner
- [ ] Test : désinstaller STV Player → gérer le cas "app non installée"
- [ ] Version release testée sur device physique
- [ ] Keystore sauvegardé en lieu sûr (backup chiffré)

---

## Sécurité du keystore

### ⚠️ IMPORTANT
- **Ne JAMAIS partager** `release.keystore` publiquement
- **Ne JAMAIS commit** le keystore dans Git
- **Backup sécurisé** : Google Drive chiffré, 1Password, coffre-fort

### Pour une nouvelle machine
Copier `release.keystore` et `keystore.properties` dans le nouveau projet.

---

## Exemple complet - Catalogue minimal

```kotlin
@Composable
fun VideoCard(video: Video, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column {
            Text(video.title)
            Text(video.url)
        }
    }
}

@Composable
fun CatalogScreen(videos: List<Video>) {
    val context = LocalContext.current
    
    LazyColumn {
        items(videos) { video ->
            VideoCard(video) {
                playStream(context, video.url)
            }
        }
    }
}

fun playStream(context: Context, streamUrl: String) {
    if (!isSTVPlayerInstalled(context)) {
        // Proposer d'installer STV Player
        openPlayStore(context)
        return
    }
    
    val intent = Intent("com.example.stv.action.PLAY_STREAM")
    intent.putExtra("VIDEO_URL", streamUrl)
    context.startActivity(intent)
}
```

---

## FAQ

**Q : Puis-je signer mes catalogues avec une clé différente ?**  
❌ Non. STV Player refusera l'appel. Tu DOIS utiliser la même clé.

**Q : Que se passe-t-il si je perds le keystore ?**  
⚠️ Tu ne pourras plus mettre à jour STV Player sur Play Store. Sauvegarde-le !

**Q : Puis-je développer 10 catalogues différents ?**  
✅ Oui, tant qu'ils sont tous signés avec la même clé.

**Q : Et si un utilisateur désinstalle STV Player ?**  
⚠️ Ton app doit détecter l'absence et proposer de l'installer via Play Store.

---

## Contact / Support
Pour toute question technique : [ton email ou support]

