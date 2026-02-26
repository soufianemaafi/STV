# 🏗️ SCHÉMA D'ARCHITECTURE - Écosystème STV

**Date :** 26 Février 2026

---

## 📐 ARCHITECTURE CIBLE (Après Implémentation)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          GOOGLE PLAY STORE                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  📱 STV Player (com.example.stv)                                 │   │
│  │  ★★★★★ 4.5 | 10K+ téléchargements                               │   │
│  │  Lecteur vidéo universel pour streaming HLS/DASH/RTSP            │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  📺 SoukiTV (com.example.soukitv)                                │   │
│  │  ★★★★☆ 4.2 | 5K+ téléchargements                                │   │
│  │  Catalogue de chaînes IPTV - Nécessite STV Player                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  🎬 Future Catalog App 1                                          │   │
│  │  Compatible avec STV Player via Deep Link                         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 FLUX D'INTÉGRATION (User Journey)

### Scénario 1 : STV déjà installé

```
┌─────────────┐
│  Utilisateur│
│   ouvre     │
│  SoukiTV    │
└──────┬──────┘
       │
       ▼
┌────────────────────────────┐
│  SoukiTV HomeScreen        │
│  ┌──────────────────────┐  │
│  │ [NASA TV]            │  │
│  │ [France 24]          │◄─┼─ Click sur "NASA TV"
│  │ [Al Jazeera]         │  │
│  └──────────────────────┘  │
└──────┬─────────────────────┘
       │
       │ Intent(ACTION_VIEW, "stv://play?url=...")
       │
       ▼
┌────────────────────────────┐
│  Android Intent Resolver   │
│  "Ouvrir avec..."          │
│  ┌──────────────────────┐  │
│  │  ✅ STV Player       │◄─┼─ Unique option → ouvre automatiquement
│  └──────────────────────┘  │
└──────┬─────────────────────┘
       │
       ▼
┌────────────────────────────┐
│  STV Player - PlayerActivity│
│  ┌──────────────────────┐  │
│  │  Loading Ad...       │  │ ◄─ Interstitiel AdMob (10s max)
│  │  [●●●●●○○○○○]       │  │
│  └──────────────────────┘  │
└──────┬─────────────────────┘
       │
       │ Ad dismissed ou completed
       │
       ▼
┌────────────────────────────┐
│  STV Player - Video Playback│
│  ┌──────────────────────┐  │
│  │  🎬 NASA TV          │  │
│  │  ▶️ [En lecture]     │  │
│  │  ════════════        │  │ ◄─ Vidéo en cours (ExoPlayer)
│  │  00:05:23 / Live     │  │
│  │  [HD] [PiP] [<]      │  │
│  └──────────────────────┘  │
└────────────────────────────┘
```

---

### Scénario 2 : STV non installé

```
┌─────────────┐
│  Utilisateur│
│   ouvre     │
│  SoukiTV    │
└──────┬──────┘
       │
       ▼
┌────────────────────────────┐
│  SoukiTV HomeScreen        │
│  ┌──────────────────────┐  │
│  │ [NASA TV]            │  │
│  │ [France 24]          │◄─┼─ Click sur "NASA TV"
│  └──────────────────────┘  │
└──────┬─────────────────────┘
       │
       │ intent.resolveActivity() → null
       │
       ▼
┌────────────────────────────┐
│  SoukiTV - AlertDialog     │
│  ┌──────────────────────┐  │
│  │  STV Player requis   │  │
│  │                      │  │
│  │  Cette app nécessite│  │
│  │  STV Player pour    │  │
│  │  lire les chaînes.  │  │
│  │                      │  │
│  │  [Installer] [Annuler]│◄─┼─ User clique "Installer"
│  └──────────────────────┘  │
└──────┬─────────────────────┘
       │
       │ Intent(ACTION_VIEW, "market://details?id=com.example.stv")
       │
       ▼
┌────────────────────────────┐
│  Google Play Store         │
│  ┌──────────────────────┐  │
│  │  📱 STV Player       │  │
│  │  [Installer]         │◄─┼─ User installe STV
│  └──────────────────────┘  │
└──────┬─────────────────────┘
       │
       │ Installation terminée
       │
       ▼
┌────────────────────────────┐
│  Retour à SoukiTV          │
│  ┌──────────────────────┐  │
│  │ [NASA TV]            │  │
│  │ [France 24]          │◄─┼─ Re-click → fonctionne maintenant
│  └──────────────────────┘  │
└────────────────────────────┘
```

---

## 🔗 DEEP LINK ARCHITECTURE

### Format de Deep Link

```
Scheme:   stv://
Host:     play
Query:    url=<encoded_stream_url>

Exemple complet :
stv://play?url=https%3A%2F%2Fstream.example.com%2Fhls%2Fchannel1.m3u8
```

### Mapping dans AndroidManifest.xml

```xml
<activity android:name=".PlayerActivity">
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

### Parsing dans PlayerActivity.kt

```kotlin
val deepLinkUri: Uri? = intent.data
if (deepLinkUri != null && deepLinkUri.scheme == "stv" && deepLinkUri.host == "play") {
    val streamUrl = deepLinkUri.getQueryParameter("url")
    // Validation + démarrage vidéo
}
```

---

## 🛡️ SÉCURITÉ - Architecture en Couches

```
┌────────────────────────────────────────────────────────────┐
│  LAYER 1 : Validation URL (PlayerController)               │
│  ✓ Schéma http/https uniquement                            │
│  ✓ Longueur < 2048 chars                                   │
│  ✓ Format valide (Patterns.WEB_URL)                        │
│  ✓ Optionnel : Whitelist de domaines                       │
└────────┬───────────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────┐
│  LAYER 2 : Vérification Appelant (PermissionHelper)        │
│  ✓ Package name dans whitelist                             │
│    - com.example.soukitv                                    │
│    - com.example.stv (appels internes)                      │
│    - com.example.futurecatalog                              │
│  ✓ Optionnel : Vérification signature si signature-level   │
└────────┬───────────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────┐
│  LAYER 3 : Intent Filter (Android System)                  │
│  ✓ Seules les apps déclarant stv:// peuvent ouvrir STV     │
│  ✓ Protection contre injections via Intent extras          │
└────────┬───────────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────┐
│  LAYER 4 : Network Security (network_security_config.xml)  │
│  ✓ Force HTTPS pour domaines sensibles                     │
│  ✓ Certificate Pinning (optionnel)                         │
│  ✓ Autorise cleartext pour localhost (debug)               │
└────────────────────────────────────────────────────────────┘
```

---

## 💰 MONÉTISATION - Stratégie AdMob

### Placement Publicitaire

```
User Flow:
  ┌───────────────┐
  │  Click chaîne │
  │  dans SoukiTV │
  └───────┬───────┘
          │
          ▼
  ┌──────────────────────┐
  │  Ouverture STV       │
  │  (PlayerActivity)    │
  └───────┬──────────────┘
          │
          ▼
  ┌──────────────────────┐
  │  Chargement          │  ◄─ 10s max (timeout)
  │  Interstitiel AdMob  │
  └───────┬──────────────┘
          │
          ├─► Ad loaded + shown (90% cas)
          │   └─► User ferme ad → Vidéo démarre
          │
          ├─► Ad loaded but "No Fill" (8% cas)
          │   └─► Fallback Bannière → Vidéo démarre après 2s
          │
          └─► Ad failed 3x (2% cas = AdBlocker détecté)
              └─► Dialogue strict → Choix : "Réessayer" ou "Fermer"
```

### Types d'Ads Utilisés

| Type | ID (Test) | ID (Prod) | Placement | Fréquence |
|------|-----------|-----------|-----------|-----------|
| **Interstitiel** | ca-app-pub-3940256099942544/1033173712 | À configurer | Avant lecture | 1x par session vidéo |
| **Bannière** | ca-app-pub-3940256099942544/6300978111 | À configurer | Fallback si interstitiel échoue | Permanent en bas |

### Revenus Estimés (Hypothèse)

```
Scénario conservateur :
- 1000 utilisateurs actifs / jour
- 3 vidéos lues / utilisateur / jour
- eCPM interstitiel : 5€ (France/Europe)
- eCPM bannière : 0.50€
- Fill rate : 90%

Calcul :
  Interstitiels/jour = 1000 users × 3 vidéos × 90% fill = 2700 impressions
  Revenus interstitiels/jour = (2700 / 1000) × 5€ = 13.50€
  
  Bannières/jour (fallback 10%) = 300 impressions × 30s moyenne = 300 impressions
  Revenus bannières/jour = (300 / 1000) × 0.50€ = 0.15€
  
  TOTAL/jour : ~13.65€
  TOTAL/mois : ~410€
  TOTAL/an : ~4920€

Scénario optimiste (10K users actifs/jour) :
  → ~50K€/an (avant taxes)
```

---

## 📊 ANALYTICS - Événements à Tracker

### STV Player

| Événement | Paramètres | Usage |
|-----------|-----------|-------|
| `video_play_start` | `source_app`, `url_domain` | Mesurer origines du trafic |
| `video_play_duration` | `seconds_watched`, `quality` | Mesurer engagement |
| `ad_impression` | `ad_type`, `fill_status` | Optimiser revenus AdMob |
| `ad_click` | `ad_type` | CTR publicitaire |
| `ad_block_detected` | `attempts_failed` | Détecter taux AdBlock |
| `quality_change` | `from_quality`, `to_quality` | Optimiser buffer |
| `pip_enter` | `time_in_video` | Mesurer usage PiP |
| `error_playback` | `error_code`, `url_domain` | Détecter sources problématiques |

### SoukiTV

| Événement | Paramètres | Usage |
|-----------|-----------|-------|
| `channel_click` | `channel_id`, `category` | Mesurer popularité chaînes |
| `stv_installed_check` | `is_installed` | Taux conversion installation STV |
| `install_dialog_shown` | - | Taux d'affichage dialogue |
| `install_clicked` | - | Conversion dialogue → Play Store |
| `search_query` | `query_text` (optionnel) | Améliorer catalogue |

---

## 🔄 COMPATIBILITÉ - Matrix des Versions

### Versions Android Supportées

| Android Version | API Level | STV Support | SoukiTV Support | Notes |
|-----------------|-----------|-------------|-----------------|-------|
| Android 7.0 Nougat | 24 | ✅ Full | ✅ Full | MinSDK |
| Android 8.0 Oreo | 26 | ✅ Full | ✅ Full | PiP activé |
| Android 9.0 Pie | 28 | ✅ Full | ✅ Full | |
| Android 10 | 29 | ✅ Full | ✅ Full | |
| Android 11 | 30 | ✅ Full | ✅ Full | Package visibility requise |
| Android 12 | 31 | ✅ Full | ✅ Full | Splash Screen API |
| Android 13 | 33 | ✅ Full | ✅ Full | Notification permission |
| Android 14 | 34 | ✅ Full | ✅ Full | |
| Android 15 | 35 | ✅ Full | ✅ Full | TargetSDK |

### Formats Vidéo Supportés (ExoPlayer)

| Format | Extension | Container | Support STV |
|--------|-----------|-----------|-------------|
| **HLS** | .m3u8 | MPEG-TS | ✅ Excellent (ABR natif) |
| **DASH** | .mpd | MP4/WebM | ✅ Excellent (ABR natif) |
| **MP4** | .mp4 | MP4 | ✅ Full |
| **WebM** | .webm | WebM | ✅ Full |
| **RTSP** | rtsp:// | RTP | ✅ Partiel (pas de DRM) |
| **RTMP** | rtmp:// | FLV | ❌ Non supporté (deprecated) |

---

## 🌍 INTERNATIONALISATION (i18n)

### Langues Prioritaires pour Publication Play Store

| Langue | Code | Priorité | Pays cibles |
|--------|------|----------|-------------|
| Français | fr | 🔴 HIGH | France, Belgique, Suisse, Maroc, Algérie |
| Arabe | ar | 🔴 HIGH | MENA (SoukiTV focus) |
| Anglais | en | 🔴 HIGH | International |
| Espagnol | es | 🟡 MEDIUM | Espagne, Amérique Latine |
| Allemand | de | 🟡 MEDIUM | Allemagne, Autriche, Suisse |

### Fichiers de Traduction (Structure)

```
app/src/main/res/
├── values/           (Anglais - défaut)
│   └── strings.xml
├── values-fr/        (Français)
│   └── strings.xml
├── values-ar/        (Arabe)
│   └── strings.xml
├── values-es/        (Espagnol)
│   └── strings.xml
└── values-de/        (Allemand)
    └── strings.xml
```

---

## 📱 APP STORE OPTIMIZATION (ASO)

### Mots-Clés Principaux (STV Player)

**Français :**
- lecteur vidéo, streaming, IPTV, m3u8, HLS, player, média, gratuit, TV en direct

**Anglais :**
- video player, streaming, IPTV, m3u8, HLS, player, media, free, live TV

**Arabe :**
- مشغل فيديو, بث مباشر, قنوات, تلفزيون, مجاني

### Screenshot Strategy (8 screenshots)

1. **Home Screen** - Interface principale (test URL)
2. **Video Playing** - Lecture en cours (overlay contrôles visible)
3. **Quality Selection** - Menu sélection qualité (HD/SD)
4. **Picture-in-Picture** - Mode PiP actif (petite fenêtre)
5. **Video List** - Liste favoris (Room DB)
6. **Dark Theme** - Mode sombre (si implémenté)
7. **Error Handling** - Message d'erreur UX (optionnel)
8. **Ad Integration** - Bannière publicitaire (preuve monétisation)

---

## 🔐 PUBLICATION PLAY STORE - Checklist

### STV Player

#### Avant Soumission

- [ ] **App Signing** : Clé de signature sécurisée (pas celle de test)
- [ ] **Version** : versionCode incrémenté, versionName significatif (1.0.0)
- [ ] **IDs AdMob** : Test IDs remplacés par IDs production (flavor prod)
- [ ] **ProGuard** : Activé + testé en Release (pas de crash)
- [ ] **Permissions** : Minimales (INTERNET, ACCESS_NETWORK_STATE uniquement)
- [ ] **Content Rating** : Questionnaire rempli (probablement "PEGI 3" ou "Everyone")

#### Assets Graphiques

- [ ] **Icon** : 512x512 PNG (transparent interdit pour store listing)
- [ ] **Feature Graphic** : 1024x500 PNG (bannière promotionnelle)
- [ ] **Screenshots** : 2-8 images (téléphone ET tablette si supporté)
- [ ] **Vidéo démo** : 30s-2min (optionnelle mais recommandée)

#### Métadonnées

- [ ] **Titre** : Max 50 caractères (ex: "STV Player - Streaming Vidéo")
- [ ] **Description courte** : Max 80 caractères (visible dans résultats recherche)
- [ ] **Description longue** : Max 4000 caractères (détails, features)
- [ ] **Catégorie** : "Video Players & Editors"
- [ ] **Tags** : video, streaming, player, iptv, m3u8

#### Politique & Conformité

- [ ] **Privacy Policy** : URL publique (obligatoire si collecte données/ads)
- [ ] **AdMob Déclaration** : Mentionner utilisation ads dans description
- [ ] **Permissions Expliquées** : Justifier INTERNET dans description
- [ ] **Data Safety** : Formulaire rempli (types données collectées par AdMob)

---

### SoukiTV

#### Avant Soumission

- [ ] **Dépendance STV** : Mentionnée clairement dans description
- [ ] **Version** : versionCode/versionName cohérents
- [ ] **ProGuard** : Activé + testé
- [ ] **Contenu** : Catalogue initial rempli (minimum 10-15 chaînes)
- [ ] **Légalité** : Vérifier droits diffusion chaînes IPTV (⚠️ CRITIQUE)

#### Métadonnées

- [ ] **Titre** : "SoukiTV - Chaînes IPTV" (ou similaire)
- [ ] **Description** : Mentionner **obligatoirement** "Nécessite STV Player"
- [ ] **Catégorie** : "Entertainment" ou "Video Players & Editors"
- [ ] **Lien vers STV** : Dans description "Télécharger STV Player : [lien]"

#### Légalité & Conformité (⚠️ IMPORTANT)

- [ ] **Droits de Diffusion** : Autorisation écrite des diffuseurs
- [ ] **URLs Licites** : Uniquement chaînes publiques ou avec accord
- [ ] **Disclaimer** : "Nous ne sommes pas responsables du contenu des flux"
- [ ] **DMCA** : Procédure de retrait si plainte (contact email)

---

## 🚀 ROADMAP POST-LANCEMENT

### Phase 1 : MVP Stable (Mois 1-2)

```
Semaine 1-2 :  Implémentation Deep Link + fixes critiques
Semaine 3 :    Tests intensifs (beta interne)
Semaine 4 :    Préparation assets Play Store
Semaine 5 :    Soumission STV (beta fermée)
Semaine 6 :    Soumission SoukiTV (beta fermée)
Semaine 7-8 :  Itérations selon retours beta testers
```

### Phase 2 : Optimisations (Mois 3-4)

- **Analytics** : Firebase intégré, dashboards configurés
- **Crash Reporting** : Crashlytics + alertes
- **Performance** : Optimisation temps chargement < 2s
- **UX** : A/B testing formats publicités (interstitiel vs bannière)
- **Catalogue SoukiTV** : Dashboard admin pour ajouter chaînes dynamiquement

### Phase 3 : Scaling (Mois 5-6)

- **Multi-langues** : Traductions FR/EN/AR complètes
- **STV SDK** : Librairie publique pour développeurs tiers
- **Partenariats** : Collaboration avec autres catalogues IPTV
- **Monétisation++** : Abonnement Premium (sans pub, 2.99€/mois)
- **Cloud Sync** : Favoris synchronisés via Firebase (compte utilisateur)

---

## 🎯 MÉTRIQUES DE SUCCÈS (KPIs)

### STV Player

| Métrique | Objectif Mois 1 | Objectif Mois 6 | Mesure |
|----------|-----------------|-----------------|--------|
| **Installations** | 1000 | 10 000 | Play Store Console |
| **Utilisateurs actifs/jour** | 300 | 3000 | Firebase Analytics |
| **Taux de rétention J7** | 40% | 60% | Firebase Analytics |
| **Revenus AdMob/mois** | 50€ | 500€ | AdMob Dashboard |
| **Taux de crash** | < 2% | < 1% | Crashlytics |
| **Note moyenne** | 4.0⭐ | 4.5⭐ | Play Store |

### SoukiTV

| Métrique | Objectif Mois 1 | Objectif Mois 6 | Mesure |
|----------|-----------------|-----------------|--------|
| **Installations** | 500 | 5000 | Play Store Console |
| **Taux conversion STV** | 80% | 95% | Analytics (install_clicked / stv_not_installed) |
| **Chaînes regardées/user/jour** | 2 | 5 | Firebase Analytics |
| **Taux de rétention J7** | 50% | 70% | Firebase Analytics |

---

**Auteur :** GitHub Copilot  
**Version :** 1.0  
**Date :** 26 Février 2026

