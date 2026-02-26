# 🚨 PLAN D'IMPLÉMENTATION URGENT - STV & SoukiTV

**Date :** 26 Février 2026  
**Priorité :** CRITIQUE  
**Temps estimé :** 3-4 heures

---

## 🎯 OBJECTIF

**Faire fonctionner l'intégration SoukiTV → STV immédiatement**

Actuellement, SoukiTV **NE PEUT PAS** lancer STV à cause de :
1. ❌ Protection `signature-level` qui bloque SoukiTV
2. ❌ Absence de Deep Link (Intent explicite fragile)
3. ❌ Pas de redirection Play Store si STV absent

---

## 🔧 SOLUTION EN 3 ÉTAPES

### ✅ ÉTAPE 1 : Ajouter Deep Link à STV (30 min)

#### Fichier : `app/src/main/AndroidManifest.xml`

**AVANT :**
```xml
<activity
    android:name=".PlayerActivity"
    android:exported="true"
    android:permission="com.example.stv.PERMISSION_LAUNCH_PLAYER"
    ...>
</activity>
```

**APRÈS :**
```xml
<activity
    android:name=".PlayerActivity"
    android:exported="true"
    <!-- ⚠️ RETIRER android:permission temporairement -->
    android:launchMode="singleTask"
    android:excludeFromRecents="true"
    android:supportsPictureInPicture="true"
    android:theme="@style/Theme.STV">
    
    <!-- 🆕 AJOUTER Intent Filter pour Deep Link -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="stv"
            android:host="play" />
    </intent-filter>
</activity>
```

---

#### Fichier : `app/src/main/java/com/example/stv/PlayerActivity.kt`

**Modifier `onCreate()` ligne ~80 :**

**AVANT :**
```kotlin
val videoUrl = intent.getStringExtra("VIDEO_URL")
val skipAds = intent.getBooleanExtra("SKIP_ADS", false)
```

**APRÈS :**
```kotlin
// Supporter à la fois Intent classique ET Deep Link
val videoUrl = when {
    intent.hasExtra("VIDEO_URL") -> intent.getStringExtra("VIDEO_URL")
    intent.data != null -> {
        // Deep Link : stv://play?url=https://...
        val deepLinkUrl = intent.data?.getQueryParameter("url")
        Log.d(TAG, "Received deep link URL: $deepLinkUrl")
        deepLinkUrl
    }
    else -> null
}

val skipAds = intent.getBooleanExtra("SKIP_ADS", false)
```

---

### ✅ ÉTAPE 2 : Simplifier SoukiTV pour utiliser Deep Link (45 min)

#### Fichier : `soukitv/src/main/java/com/example/soukitv/ui/home/HomeScreen.kt`

**AVANT (lignes ~167-205) :**
```kotlin
onChannelClick = { channel ->
    val stvPackageNames = listOf(
        "com.example.stv",
        "com.example.stv.dev",
        "com.example.stv.prod"
    )
    
    val isInstalled = stvPackageNames.any { /* ... */ }
    
    if (isInstalled) {
        try {
            val actualPackageName = /* ... complexe ... */
            val intent = Intent()
            intent.setClassName(actualPackageName, "com.example.stv.PlayerActivity")
            intent.putExtra("VIDEO_URL", channel.streamUrl)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        showInstallDialog = true
    }
}
```

**APRÈS (REMPLACER TOUT LE BLOC) :**
```kotlin
onChannelClick = { channel ->
    // Validation de l'URL
    if (!isValidStreamUrl(channel.streamUrl)) {
        Toast.makeText(context, "URL de chaîne invalide", Toast.LENGTH_SHORT).show()
        return@onChannelClick
    }
    
    // Créer un Intent implicite avec Deep Link
    val deepLinkUri = Uri.parse("stv://play?url=${Uri.encode(channel.streamUrl)}")
    val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    
    // Vérifier si STV peut gérer cet intent
    val canHandleIntent = intent.resolveActivity(context.packageManager) != null
    
    if (canHandleIntent) {
        // STV est installé → lancer
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        // STV n'est pas installé → afficher dialogue
        showInstallDialog = true
    }
}
```

**AJOUTER cette fonction helper dans `HomeScreen.kt` (avant le @Composable) :**
```kotlin
private fun isValidStreamUrl(url: String): Boolean {
    if (url.isBlank()) return false
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false
    if (url.length > 2048) return false
    return android.util.Patterns.WEB_URL.matcher(url).matches()
}
```

---

#### AMÉLIORER le Dialogue d'Installation

**RECHERCHER dans `HomeScreen.kt` le bloc du dialogue (vers ligne 250) :**

```kotlin
if (showInstallDialog) {
    AlertDialog(
        onDismissRequest = { showInstallDialog = false },
        confirmButton = {
            TextButton(onClick = {
                // 🆕 AJOUTER LA LOGIQUE PLAY STORE ICI
                try {
                    val playStoreIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.example.stv")
                    )
                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(playStoreIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    // Fallback : ouvrir dans le navigateur si Play Store absent
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.example.stv")
                    )
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Impossible d'ouvrir le Play Store", Toast.LENGTH_SHORT).show()
                }
                showInstallDialog = false
            }) {
                Text("Installer STV Player")
            }
        },
        dismissButton = {
            TextButton(onClick = { showInstallDialog = false }) {
                Text("Annuler")
            }
        },
        title = { Text("STV Player requis") },
        text = { 
            Text("Cette application nécessite STV Player pour lire les chaînes. Voulez-vous l'installer ?") 
        }
    )
}
```

---

### ✅ ÉTAPE 3 : Tester l'Intégration (30 min)

#### Test 1 : Build et Install STV
```powershell
# Dans le terminal PowerShell
cd C:\Users\Lenovo\StudioProjects\STV4

# Build STV (flavor dev pour test)
.\gradlew :app:assembleDevDebug

# Installer sur device/emulator
.\gradlew :app:installDevDebug
```

#### Test 2 : Vérifier que le Deep Link fonctionne
```powershell
# Ouvrir une vidéo de test via adb (device connecté)
adb shell am start -a android.intent.action.VIEW -d "stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
```

**Résultat attendu :**
- ✅ STV s'ouvre
- ✅ Publicité interstitielle affichée (ou bannière si No Fill)
- ✅ Vidéo démarre après la pub

#### Test 3 : Build et Install SoukiTV
```powershell
# Build SoukiTV
.\gradlew :soukitv:assembleDebug

# Installer
.\gradlew :soukitv:installDebug
```

#### Test 4 : Tester le Flow Complet
1. Ouvrir **SoukiTV**
2. Cliquer sur une chaîne (ex: "NASA TV")
3. **Vérifier :**
   - ✅ STV s'ouvre automatiquement
   - ✅ Publicité affichée
   - ✅ Vidéo démarre

#### Test 5 : Tester le Dialogue Play Store
1. **Désinstaller STV** : `adb uninstall com.example.stv.dev`
2. Relancer **SoukiTV**
3. Cliquer sur une chaîne
4. **Vérifier :**
   - ✅ Dialogue "STV Player requis" apparaît
   - ✅ Bouton "Installer STV Player" fonctionne
   - ✅ Play Store s'ouvre (ou message d'erreur si pas publié)

---

## 🐛 TROUBLESHOOTING

### Problème 1 : "ActivityNotFoundException: No Activity found to handle Intent"

**Cause :** Deep Link non reconnu

**Solution :**
1. Vérifier que l'Intent Filter est bien dans `AndroidManifest.xml`
2. Rebuild STV : `.\gradlew :app:clean :app:assembleDevDebug`
3. Réinstaller : `.\gradlew :app:installDevDebug`

---

### Problème 2 : STV se ferme immédiatement après ouverture

**Cause :** URL invalide ou erreur dans `PlayerActivity.onCreate()`

**Solution :**
1. Vérifier les logs : `adb logcat | Select-String "PlayerActivity"`
2. Vérifier que l'URL du Deep Link est bien récupérée :
   ```kotlin
   Log.d(TAG, "Received deep link URL: $deepLinkUrl")
   ```

---

### Problème 3 : "Unauthorized caller. Finishing activity."

**Cause :** Le code de vérification des permissions n'a pas été désactivé

**Solution :**
Commenter temporairement le bloc de sécurité dans `PlayerActivity.kt` (ligne ~65) :

```kotlin
// ⚠️ COMMENTÉ POUR PERMETTRE L'ACCÈS DEPUIS SOUKITV
/*
val permissionHelper = PermissionHelper(this)
val callerPackage = callingPackage

if (callerPackage != null && !permissionHelper.isCallerAuthorized(callerPackage, requiredPermission)) {
    Log.e(TAG, "Unauthorized caller. Finishing activity.")
    finish()
    return
}
*/
```

**Note :** Réimplémenter la sécurité plus tard avec une whitelist.

---

### Problème 4 : Play Store ne s'ouvre pas depuis SoukiTV

**Cause :** Package name incorrect ou Play Store non installé (émulateur)

**Solution temporaire :**
Le fallback navigateur devrait s'ouvrir automatiquement. Si ça ne marche pas, vérifier que le package name est bien `"com.example.stv"` (et non `.dev` ou `.prod`).

---

## 📋 CHECKLIST FINALE

Avant de passer à autre chose, vérifier que :

- [ ] STV peut être lancé via `stv://play?url=...` (test adb)
- [ ] SoukiTV détecte correctement STV installé/non-installé
- [ ] Cliquer sur une chaîne dans SoukiTV ouvre STV
- [ ] Si STV absent, le dialogue Play Store s'affiche
- [ ] Les publicités AdMob fonctionnent dans STV
- [ ] Aucun crash dans les logs (`adb logcat`)

---

## 🚀 PROCHAINES ÉTAPES (Après ce Fix)

Une fois l'intégration fonctionnelle :

1. **Réimplémenter la sécurité** (whitelist de packages autorisés)
2. **Préparer les assets Play Store** (icônes, screenshots, description)
3. **Remplacer les IDs AdMob Test** par les vrais (flavor prod)
4. **Tester en Release** (`.\gradlew :app:assembleProdRelease`)
5. **Publier STV en Beta** sur Play Store (Internal Testing)

---

## 📞 BESOIN D'AIDE ?

Si blocage, fournir :
1. Logs complets : `adb logcat > logs.txt`
2. Code modifié (fichiers AndroidManifest.xml, PlayerActivity.kt, HomeScreen.kt)
3. Message d'erreur exact

---

**Auteur :** GitHub Copilot  
**Version :** 1.0  
**Date :** 26 Février 2026

