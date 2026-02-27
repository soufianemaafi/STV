# Plan de Tests - STV Player (Release v1.0)

**Date** : 27 février 2026  
**APK** : `app/build/outputs/apk/prod/release/app-prod-release.apk`  
**Version** : 1.0  
**Build** : prod-release  
**Signature** : Release keystore sécurisé

---

## Préparation des tests

### 1. Installation de l'APK
```powershell
# Méthode 1 : Via ADB (recommandé)
adb install -r "C:\Users\soufi\AndroidStudioProjects\STV5\app\build\outputs\apk\prod\release\app-prod-release.apk"

# Méthode 2 : Copier sur l'appareil
# - Copier l'APK sur le téléphone
# - Ouvrir le fichier avec le gestionnaire de fichiers
# - Autoriser l'installation depuis sources inconnues si demandé
```

### 2. Vérification de la signature
```powershell
# Depuis le PC
$keytool = "C:\Program Files\JetBrains\AndroidStudio\jbr\bin\keytool.exe"
& $keytool -printcert -jarfile "app\build\outputs\apk\prod\release\app-prod-release.apk"
```

---

## Tests fonctionnels

### Test 1 : Installation et premier lancement
**Objectif** : Vérifier l'installation et le splash screen

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 1.1 | Installer l'APK | Installation réussie | | |
| 1.2 | Lancer l'app | Splash screen STV apparaît | | |
| 1.3 | Attendre 2s | Page d'accueil s'affiche | | |
| 1.4 | Vérifier UI | Top bar avec "STV" + bouton menu + bouton "+" | | |

---

### Test 2 : Navigation et ajout de vidéo depuis l'accueil
**Objectif** : Tester le flux Accueil → Add → Accueil → Videos

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 2.1 | Cliquer sur bouton "+" central | Page "Ajouter un flux" s'affiche | | |
| 2.2 | Saisir titre : "Test Stream" | Titre accepté | | |
| 2.3 | Saisir URL : `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8` | URL valide (icône verte) | | |
| 2.4 | Cliquer "Save" | Retour à l'accueil | | |
| 2.5 | Cliquer "Mes vidéos" | Liste des vidéos s'affiche | | |
| 2.6 | Vérifier | "Test Stream" apparaît en haut de la liste | | |

---

### Test 3 : Navigation depuis la liste des vidéos
**Objectif** : Tester le flux Videos → Add → Videos (liste mise à jour)

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 3.1 | Depuis l'accueil, cliquer "Mes vidéos" | Liste affichée | | |
| 3.2 | Cliquer bouton "+" (FAB) | Page "Ajouter un flux" | | |
| 3.3 | Saisir titre : "Test 2" | Titre accepté | | |
| 3.4 | Saisir URL : `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4` | URL valide | | |
| 3.5 | Cliquer "Save" | **Retour direct à la liste** | | |
| 3.6 | Vérifier | "Test 2" apparaît immédiatement en haut | | |

---

### Test 4 : Suppression de vidéo
**Objectif** : Tester la suppression d'une vidéo (sauf Big Buck Bunny par défaut)

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 4.1 | Dans la liste, repérer "Test Stream" | Icône delete visible | | |
| 4.2 | Cliquer icône delete | Vidéo disparaît de la liste | | |
| 4.3 | Chercher "Big Buck Bunny" (défaut) | Pas d'icône delete visible | | |
| 4.4 | Quitter et relancer l'app | "Test Stream" toujours absente | | |

---

### Test 5 : Lecture de vidéo (Player)
**Objectif** : Tester le player avec une URL valide

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 5.1 | Cliquer sur "Big Buck Bunny" | **Pub interstitielle** ou fallback (5s) | | |
| 5.2 | Attendre fin pub | Player démarre automatiquement | | |
| 5.3 | Vérifier lecture | Vidéo lit correctement, son OK | | |
| 5.4 | Cliquer sur l'écran | Contrôles apparaissent (play/pause, timeline) | | |
| 5.5 | Tester pause/play | Fonctionne | | |
| 5.6 | Tester avance/recul (10s) | Fonctionne | | |
| 5.7 | Cliquer bouton retour | Retour à la liste des vidéos | | |

---

### Test 6 : Mode Picture-in-Picture (PIP)
**Objectif** : Tester le mode PIP (Android 8+)

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 6.1 | Lancer une vidéo | Player actif | | |
| 6.2 | Appuyer sur bouton Home | Vidéo continue en mode PIP | | |
| 6.3 | Vérifier mini-player | Flottant en coin d'écran, lecture continue | | |
| 6.4 | Cliquer sur mini-player | Retour en plein écran | | |

---

### Test 7 : Drawer (menu latéral)
**Objectif** : Tester le menu et ses options

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 7.1 | Cliquer icône menu (☰) | Drawer s'ouvre à gauche | | |
| 7.2 | Vérifier couleur | Même couleur que top bar | | |
| 7.3 | Vérifier items | Historique, Favoris, Paramètres, Confidentialité, CGU, Quitter | | |
| 7.4 | Cliquer "Quitter" | App se ferme complètement | | |
| 7.5 | Relancer l'app | Redémarre normalement | | |

---

### Test 8 : Validation d'URL
**Objectif** : Tester la validation stricte des URLs

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 8.1 | Ajouter flux avec titre vide | Message d'erreur | | |
| 8.2 | Ajouter flux avec URL invalide : "test" | Message "URL invalide" | | |
| 8.3 | Ajouter flux HTTP : `http://example.com/test.mp4` | Accepté (cleartext autorisé) | | |
| 8.4 | Ajouter flux HTTPS valide | Accepté | | |

---

### Test 9 : Recherche de vidéos
**Objectif** : Tester la barre de recherche

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 9.1 | Dans la liste, saisir "Buck" dans la recherche | Seul "Big Buck Bunny" visible | | |
| 9.2 | Cliquer X pour effacer | Toutes les vidéos réapparaissent | | |
| 9.3 | Rechercher un titre inexistant | "Aucun résultat" | | |

---

### Test 10 : Deep Links
**Objectif** : Tester les intents externes

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 10.1 | Ouvrir navigateur, saisir : `stv://play?url=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8` | STV Player se lance + lit la vidéo | | |
| 10.2 | Depuis un explorateur de fichiers, ouvrir un fichier .mp4 | STV proposé comme option de lecture | | |

---

### Test 11 : Gestion réseau
**Objectif** : Tester le comportement hors ligne

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 11.1 | Activer mode avion | | | |
| 11.2 | Lancer une vidéo | Message "Erreur réseau" ou équivalent | | |
| 11.3 | Désactiver mode avion | | | |
| 11.4 | Réessayer lecture | Fonctionne | | |

---

### Test 12 : Publicités (AdMob)
**Objectif** : Vérifier le comportement des pubs

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 12.1 | Lancer une vidéo | Pub interstitielle test s'affiche | | |
| 12.2 | Fermer la pub | Player démarre | | |
| 12.3 | Si pub ne charge pas (6s) | Fallback bannière 5s puis player | | |
| 12.4 | Vérifier IDs pub | IDs test AdMob visibles (à remplacer avant prod) | | |

---

### Test 13 : Rotation écran
**Objectif** : Tester la stabilité lors de rotation

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 13.1 | Sur l'accueil, tourner l'écran | UI s'adapte sans crash | | |
| 13.2 | Dans la liste, tourner l'écran | Liste reste affichée | | |
| 13.3 | Dans le player, tourner l'écran | Lecture continue sans interruption | | |

---

### Test 14 : Sécurité - AppelantNonAutorisé
**Objectif** : Vérifier que les apps non signées sont refusées (test théorique)

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 14.1 | Créer une app test non signée | App lance STV | | |
| 14.2 | App test appelle PlayerActivity | Accès refusé + message "Accès refusé" | | |

**Note** : Ce test nécessite une app tierce. Sauter si pas d'app test disponible.

---

## Tests de régression

### Test 15 : Après mise à jour
**Objectif** : Vérifier que les données persistent après mise à jour

| Étape | Action | Résultat attendu | ✅/❌ | Notes |
|-------|--------|------------------|-------|-------|
| 15.1 | Ajouter 3 vidéos | Vidéos présentes | | |
| 15.2 | Désinstaller l'app | | | |
| 15.3 | Réinstaller la même APK | | | |
| 15.4 | Vérifier la liste | Vidéos peuvent revenir (backup Android) ou non | | |

---

## Résultats attendus

### ✅ Tests critiques (obligatoires)
- [ ] Test 1 : Installation
- [ ] Test 2 : Ajout vidéo depuis accueil
- [ ] Test 3 : Ajout vidéo depuis liste
- [ ] Test 5 : Lecture vidéo
- [ ] Test 7 : Menu drawer

### ⚠️ Tests importants (recommandés)
- [ ] Test 4 : Suppression
- [ ] Test 6 : PIP
- [ ] Test 8 : Validation URL
- [ ] Test 9 : Recherche
- [ ] Test 12 : Publicités

### 📋 Tests optionnels
- [ ] Test 10 : Deep links
- [ ] Test 11 : Hors ligne
- [ ] Test 13 : Rotation
- [ ] Test 14 : Sécurité
- [ ] Test 15 : Régression

---

## Critères de succès

### Pour valider l'APK
- ✅ Tous les tests critiques passent
- ✅ Aucun crash pendant les tests
- ✅ Navigation cohérente et fluide
- ✅ Liste des vidéos mise à jour correctement
- ✅ Player lit les vidéos sans erreur

### Blockers (rejeter l'APK si)
- ❌ Crash au lancement
- ❌ Impossible d'ajouter une vidéo
- ❌ Liste ne se met pas à jour après ajout
- ❌ Player ne lit pas les vidéos
- ❌ Bouton retour ne fonctionne pas

---

## Bugs connus à surveiller

1. **Navigation** : Vérifier que le retour depuis AddVideoActivity va bien à la liste (et pas l'accueil)
2. **Refresh liste** : La liste doit se mettre à jour automatiquement après ajout
3. **Icons RTL** : Les flèches doivent se retourner en arabe (si testé)
4. **Ads test** : IDs test AdMob actifs → à remplacer avant prod

---

## Rapport de tests

À remplir après tests :

**Date des tests** : ___________  
**Testeur** : ___________  
**Appareil** : ___________  
**Android version** : ___________  

**Résumé** :
- Tests réussis : __ / 15
- Tests échoués : __
- Bugs trouvés : __

**Décision** :
- [ ] ✅ APK validé pour publication (après remplacement IDs AdMob)
- [ ] ⚠️ APK à corriger (bugs mineurs)
- [ ] ❌ APK à rejeter (bugs critiques)

**Notes** :
_______________________________________________________
_______________________________________________________
_______________________________________________________

