# 📘 GUIDE PRATIQUE - Utiliser STV et Faire des Demandes

**Date** : 26/02/2026  
**Format** : Pratique et opérationnel  
**Public** : Utilisateurs et développeurs

---

## 🎯 SECTION 1 : COMPRENDRE LE FONCTIONNEMENT

### 1.1 Ouvrir STV - 3 façons différentes

#### Façon 1 : Via Android Studio (Dev)
```bash
# Ouvrir le projet
Android Studio > File > Open > C:\Users\soufi\AndroidStudioProjects\STV5

# Builder l'app
Build > Build Bundle(s) / APK(s) > Build APK(s)

# Lancer sur device/émulateur
Run > Run 'app'
```

#### Façon 2 : Via ligne de commande
```powershell
# Naviguer au dossier
cd C:\Users\soufi\AndroidStudioProjects\STV5

# Builder APK dev
.\gradlew.bat assembleDevDebug

# Builder APK prod
.\gradlew.bat assembleProvDebug

# APK généré à
app\build\outputs\apk\dev\debug\app-dev-debug.apk
app\build\outputs\apk\prod\debug\app-prod-debug.apk
```

#### Façon 3 : Depuis une autre app (SoukiTV)
```kotlin
// SoukiTV appelle STV
Intent("com.example.stv.action.PLAY_STREAM").apply {
    setPackage("com.example.stv")  // Force STV
    putExtra("VIDEO_URL", "https://stream.example.com/hls.m3u8")
    startActivity(this@YourActivity)
}
```

---

### 1.2 Interface Utilisateur

#### Écran Principal (MainActivity)
```
┌─────────────────────────────────────┐
│ ☰  STV             [Menu button]    │
├─────────────────────────────────────┤
│                                     │
│  📹 Choisir une vidéo              │
│  ┌─────────────────────────────────┐│
│  │ • Sky News Arabia               ││  ← Sélectionner une vidéo
│  │ • France Télévisions            ││
│  │ • BBC World                     ││
│  └─────────────────────────────────┘│
│                                     │
│  OU                                 │
│                                     │
│  🔗 URL personnalisée              │
│  ┌─────────────────────────────────┐│
│  │ https://example.com/stream.m3u8 ││  ← Saisir URL
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │    [LIRE LA VIDÉO]              ││  ← Clique ici
│  └─────────────────────────────────┘│
│                                     │
│ 🌐 Internet: Connecté ✓            │
└─────────────────────────────────────┘

Menu latéral (☰) :
├─ 📋 Politique de Confidentialité
├─ ⚖️ Conditions d'Utilisation
└─ [Autres éléments futurs]
```

#### Écran du Lecteur (PlayerActivity)
```
┌─────────────────────────────────────┐
│ ◀ Titre de la vidéo                 │ ← Header
├─────────────────────────────────────┤
│                                     │
│                                     │
│       ★ VIDÉO JOUÉE ICI ★           │
│       (Plein écran)                 │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  ▶ ⏸  [████░░░░░] 2:30/5:00        │ ← Contrôles
│     🔊 [████░] 75%                  │ ← Volume
│     📺 720p ▼  CC ▼  ⠿ PiP         │ ← Qualité/Sous-titres
├─────────────────────────────────────┤
│ [Publicité AdMob peut s'afficher]   │
└─────────────────────────────────────┘

Auto-hide: Contrôles disparaissent après 3s d'inactivité
Cliquez sur vidéo pour les montrer
```

---

### 1.3 Flux de lecture détaillé

**Étape 1 : Saisie de l'URL**
```
Utilisateur clique sur le champ
├─ Clavier apparaît
├─ Peut copier/coller une URL
└─ Validation en temps réel
   └─ URL invalide → Message rouge
   └─ URL valide → Couleur normale
```

**Étape 2 : Validation**
```
Utilisateur clique "LIRE"
├─ MainViewModel valide l'URL
│  ├─ ✓ Non vide
│  ├─ ✓ Commence par http:// ou https://
│  ├─ ✓ Format URL correct
│  └─ ✓ Moins de 2048 caractères
│
├─ Si ✗ ERREUR → Dialog "URL invalide"
│  └─ Utilisateur doit corriger
│
└─ Si ✓ OK → Lance PlayerActivity
```

**Étape 3 : Chargement du lecteur**
```
PlayerActivity s'affiche
├─ Montre spinner "Chargement..."
├─ Initialise ExoPlayer
├─ Charge la vidéo (buffering)
└─ Attend 3+ secondes généralement
```

**Étape 4 : Publicité**
```
Avant que la vidéo ne joue
├─ AdMob charge une pub (6 secondes max)
│  ├─ Pub chargée ? → L'affiche
│  ├─ Timeout ? → Lance la vidéo quand même
│  └─ Erreur ? → Lance bannière + vidéo
│
└─ Utilisateur voit pub ou commence vidéo
```

**Étape 5 : Lecture**
```
Lecteur en plein écran
├─ Vidéo joue automatiquement
├─ Contrôles disponibles
│  ├─ Play/Pause
│  ├─ Barre de progression (seek)
│  ├─ Volume
│  ├─ Qualité (si multi-bitrate)
│  ├─ Sous-titres (si disponibles)
│  └─ Picture-in-Picture
│
└─ Utilisateur interagit librement
```

---

## 🔧 SECTION 2 : ARCHITECTURE TECHNIQUE

### 2.1 Composants Principaux

| Composant | Rôle | Fichier |
|-----------|------|---------|
| **MainActivity** | Écran d'accueil | MainActivity.kt |
| **PlayerActivity** | Lecteur vidéo | PlayerActivity.kt |
| **AdManager** | Gestion annonces AdMob | AdManager.kt |
| **PlayerViewModel** | État du lecteur | PlayerViewModel.kt |
| **PlayerController** | Validation URL | player/PlayerController.kt |
| **AdsController** | Logique ads | ads/AdsController.kt |
| **PermissionHelper** | Sécurité des activités | security/PermissionHelper.kt |

### 2.2 Fichiers de Configuration

| Fichier | Rôle |
|---------|------|
| **AndroidManifest.xml** | Déclaration activités, permissions, intent filters |
| **build.gradle.kts** | Dépendances, flavors (dev/prod), signing |
| **libs.versions.toml** | Versions des libraires |
| **proguard-rules.pro** | Règles de minification |
| **release.keystore** | Clé de signature release |

### 2.3 Structure des répertoires

```
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/stv/
│   │   │   ├── MainActivity.kt
│   │   │   ├── PlayerActivity.kt
│   │   │   ├── AdManager.kt
│   │   │   ├── MainViewModel.kt
│   │   │   ├── PlayerViewModel.kt
│   │   │   ├── VideoItem.kt
│   │   │   ├── VideoListActivity.kt
│   │   │   ├── AddVideoActivity.kt
│   │   │   ├── PrivacyPolicyActivity.kt
│   │   │   ├── TermsOfServiceActivity.kt
│   │   │   │
│   │   │   ├── ads/
│   │   │   │   └── AdsController.kt
│   │   │   │
│   │   │   ├── player/
│   │   │   │   └── PlayerController.kt
│   │   │   │
│   │   │   ├── security/
│   │   │   │   └── PermissionHelper.kt
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── PlayerUiState.kt
│   │   │       └── theme/
│   │   │           └── Color.kt, ...
│   │   │
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   └── strings.xml
│   │   │   ├── drawable/
│   │   │   ├── mipmap/
│   │   │   │   └── ic_launcher (icône app)
│   │   │   └── xml/
│   │   │       └── network_security_config.xml
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── test/ & androidTest/
│       └── Tests unitaires et d'instrumentation
│
├── build.gradle.kts
├── proguard-rules.pro
└── release.keystore

gradle/
└── libs.versions.toml

build.gradle.kts (root)
settings.gradle.kts
```

---

## 📋 SECTION 3 : COMMANDES UTILES

### 3.1 Construire l'APK

```powershell
# 1. Build APK Debug (dev)
.\gradlew.bat assembleDevDebug

# 2. Build APK Debug (prod)
.\gradlew.bat assembleProdDebug

# 3. Build APK Release (prod)
.\gradlew.bat assembleProdRelease

# 4. Build tous les flavors
.\gradlew.bat assemble

# 5. Build et lance sur device
.\gradlew.bat installDevDebug

# 6. Build et tests
.\gradlew.bat testDevDebug
```

**Sortie** :
```
app\build\outputs\apk\dev\debug\app-dev-debug.apk
app\build\outputs\apk\prod\debug\app-prod-debug.apk
app\build\outputs\apk\prod\release\app-prod-release.apk
```

### 3.2 Nettoyer le build

```powershell
# Nettoyer tout
.\gradlew.bat clean

# Nettoyer et rebuilder
.\gradlew.bat clean assembleDevDebug

# Forcer rechargement des dépendances
.\gradlew.bat build --refresh-dependencies
```

### 3.3 Logs et debugging

```powershell
# Voir les logs device
adb logcat

# Filtrer par tag
adb logcat PlayerActivity

# Effacer les logs
adb logcat -c

# Sauvegarder logs dans un fichier
adb logcat > logs.txt
```

### 3.4 Synchroniser Gradle

```powershell
# Dans Android Studio ou terminal
.\gradlew.bat sync

# Ou via Android Studio UI
File > Sync Now
```

---

## 🎬 SECTION 4 : CAS D'UTILISATION TYPES

### Cas 1 : Ajouter une nouvelle vidéo à la liste

**Localisation** : MainViewModel.kt, MainActivity.kt

**Actuel** :
```kotlin
private val _streamUrl = MutableStateFlow("https://stream.skynewsarabia.com/hls/sna_720.m3u8")
```

**Pour ajouter une vidéo** :
```kotlin
// Ajouter une liste de vidéos pré-définies
data class VideoList(
    val videos: List<VideoItem>
)

// Dans MainActivity, afficher la liste
LazyColumn {
    items(videosList) { video ->
        VideoItemUI(video) {
            // Clique sur vidéo → Lance PlayerActivity
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("VIDEO_URL", video.url)
            }
            startActivity(intent)
        }
    }
}
```

---

### Cas 2 : Modifier les IDs AdMob

**Localisation** : app/build.gradle.kts

**Actuel** (Test) :
```kotlin
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
    "\"ca-app-pub-3940256099942544/1033173712\"")
```

**Pour utiliser vos IDs** :
```kotlin
// 1. Obtenir vos IDs depuis Google AdMob console
// 2. Remplacer les IDs dans le fichier

create("prod") {
    dimension = "environment"
    // ✅ À remplacer par vos vrais IDs
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", 
        "\"ca-app-pub-YOUR_ID/YOUR_UNIT_ID\"")
    buildConfigField("String", "ADMOB_BANNER_ID", 
        "\"ca-app-pub-YOUR_ID/YOUR_BANNER_ID\"")
}

// 3. Reconstruire
.\gradlew.bat clean assembleProdRelease
```

---

### Cas 3 : Changer les couleurs de l'application

**Localisation** : app/src/main/java/com/example/stv/ui/theme/Color.kt

**Actuel** :
```kotlin
val primaryColor = Color(0xFF...)
val secondaryColor = Color(0xFF...)
```

**Pour changer** :
```kotlin
// 1. Ouvrir le fichier Color.kt
// 2. Modifier les valeurs hex
val primaryColor = Color(0xFF1976D2)  // Bleu
val secondaryColor = Color(0xFFFFC107)  // Ambre

// 3. Material Design 3 auto-applique partout
// (Boutons, TopBar, Text, etc.)

// 4. Recompiler
.\gradlew.bat assembleDevDebug
```

---

### Cas 4 : Ajouter un nouveau menu / Feature

**Localisation** : MainActivity.kt, PlayerActivity.kt

**Exemple : Ajouter un bouton "Historique"** :

1. **Dans MainScreen()** :
```kotlin
NavigationDrawerItem(
    label = { Text("Historique") },
    selected = false,
    onClick = {
        // Action
        navigateToHistoryScreen()
    }
)
```

2. **Créer l'écran** :
```kotlin
@Composable
fun HistoryScreen() {
    // Afficher liste vidéos visionnées
    // Avec dates, durées, etc.
}
```

3. **Ajouter la navigation** :
```kotlin
Navigation {
    composable("main") { MainScreen() }
    composable("history") { HistoryScreen() }
}
```

---

### Cas 5 : Intégrer avec SoukiTV

**Dans SoukiTV** :
```kotlin
// 1. Créer l'intent
val intent = Intent("com.example.stv.action.PLAY_STREAM").apply {
    setPackage("com.example.stv")  // Force STV
    putExtra("VIDEO_URL", videoUrl)
}

// 2. Vérifier si STV est installée
if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    // STV pas installée
    Toast.makeText(this, "STV Player non installé", Toast.LENGTH_SHORT).show()
}
```

**Dans STV** (PlayerActivity) :
```kotlin
// VIDEO_URL est automatiquement reçue
val videoUrl = resolveVideoUrl(intent)  // Récupère l'URL
// Le lecteur lance avec cette URL
```

---

## 💡 SECTION 5 : BONNES PRATIQUES

### 5.1 Avant de faire une demande

✅ **BIEN** :
```
DEMANDE: Ajouter support des sous-titres personnalisés

DÉTAILS:
- Formats: SRT, WebVTT
- Permettre upload utilisateur
- Sauvegarde de la préférence (langue)

PRIORITÉ: Moyenne
DEADLINE: 1 semaine
```

❌ **MAUVAIS** :
```
DEMANDE: Changer des trucs
(Trop vague, pas d'info)
```

### 5.2 Structure d'une bonne demande

```
TITRE: [Clair et court]
DESCRIPTION: [Contexte, pourquoi c'est important]
SPÉCIFICATIONS: [Details techniques]
FICHIERS CONCERNÉS: [Quels fichiers modifier]
PRIORITÉ: [Basse/Moyenne/Haute]
DEADLINE: [Date si applicable]
ACCEPTANCE CRITERIA: [Comment on valide que c'est fait]
```

### 5.3 Dépannage courant

| Problème | Solution |
|----------|----------|
| **APK ne compile** | `.\gradlew.bat clean && .\gradlew.bat assembleDevDebug` |
| **App crash au démarrage** | Vérifier les logs avec `adb logcat` |
| **AdMob ne charge pas** | Vérifier les IDs dans BuildConfig |
| **Vidéo ne joue pas** | Vérifier URL valide, connexion internet |
| **Gradle timeout** | Augmenter timeout dans gradle.properties |
| **Erreur de signing** | Vérifier release.keystore existe |

---

## 🚀 SECTION 6 : PROCHAINES DEMANDES

**Je suis prêt pour vos demandes ! Voici ce que vous pouvez me demander :**

### Features possibles
- ✅ Historique de lecture
- ✅ Favoris/Signets
- ✅ Playlist personnalisée
- ✅ Reconnaissance gestuelle (swipe = seek)
- ✅ Mode sombre/clair complet
- ✅ Multi-langue (i18n)
- ✅ Statistiques de lecture
- ✅ Partage de vidéos

### Optimisations possibles
- ✅ Cache intelligent HLS/DASH
- ✅ Adaptation réseau (bitrate)
- ✅ Préchargement (prefetch)
- ✅ Offline playback
- ✅ Réduction taille APK

### Intégrations possibles
- ✅ SoukiTV (déjà supporté)
- ✅ Plus de catalogues d'apps
- ✅ Analytics (Firebase)
- ✅ Crash reporting (Crashlytics)
- ✅ Authentication (login utilisateur)

### Configuration possibles
- ✅ AdMob production IDs
- ✅ Thème personnalisé
- ✅ Serveurs personnalisés
- ✅ Contenu réstrictif (parental control)
- ✅ Branding personnalisé

---

**DOCUMENT PRATIQUE COMPLET ✅**

**Vous avez maintenant tout ce qui est nécessaire pour:**
1. Comprendre STV complètement
2. Utiliser l'app
3. Faire des demandes claires
4. Dépanner les problèmes

**Prêt pour vos demandes ! 🎯**

