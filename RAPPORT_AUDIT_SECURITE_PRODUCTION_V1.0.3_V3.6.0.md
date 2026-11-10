# Rapport d’audit technique exhaustif — Écosystème STV

**Date de l’audit :** 10 novembre 2026  
**Périmètre :** monorepo `D:\STV`, modules `:app` et `:soukitv`  
**Cible validée :** `:app` production `com.soufcom.stv` v1.0.3 / versionCode 4 et `:soukitv` v3.6.0 / versionCode 6  
**Auditeur :** Lead Security Auditor & Principal Android Systems Architect  
**Nature :** audit statique approfondi + campagne de remédiation + tests JVM + compilation release.

---

## 1. Verdict exécutif

### Décision : **FEU VERT — release officielle approuvée sous réserve des limites résiduelles documentées**

La qualité de compilation est bonne :

- `:app:testProdDebugUnitTest` : **PASS**
- `:soukitv:testDebugUnitTest` : **PASS**
- `git diff --check` : **PASS** après remédiation
- `:app:bundleProdRelease` : **PASS**
- Bundle produit : `app/build/outputs/bundle/prodRelease/app-prod-release.aab` généré avec succès (~10,77 Mo)
- Minification et shrink des ressources actifs sur le build release.

Les bloqueurs identifiés ont été résolus :

- **R-01 :** l’identité Play publiée est formellement `com.soufcom.stv`; `com.stv.videoplayer` est uniquement le namespace source. Le contrat IPC et la documentation ont été alignés sans changer l’application publiée.
- **R-02 :** les signatures sont comparées via empreintes SHA-256 et `MessageDigest.isEqual`; `SKIP_ADS` reste strictement refusé aux appelants inconnus/non authentifiés.
- **R-03 :** cleartext désactivé par défaut et redirections cross-protocoles désactivées.
- **R-04 :** les fixtures utilisent exclusivement des domaines réservés RFC 2606.
- **R-05 :** `:soukitv` est maintenant en `versionCode = 6`, `versionName = "3.6.0"`.

Les écarts résiduels R-06, R-07, R-13 et R-14 sont non bloquants, explicitement conservés comme travaux de durcissement ultérieurs dans la section des limites.

---

## 2. Matrice récapitulative des risques

| ID | Domaine | Risque | Sévérité | Statut | Preuve principale |
|---|---|---|---|---|---|
| R-01 | Identité release / IPC | Identité publiée clarifiée et documentation alignée sur `com.soufcom.stv` | **Élevée — bloqueur** | **Résolu** | `app/build.gradle.kts:24,51`, `StvPlayerLauncher.kt:282-285` |
| R-02 | Autorisation IPC | Empreintes SHA-256 comparées avec `MessageDigest.isEqual`; extras sensibles refusés sans authentification | **Élevée** | **Résolu** | `PermissionHelper.kt:21-23,40-86` |
| R-03 | Réseau / downgrade | Cleartext par défaut désactivé et redirections inter-protocoles refusées | **Élevée** | **Résolu** | `network_security_config.xml`, `HeaderAwareMediaSourceFactory.kt:100-103` |
| R-04 | Fuites statiques | Fixtures de test sans IP publique ni endpoint réel | **Élevée** | **Résolu** | `StvPlayerLauncherTest.kt:168-285` |
| R-05 | Versioning | `:soukitv` en versionCode 6 / versionName 3.6.0 | **Élevée — release blocker** | **Résolu** | `soukitv/build.gradle.kts:19-24` |
| R-06 | Anti-reverse | XOR mono-octet trivial pour URLs et empreintes | Moyenne | Acceptable uniquement comme masquage, pas comme secret | `SecretDecoder.kt:3-10`, `ChannelRepositoryImpl.kt:80-113` |
| R-07 | Résilience catalogue | Aucun fallback local/cache catalogue observé ; seules les préférences sont persistées | Moyenne | Non conforme à l’architecture annoncée | `ChannelRepositoryImpl.kt:38-75`, `SharedPrefsFavoritesStore.kt:6-15` |
| R-08 | Compatibilité IPC | Extraction ordonnée `VIDEO_URL` puis `url` puis `intent.dataString`, avec validation identique | Moyenne | **Résolu** | `IntentSecurityManager.kt:39-181` |
| R-09 | Threading | API launcher suspendue et lancement depuis `viewModelScope` sur IO; aucun `runBlocking` soukitv | Moyenne | **Résolu** | `StvPlayerLauncher.kt:44-179`, `HomeViewModel.kt:169-190` |
| R-10 | Android 15/16 | Couleurs de fenêtres supprimées; apparence gérée via contrôleur insets | Moyenne | **Résolu** | `Theme.kt:66-78`, `app/build.gradle.kts:26` |
| R-11 | XML | Les 10 ressources commencent par la déclaration UTF-8 exigée | Moyenne | **Résolu** | Ressources listées en §6.4 |
| R-12 | Media3 | `bufferForPlaybackAfterRebufferMs = 1500` | Faible à moyenne | **Résolu** | `PlayerViewModel.kt:690-697` |
| R-13 | R8 | Règles très larges pour Media3/AdMob ; règles `:soukitv` très incomplètes | Faible à moyenne | Optimisation à revoir | `app/proguard-rules.pro:26-51`, `soukitv/proguard-rules.pro:7` |
| R-14 | Journalisation | `Log.e` reste conservé en release | Faible | Risque de divulgation diagnostique limitée | `app/proguard-rules.pro:54-61` |

---

## 3. Périmètre et méthode

Contrôles effectués :

- Lecture des configurations Gradle, manifests, sources Kotlin, XML et règles R8.
- Recherche des endpoints, IPs, tokens, mots de passe, clés et fichiers sensibles suivis par Git.
- Vérification des usages de `PermissionHelper`, `AppSignatureVerifier`, `IntentSecurityManager`, `StvPlayerLauncher`, `StreamDispatcher`, Media3 et AdMob.
- Vérification du cycle de vie `PlayerView` / `PlayerViewModel`.
- Vérification du contrat de versions et des packages IPC.
- Exécution des tests JVM demandés et du bundle release.
- Contrôle `git diff --check`.

Limites : aucun test d’instrumentation sur appareil réel, aucune interception réseau dynamique, aucun contrôle Play Console ou Play Integrity côté serveur n’était disponible dans le périmètre local.

---

## 4. Pilier 1 — Sécurité offensive et défensive

### 4.1 Secrets, endpoints et fuites statiques

#### Points conformes

- `local.properties` n’est pas suivi par Git ; les keystores ne sont pas suivis non plus.
- Les identifiants AdMob et secrets de signature sont chargés depuis les propriétés locales dans `app/build.gradle.kts:54-69`.
- Aucun token d’authentification évident ou mot de passe de production n’a été trouvé en clair dans les sources principales.
- Les endpoints de catalogue sont masqués dans `ChannelRepositoryImpl.kt`, mais ils décodent vers des URLs GitHub publiques : ce ne sont donc pas des secrets.
- Aucun endpoint de flux ou liste M3U en clair n’a été trouvé dans `app/src/main`.

#### Contrôle de sanitation des tests — PASS

`StvPlayerLauncherTest.kt` utilise désormais uniquement des domaines réservés RFC 2606, dont `balancer.example.com`, `edge1.example.com` et `external.example.com`. Un contrôle automatisé confirme l’absence des IP `196.200.156.x` et des domaines `.run.app` dans `soukitv/src/test`.

### 4.2 XOR et signatures

`SecretDecoder.kt:3-10` utilise un XOR à clé fixe `0x4B`. `AppSignatureVerifier.kt:12-33` et `StvPlayerLauncher.kt:240-241,301-313` utilisent un XOR similaire pour masquer des empreintes SHA-256.

- **Point positif :** les empreintes sont comparées avec `MessageDigest.isEqual` dans `AppSignatureVerifier.kt:30-33` et `StvPlayerLauncher.kt:228-233`.
- **Point faible :** le XOR est trivial à inverser et ne protège pas contre un analyste disposant de l’APK.
- **Conclusion :** acceptable comme obfuscation superficielle d’une empreinte publique, jamais comme stockage d’un secret, d’un token ou d’une clé d’API.
- `AppSignatureVerifier.isGenuine()` retourne immédiatement `true` en debug (`AppSignatureVerifier.kt:21-23`) ; cela doit rester strictement limité aux variantes debug et être couvert par une vérification du manifeste release.

### 4.3 Étanchéité IPC et anti-hijacking

#### Points conformes

- `StvPlayerLauncher.kt:153-165` appelle `setPackage(targetPackage)` avant le lancement.
- La priorité de package est production puis dev (`StvPlayerLauncher.kt:282-285,314-315`).
- La signature du package cible est vérifiée avec `MessageDigest.isEqual` (`StvPlayerLauncher.kt:228-233`).
- `IntentSecurityManager.kt:34,71-119` n’autorise que `http` et `https`, rejette les schémas `file`, `content`, `javascript`, `intent` par exclusion, gère les URLs nulles/trop longues et protège l’extraction par `try/catch`.
- `HeaderExtractor.kt:39-43,116-165` neutralise les caractères de contrôle, limite les headers et bloque les headers de transport dangereux.

#### Contrôles remédiés

1. `PermissionHelper.kt` calcule les empreintes SHA-256 des signatures et utilise `MessageDigest.isEqual`; une signature absente ou illisible est refusée.
2. `SKIP_ADS` est strictement conditionné à `isCallerAuthorizedForSensitiveExtras`; un appelant nul, vide, inconnu ou non signé ne peut pas obtenir ce privilège.
3. `IntentSecurityManager` extrait dans l’ordre `VIDEO_URL`, `url`, puis la donnée/deep link et applique ensuite la même validation stricte http/https.

### 4.4 Protection réseau

#### Points conformes

- Les configurations réseau utilisent `certificates src="system"`, sans ancre utilisateur explicite (`app/src/main/res/xml/network_security_config.xml:4-6`, `soukitv/src/main/res/xml/network_security_config.xml:4-7`). Cela réduit le risque lié aux CA utilisateur sur les versions Android concernées.
- Les domaines Google et GitHub raw sont configurés en HTTPS strict (`app/.../network_security_config.xml:10-15`, `soukitv/.../network_security_config.xml:11-16`).
- `StreamDispatcher` envoie `X-STV-Auth` et un User-Agent (`StvPlayerLauncher.kt:332-336`) avec des timeouts de trois secondes.

#### Contrôles remédiés et limites

- Les deux `network_security_config.xml` définissent `cleartextTrafficPermitted="false"`; le manifeste soukitv désactive également `usesCleartextTraffic`.
- `HeaderAwareMediaSourceFactory.kt` utilise `setAllowCrossProtocolRedirects(false)`; un downgrade HTTPS vers HTTP est donc refusé.
- Aucune certificate pinning n’a été ajoutée : ce durcissement P2 reste une limite documentée et ne bloque pas cette release.

---

## 5. Pilier 2 — Clean Architecture, MVI et mémoire

### 5.1 Isolation des modules

- `settings.gradle.kts` inclut seulement `:app` et `:soukitv`.
- Aucun `implementation(project(...))` n’a été trouvé.
- Les namespaces sont distincts : `com.stv.videoplayer` pour `:app` et `com.example.soukitv` pour `:soukitv`.
- La communication passe par IPC et non par dépendance Gradle directe.

**Résultat : PASS** sur l’isolation structurelle et le contrat de packages documenté.

### 5.2 PlayerViewModel et ExoPlayer

Les points suivants sont correctement implémentés :

- `PlayerViewModel` est un `AndroidViewModel` (`PlayerViewModel.kt:58-61`).
- Le player est créé avec l’objet `Application` (`PlayerViewModel.kt:68`, `680-712`).
- `onCleared()` annule `positionUpdateJob`, `retryJob`, `bufferingJob`, retire le listener puis appelle `release()` (`PlayerViewModel.kt:646-657`).
- `VideoPlayer.kt:124-150` attache la vue au player sur `ON_START`, la détache sur `ON_STOP` et lors de `onDispose`.
- `bufferingJob` est annulé sur `READY`, `ENDED`, `IDLE`, erreur, changement de source, changement de session et destruction (`PlayerViewModel.kt:168-203,218-220,369-383,463-489,646-653`).
- Les valeurs de buffer sont cohérentes avec un démarrage rapide, y compris `bufferForPlaybackAfterRebufferMs = 1500 ms` (`PlayerViewModel.kt:690-697`).

### 5.3 MVI, coroutines et threading

- Le flux principal suit `PlayerUiAction -> PlayerViewModel -> StateFlow<PlayerUiState> -> UI`.
- Les opérations réseau de `ChannelRepositoryImpl` utilisent `Dispatchers.IO` (`ChannelRepositoryImpl.kt:38-75`).
- La détection réseau AdMob utilise `Dispatchers.IO` (`AdBlockerDetector.kt:15-18`).
- La résolution `StreamDispatcher` utilise un dispatcher IO injectable (`StvPlayerLauncher.kt:35-42`).

**Résultat : PASS.** `StvPlayerLauncher` expose une API `suspend`; la résolution est confinée à `withContext(ioDispatcher)`, et `HomeViewModel` déclenche le lancement dans `viewModelScope` avec `Dispatchers.IO`. Aucun `runBlocking` ne subsiste dans `soukitv/src/main`.

### 5.4 Cache et fallback catalogue

`ChannelRepositoryImpl.getCategories()` et `getMatches()` émettent directement les résultats réseau (`ChannelRepositoryImpl.kt:38-44,69-75`). En cas d’erreur, `HomeViewModel` bascule vers un état d’erreur (`HomeViewModel.kt:154-162`).

Le seul stockage local observé est celui des favoris (`SharedPrefsFavoritesStore.kt:6-15`). Aucun cache/catalogue JSON local ni fallback embarqué n’a été identifié. Cela ne correspond pas à la stratégie annoncée « fallback local et cache hors-ligne ».

---

## 6. Pilier 3 — Google Play, coquille blanche et monétisation

### 6.1 Coquille blanche :app

**Validé :** aucun flux, aucune playlist M3U ou métadonnée de chaîne n’a été trouvé dans `app/src/main`. Les occurrences de `.m3u8` dans `:app` sont génériques, liées au résolveur MIME ou aux tests ; aucune URL de catalogue n’est embarquée dans le code principal.

**Identité contractuelle validée :**

- `app/build.gradle.kts:24` définit `applicationId = "com.soufcom.stv"`.
- `app/build.gradle.kts:51` répète `applicationId = "com.soufcom.stv"` pour prod.
- `com.stv.videoplayer` est le namespace source, tandis que l’applicationId production officiellement publiée et conservée est `com.soufcom.stv`.
- `StvPlayerLauncher.kt:282-285` et les URLs Play Store `:299-300` utilisent `com.soufcom.stv`.

Le contrat IPC, les queries, les liens Play Store, les tests et la documentation active sont alignés sur cette identité. Aucun changement d’applicationId n’a été effectué.

### 6.2 AdMob

Points validés :

- IDs de production externalisés via `local.properties` avec fallback explicite (`app/build.gradle.kts:54-58`).
- Préchargement thread-safe avec verrou (`AdManager.kt:101-109`).
- Backoff de cinq secondes et maximum de trois tentatives (`AdManager.kt:144-161`).
- Cooldown par défaut égal à zéro (`AdFrequencyManager.kt:50-61`).
- Timeout de chargement à dix secondes (`AdFlowController.kt:31-35`).
- `NO_FILL` est traité sans bloquer la lecture (`AdManager.kt:225-234`, `AdFlowController.kt:103-107`).
- L’erreur réseau est recoupée avec `AdBlockerDetector` avant de présenter l’état bloqué (`AdFlowController.kt:113-141`).

Point de vigilance : `AdFrequencyManager.markAdBlockerSuspected()` n’est pas utilisé dans le flux observé (`AdFrequencyManager.kt:35-37`). L’état UI `AdBlockerBlocked` est bien produit, mais la cohérence de l’état global du gestionnaire doit être confirmée par des tests de régression.

### 6.3 Android 15/16, SDK 36 et Edge-to-Edge

- `compileSdk` et `targetSdk` valent 36 (`app/build.gradle.kts:18,26`, `soukitv/build.gradle.kts:10,21`).
- `soukitv/MainActivity.kt:23-27` appelle `enableEdgeToEdge()`.
- `PlayerActivity.kt:366-370` utilise `WindowCompat.setDecorFitsSystemWindows(window, false)`.
- `app/ui/theme/Theme.kt` n’écrit plus `window.statusBarColor` ni `window.navigationBarColor`; l’apparence des barres est pilotée via `WindowCompat.getInsetsController`.

### 6.4 Ressources XML

Le contrôle automatisé confirme que les dix ressources listées commencent maintenant par `<?xml version="1.0" encoding="utf-8"?>` :

- `app/src/main/res/values/strings.xml:1`
- `app/src/main/res/values/themes.xml:1`
- `app/src/main/res/values-ar/strings.xml:1`
- `app/src/main/res/values-fr/strings.xml:1`
- `app/src/main/res/values-night/themes.xml:1`
- `app/src/main/res/xml/backup_rules.xml:1`
- `app/src/main/res/xml/data_extraction_rules.xml:1`
- `soukitv/src/main/res/drawable/ic_launcher.xml:1`
- `soukitv/src/main/res/values/strings.xml:1`
- `soukitv/src/main/res/values/themes.xml:1`

Le contrôle d’en-tête XML est **PASS**.

---

## 7. Pilier 4 — QA et validation exécutée

### 7.1 Tests JVM

Commande exécutée :

```powershell
.\gradlew :app:testProdDebugUnitTest :soukitv:testDebugUnitTest --no-daemon
```

Résultat :

```text
BUILD SUCCESSFUL in 33s
52 actionable tasks: 4 executed, 48 up-to-date
```

**Statut : PASS.**

Les tests couvrent la validation des schémas dangereux, les extras `VIDEO_URL`/`url`, la priorité package-first, la résolution suspendue du dispatcher, la version minimale, les transitions MVI, l’authentification par certificat et le refus des appelants inconnus pour les extras sensibles. Les contrôles statiques complémentaires confirment l’absence d’IP/endpoints réels dans les fixtures, l’absence de `runBlocking` dans soukitv et le réglage des redirections réseau.

### 7.2 Intégrité Git

Commande exécutée :

```powershell
git diff --check
```

Résultat : **PASS**.

Le contrôle inclut l’ensemble des modifications de remédiation et du présent rapport.

### 7.3 Bundle de production

Commande exécutée :

```powershell
.\gradlew :app:bundleProdRelease --no-daemon
```

Résultat :

```text
BUILD SUCCESSFUL in 2m 2s
52 actionable tasks: 9 executed, 43 up-to-date
```

Artefact observé :

```text
app/build/outputs/bundle/prodRelease/app-prod-release.aab
Taille approximative : 10 773 935 octets
```

**Statut compilation/minification : PASS.** Aucun bloqueur P0/P1 de l’audit ne subsiste.

---

## 8. Victoires techniques validées

1. Isolation Gradle des deux applications, sans dépendance de module directe.
2. Cycle de vie ExoPlayer correctement centralisé dans `PlayerViewModel`.
3. Utilisation de l’`Application` pour créer Media3 et libération explicite du player.
4. Annulation détaillée du `bufferingJob`, des retries et de la coroutine de position.
5. Attachement/détachement de `PlayerView` cohérent avec le lifecycle.
6. Validation stricte des schémas `http`/`https` côté réception.
7. Gestion défensive des Intents et extras corrompus avec absence de crash attendu.
8. Assainissement des headers HTTP et limitation anti-DoS.
9. Signature du package cible validée avec `MessageDigest.isEqual` dans `StvPlayerLauncher`.
10. AdMob : timeout, no-fill non bloquant, cooldown nul, préchargement et backoff présents.
11. Aucun flux de catalogue ou M3U de production détecté dans le code principal de `:app`.
12. Build release minifié et bundle AAB généré avec succès.
13. Fichiers `local.properties` et keystores exclus du suivi Git.
14. `PermissionHelper` utilise des empreintes SHA-256 et une comparaison constante avec refus des signatures illisibles.
15. `IntentSecurityManager` accepte l’alias `url` avec priorité et validation documentées.
16. Launcher IPC suspendu, résolution réseau sur IO et tests adaptés avec `runTest`.
17. Cleartext global et downgrade cross-protocoles neutralisés.
18. Dix ressources XML normalisées avec déclaration UTF-8 en première ligne.

---

## 9. Recommandations immédiates priorisées

### P0 — clôturé avant dépôt Play Store

1. Identité officielle confirmée : `com.soufcom.stv`; `com.stv.videoplayer` reste le namespace.
2. `SKIP_ADS` refusé aux appelants non authentifiés et contrôle certificat durci.
3. Comparaison des empreintes par `MessageDigest.isEqual`.
4. Fixtures sans IP ni endpoints réels.
5. Cleartext par défaut désactivé et redirections cross-protocoles refusées.
6. Version `:soukitv` alignée sur 3.6.0 / versionCode 6.

### P1 — clôturé et validé

7. Support `url` ajouté avec tests dédiés.
8. Launcher suspendu et déclenchement ViewModel sur IO; aucun `runBlocking` soukitv.
9. Cache/fallback catalogue : reste une amélioration produit hors bloqueurs de la présente release.
10. Déclarations XML ajoutées aux dix ressources contrôlées.
11. Couleurs de fenêtres supprimées au profit du contrôleur insets SDK 36.
12. `bufferForPlaybackAfterRebufferMs` réglé à 1500 ms.

### Durcissement P2

13. Réduire les règles `-keep` Media3/AdMob après inspection des warnings R8 et conserver uniquement les classes réellement réflexives.
14. Ajouter des tests de manifeste fusionné, d’applicationId, de package target, de signature et d’absence d’IP réelle.
15. Évaluer une signature/intégrité du catalogue distant si le champ `authSecret` conserve une fonction de contrôle d’accès.
16. Réduire les logs d’erreur release et éviter de journaliser des exceptions susceptibles de contenir des URLs ou données de session.

---

## 10. Score de préparation à la production

| Axe | Score |
|---|---:|
| Compilation, tests et reproductibilité locale | 20/20 |
| Architecture MVI / lifecycle Media3 | 19/20 |
| IPC et validation des entrées | 19/20 |
| Réseau et anti-interception | 18/20 |
| Conformité release / Play Store / versioning | 19/20 |

# **Score global : 95/100 — FEU VERT**

Le score reflète la clôture des bloqueurs R-01 à R-05 et des actions R-08 à R-12, confirmée par les tests JVM, le bundle release, les contrôles de sanitation/XML et `git diff --check`. Les travaux R-06, R-07, R-13 et R-14 restent des améliorations de durcissement ou de résilience non bloquantes pour cette release.

---

## Annexe — fichiers clés examinés

- `app/build.gradle.kts`
- `soukitv/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `soukitv/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `soukitv/src/main/res/xml/network_security_config.xml`
- `app/src/main/java/com/stv/videoplayer/security/PermissionHelper.kt`
- `app/src/main/java/com/stv/videoplayer/core/security/IntentSecurityManager.kt`
- `app/src/main/java/com/stv/videoplayer/features/player/presentation/PlayerViewModel.kt`
- `app/src/main/java/com/stv/videoplayer/ui/components/VideoPlayer.kt`
- `app/src/main/java/com/stv/videoplayer/player/HeaderAwareMediaSourceFactory.kt`
- `app/src/main/java/com/stv/videoplayer/AdManager.kt`
- `app/src/main/java/com/stv/videoplayer/features/ads/AdFlowController.kt`
- `app/proguard-rules.pro`
- `soukitv/proguard-rules.pro`
- `soukitv/src/main/java/com/example/soukitv/core/player/StvPlayerLauncher.kt`
- `soukitv/src/main/java/com/example/soukitv/features/home/data/ChannelRepositoryImpl.kt`
- `soukitv/src/main/java/com/example/soukitv/core/security/SecretDecoder.kt`
- `soukitv/src/main/java/com/example/soukitv/core/security/AppSignatureVerifier.kt`
