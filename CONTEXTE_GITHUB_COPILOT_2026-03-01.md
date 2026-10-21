# 🌍 CONTEXTE DU PROJET : STV ECOSYSTEM (Monorepo)

**Date de mise à jour :** Octobre 2026  
**Statut du projet :** ✅ **PRÊT POUR LA PRODUCTION (V2.9)**  
**Architecture globale :** Monorepo Android structuré en 2 modules applicatifs isolés (`:app` et `:soukitv`).

---

## 1. MODULE `:app` (STV Player) - L'Application Officielle
**Identifiant Play Store :** `com.soufcom.stv`
**Rôle :** Lecteur vidéo universel et neutre (White-label), distribué sur le Google Play Store.

### Spécifications Techniques :
* **Moteur Vidéo :** Media3 (ExoPlayer). 
  * Gestion du cycle de vie sans fuite mémoire (Player encapsulé dans le ViewModel avec `ApplicationContext`).
  * Buffer ultra-rapide (800ms Fast-Startup).
  * Distinction intelligente entre *Pure Live* (Seekbar masquée) et *Live DVR / VOD*.
  * Tolérance réseau (Cross-Protocol Redirects) et décodeur de secours logiciel.
* **Monétisation (AdMob) :**
  * Format : Interstitiel Pre-roll plein écran.
  * Logique de fréquence : `Cooldown = 0s` (La fréquence est gérée à distance via le tableau de bord AdMob).
  * UX / Tolérance : Patience de `10 secondes` au démarrage. Si le réseau est lent, la pub est ignorée (skip) et la vidéo démarre sans bloquer l'utilisateur. L'écran de blocage *AdBlocker* ne s'affiche que sur erreur réseau stricte.
* **Sécurité & Légal :**
  * `IntentSecurityManager` avec whitelist stricte (HTTP/HTTPS uniquement).
  * Disclaimer légal inclus dans le menu (Drawer).
  * URLs de Privacy Policy et Terms of Service officielles liées à Google Sites.
* **Tests :** Couverture validée par 174 tests unitaires (100% Success).

---

## 2. MODULE `:soukitv` (SoukiTV) - Le Catalogue Front-End
**Identifiant :** `com.example.soukitv` (Distribué hors Play Store)
**Rôle :** Interface utilisateur riche permettant de naviguer dans les chaînes et les matchs, et d'envoyer l'ordre de lecture à STV Player.

### Spécifications Techniques :
* **UI/UX (Jetpack Compose Material 3) :** 
  * Thème dynamique (Dark/Light mode avec toggle animé ☀️/🌙).
  * BottomNavigationBar épurée (Matchs / Chaînes TV).
  * Navigation Hiérarchique (Drill-down à 3 niveaux : Catégories ➔ Pays ➔ Chaînes).
* **Gestion des Données (Remote Driven) :**
  * Piloté à distance via Retrofit (fichiers `channels.json` et `matches.json` hébergés sur GitHub).
  * Fallback hors-ligne immédiat en cas d'absence de réseau.
  * Modèles protégés pour ProGuard (`@SerializedName`).
* **Traitement d'Image (Coil) :** 
  * Utilisation d'une transformation sur-mesure `AutoCropTransparentTransformation` pour harmoniser automatiquement la taille de tous les logos téléchargés.

---

## 3. LE CONTRAT DE COMMUNICATION (IPC)
Les deux modules sont indépendants mais communiquent via un contrat d'Intent strict :

* **Action :** `com.soufcom.stv.action.PLAY_STREAM`
* **Sécurité :** L'émetteur (`:soukitv`) verrouille la cible via `setPackage("com.soufcom.stv")`. Le récepteur (`:app`) isole l'exécution via `taskAffinity=""` pour un retour physique (Back button) fluide.
* **Payload :** URL du flux (`video/*`), Titre de la chaîne (`"title"`), et Headers HTTP d'authentification optionnels (`"headers"`).

---

## 4. RÈGLES DE DÉVELOPPEMENT (Pour l'IA et les Devs)
1. **Architecture :** MVI (Model-View-Intent) strict. L'UI observe `StateFlow` et émet des `UiAction`. Aucune logique métier dans les Composables.
2. **Asynchronisme :** Tout appel réseau ou base de données doit s'exécuter sur `Dispatchers.IO`.
3. **Tests :** Toute modification de logique doit être couverte par des tests unitaires exécutables sur la JVM locale.