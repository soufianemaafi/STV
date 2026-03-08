# 🎯 SOLUTION : Architecture Hybride STV

**Date :** 26 Février 2026  
**Problème :** Permettre l'ouverture publique de STV MAIS forcer son utilisation depuis SoukiTV

---

## 📋 ARCHITECTURE PROPOSÉE

### Scénario 1 : Apps Tierces (VLC style)
```
App Tierce → Intent ACTION_VIEW (Deep Link) → Android Chooser → User choisit STV ou autre
```
✅ L'utilisateur peut choisir entre STV, VLC, MX Player, etc.

### Scénario 2 : SoukiTV (Vos Apps)
```
SoukiTV → Intent Explicite (setClassName) → STV directement (pas de choix)
```
✅ L'utilisateur NE PEUT PAS choisir, STV s'ouvre automatiquement

---

## 🔧 MODIFICATIONS À APPORTER

### 1️⃣ STV - AndroidManifest.xml

**Ajouter DEUX intent-filters à PlayerActivity :**

```xml
<activity
    android:name=".PlayerActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:excludeFromRecents="true"
    android:supportsPictureInPicture="true"
    android:theme="@style/Theme.STV">
    
    <!-- ✅ POINT D'ENTRÉE PUBLIC : Deep Link (pour apps tierces) -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- Schéma stv://play?url=... -->
        <data
            android:scheme="stv"
            android:host="play" />
    </intent-filter>
    
    <!-- ✅ POINT D'ENTRÉE PUBLIC : Fichiers vidéo (comme VLC) -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        
        <data android:scheme="http" />
        <data android:scheme="https" />
        <data android:mimeType="application/vnd.apple.mpegurl" /> <!-- HLS .m3u8 -->
        <data android:mimeType="video/*" /> <!-- MP4, MKV, etc. -->
    </intent-filter>
</activity>
```

**⚠️ Retirer la permission signature** (trop restrictive) :
```xml
<!-- AVANT -->
android:permission="com.example.stv.PERMISSION_LAUNCH_PLAYER"

<!-- APRÈS -->
<!-- Pas de permission → ouvert à tous -->
```

---

### 2️⃣ SoukiTV - HomeScreen.kt

**Garder l'Intent EXPLICITE** (ne pas utiliser ACTION_VIEW) :

```kotlin
onChannelClick = { channel ->
    val stvPackageNames = listOf(
        "com.example.stv",
        "com.example.stv.dev",
        "com.example.stv.prod"
    )

    val actualPackageName = stvPackageNames.firstOrNull { packageName ->
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    if (actualPackageName != null) {
        try {
            // ✅ Intent EXPLICITE → Force STV (pas de chooser Android)
            val intent = Intent().apply {
                setClassName(actualPackageName, "com.example.stv.PlayerActivity")
                putExtra("VIDEO_URL", channel.streamUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        // STV non installé → Redirection Play Store
        showInstallDialog = true
    }
}
```

**Pourquoi ça marche ?**
- Intent explicite (`setClassName`) → **Pas de chooser Android**
- L'utilisateur ne voit jamais la liste des apps compatibles
- STV s'ouvre directement (ou erreur si absent)

---

### 3️⃣ STV - PlayerActivity.kt

**Gérer les DEUX types d'Intent :**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Récupérer l'URL depuis Intent extra OU Deep Link
    val videoUrl = when {
        // Cas 1 : Intent explicite depuis SoukiTV (VIDEO_URL extra)
        intent.hasExtra("VIDEO_URL") -> {
            Log.d(TAG, "Launched from catalog app (explicit intent)")
            intent.getStringExtra("VIDEO_URL")
        }
        
        // Cas 2 : Deep Link public (stv://play?url=...)
        intent.data != null -> {
            Log.d(TAG, "Launched via deep link: ${intent.data}")
            intent.data?.getQueryParameter("url")
        }
        
        // Cas 3 : Intent implicite (ACTION_VIEW avec URI direct)
        intent.action == Intent.ACTION_VIEW && intent.dataString != null -> {
            Log.d(TAG, "Launched via ACTION_VIEW: ${intent.dataString}")
            intent.dataString // URL directe (ex: http://example.com/video.m3u8)
        }
        
        else -> null
    }
    
    // Validation de l'URL...
    val urlError = playerController.validateStreamUrl(videoUrl)
    // ...
}
```

---

## 📊 COMPARAISON DES COMPORTEMENTS

### Apps Tierces (Utilisant ACTION_VIEW)

**Exemple :** Un navigateur web clique sur un lien `.m3u8`

```kotlin
// Code dans l'app tierce
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/video.m3u8"))
context.startActivity(intent)
```

**Résultat :**
```
┌─────────────────────────────────┐
│   Ouvrir avec                    │
├─────────────────────────────────┤
│  📱 STV Player                   │
│  📹 VLC                          │
│  🎬 MX Player                    │
│  🌐 Navigateur                   │
└─────────────────────────────────┘
```
→ **L'utilisateur choisit**

---

### SoukiTV (Utilisant Intent Explicite)

```kotlin
// Code dans SoukiTV
val intent = Intent()
intent.setClassName("com.example.stv", "com.example.stv.PlayerActivity")
intent.putExtra("VIDEO_URL", "https://example.com/video.m3u8")
context.startActivity(intent)
```

**Résultat :**
```
STV s'ouvre directement → Pas de choix → Lecture immédiate
```
→ **STV forcé, pas de chooser**

---

## ✅ AVANTAGES DE CETTE SOLUTION

| Feature | Apps Tierces | SoukiTV & Vos Apps |
|---------|--------------|---------------------|
| **Ouverture STV** | ✅ Oui (avec chooser) | ✅ Oui (direct) |
| **Choix utilisateur** | ✅ Peut choisir VLC/MX/etc. | ❌ STV forcé |
| **Deep Link** | ✅ `stv://play?url=...` | ✅ Fonctionne aussi |
| **Intent explicite** | ❌ Non (pas de setClassName) | ✅ Oui |
| **Sécurité** | ✅ Validation URL | ✅ Validation URL |

---

## 🚀 PLAN D'IMPLÉMENTATION

### Étape 1 : Modifier STV AndroidManifest.xml (5 min)
- Ajouter intent-filter Deep Link
- Ajouter intent-filter fichiers vidéo
- **Retirer** `android:permission="signature"`

### Étape 2 : Modifier STV PlayerActivity.kt (10 min)
- Gérer les 3 types d'Intent (extra, deep link, ACTION_VIEW)

### Étape 3 : Tester (15 min)
- Test 1 : Ouvrir depuis SoukiTV → STV direct
- Test 2 : Ouvrir via Deep Link → STV dans chooser
- Test 3 : Ouvrir .m3u8 depuis navigateur → STV dans chooser

### Étape 4 : Rebuild + Install (5 min)
```powershell
.\gradlew :app:assembleProdRelease
adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk
```

---

## 🔐 SÉCURITÉ

**Question :** Si on retire la permission, n'importe qui peut ouvrir STV ?

**Réponse :** Oui, **C'EST LE BUT** ! STV doit être comme VLC/MX Player (public).

**Protection :**
- Validation stricte des URLs dans `PlayerController`
- Whitelist de domaines (optionnelle)
- Vérification du `callingPackage` si besoin de logs analytics

---

## 📝 RÉSUMÉ

### Ce qui change
- ✅ STV devient **ouvert à tous** (comme VLC)
- ✅ SoukiTV **force STV** via Intent explicite (pas de chooser)

### Ce qui ne change PAS
- ✅ SoukiTV garde le contrôle (Intent explicite)
- ✅ Validation URL stricte
- ✅ Publicités AdMob

---

Voulez-vous que j'implémente ces modifications maintenant ?

