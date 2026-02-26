# Stratégie Apps Catalogue + STV Player

## Architecture adoptée
- **STV Player** : app unique publiée sur Play Store, utilisable par toutes les apps
- **Apps catalogue** (SoukiTV, futures apps) : peuvent être hors Play Store, forcent l'usage de STV

## Principe de protection des revenus

### ✅ Ce qui est protégé
- **Les apps catalogue forcent STV exclusivement** via intent explicite `setPackage("com.example.stv")`
- L'utilisateur **ne peut pas choisir** VLC/MXPlayer depuis les apps catalogue
- Si STV n'est pas installée, un dialog force l'installation

### ✅ Ce qui est ouvert
- **Toute app tierce peut utiliser STV** (via `com.example.stv.action.PLAY_STREAM`)
- Bénéfice : plus d'utilisations = plus de pubs affichées = plus de revenus

## Modifications appliquées (2026-02-25)

### 1. Suppression de la permission signature
**Fichier** : `app/src/main/AndroidManifest.xml`
- ✅ Retiré `android:permission="com.example.stv.PERMISSION_LAUNCH_PLAYER"` sur `PlayerActivity`
- ✅ Retiré la déclaration `<permission>` signature
- ✅ Retiré `<uses-permission>` associée

**Raison** : permettre à toutes les apps d'utiliser STV (augmentation revenus pub)

### 2. Suppression du contrôle de signature côté code
**Fichier** : `app/src/main/java/com/example/stv/PlayerActivity.kt`
- ✅ Retiré la vérification `PermissionHelper.isCallerAuthorized`

**Raison** : cohérence avec la suppression de la permission manifest

### 3. Nettoyage SoukiTV
**Fichier** : `soukitv/src/main/AndroidManifest.xml`
- ✅ Retiré `<uses-permission android:name="com.example.stv.PERMISSION_LAUNCH_PLAYER" />`
- ✅ Conservé `<queries>` pour détecter l'installation de STV

**Raison** : permission obsolète

### 4. Intent explicite côté catalogue (déjà en place)
**Fichier** : `soukitv/src/main/java/com/example/soukitv/ui/home/HomeScreen.kt`
```kotlin
val intent = Intent("com.example.stv.action.PLAY_STREAM").apply {
    setPackage(actualPackageName) // Force STV uniquement
    putExtra("VIDEO_URL", channel.streamUrl)
}
```
- ✅ Garantit que **seul STV** sera lancé (pas de sélecteur de player)

## Garanties pour les futures apps catalogue

### Pour conserver les revenus publicitaires
1. **Toujours utiliser `setPackage("com.example.stv")`** dans l'intent
2. **Ne jamais utiliser** `ACTION_VIEW` générique (sinon l'utilisateur peut choisir VLC)
3. **Vérifier l'installation** de STV avant de lancer l'intent
4. **Afficher un dialog d'installation** si STV n'est pas présente

### Contrat d'API stable (à respecter dans STV)
- **Action** : `com.example.stv.action.PLAY_STREAM` (ne jamais changer)
- **Extra** : `VIDEO_URL` (String, ne jamais renommer)
- **Package** : `com.example.stv` (ne jamais changer l'applicationId)
- **Intent-filter** : toujours présent sur `PlayerActivity`

## Signature des futures apps catalogue
- ❌ **Aucune contrainte de signature** (plus de permission signature)
- ✅ Les apps peuvent être signées avec n'importe quelle clé
- ✅ Les apps peuvent être distribuées hors Play Store (APK direct)

## Avantages de cette architecture
- ✅ STV peut être utilisée par des apps tierces (revenus supplémentaires)
- ✅ Tes apps catalogue forcent STV (pas de perte de revenus)
- ✅ Pas de contrainte de signature entre les apps
- ✅ Distribution flexible des apps catalogue (Play Store ou hors store)

## Risques résiduels
- ⚠️ Une app malveillante peut abuser de STV (spam de streams)
- ⚠️ Mais elle génère quand même des impressions pub (= revenus)
- 💡 Si abus constaté, possibilité d'ajouter un rate-limiting ou token côté serveur

## Checklist pour les nouvelles apps catalogue
- [ ] Ajouter `<queries><package android:name="com.example.stv" /></queries>` dans le manifest
- [ ] Utiliser `Intent("com.example.stv.action.PLAY_STREAM")` + `setPackage("com.example.stv")`
- [ ] Vérifier l'installation via `packageManager.getPackageInfo("com.example.stv", 0)`
- [ ] Afficher un dialog avec lien Play Store si STV n'est pas installée
- [ ] Ne jamais utiliser `ACTION_VIEW` générique pour les streams

## Exemple de code à réutiliser (template)
```kotlin
// Détection installation STV
val isStvInstalled = try {
    context.packageManager.getPackageInfo("com.example.stv", 0)
    true
} catch (e: Exception) {
    false
}

// Lancement STV
if (isStvInstalled) {
    val intent = Intent("com.example.stv.action.PLAY_STREAM").apply {
        setPackage("com.example.stv")
        putExtra("VIDEO_URL", streamUrl)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
} else {
    // Afficher dialog installation Play Store
}
```

