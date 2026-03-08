## Refactorisation PlayerActivity - Résumé des changements

### Fichiers créés
1. **PlayerUiState.kt** (`app/src/main/java/com/example/stv/ui/PlayerUiState.kt`)
   - Sealed class représentant les états de l'UI
   - États : LoadingAds, Fallback, Ready, Error
   - Remplace les booléens multiples (shouldPlayVideo, showFallbackBanner, isAdShown, etc.)

2. **AdsController.kt** (`app/src/main/java/com/example/stv/ads/AdsController.kt`)
   - Gère la logique d'affichage des publicités
   - Découple la logique métier d'ads de l'activité
   - Retourne un `AdResult` sealed class

3. **PlayerController.kt** (`app/src/main/java/com/example/stv/player/PlayerController.kt`)
   - Gère l'orchestration du player (validation d'URL, whitelist optionnel)
   - Validation stricte des URLs (schéma, longueur, format)

### Fichier modifié
**PlayerActivity.kt** :
- ✅ Imports mis à jour
- ✅ onCreate simplifié : valide l'URL, crée les contrôleurs
- ✅ setContent remplacé par une state machine unique
- ✅ LaunchedEffect pour orchestrer les ads
- ✅ When() pour le rendu selon l'état
- ✅ FallbackBanner mise à jour avec paramètre `adBlockDetected`

### Bénéfices
- **Moins de bugs** : un seul état au lieu de 3 booléens interagissant
- **Plus lisible** : la logique est linéaire (état initial → orchestration → rendu)
- **Plus testable** : les contrôleurs sont des classes simples
- **Maintenable** : ajouter un état = ajouter une ligne dans le sealed class

### Prochaines étapes
1. Compiler et tester
2. Validation des URLs en strict
3. Sécuriser SKIP_ADS (permission/signature)
4. Configurer IDs AdMob prod vs test

