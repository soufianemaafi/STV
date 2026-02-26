# 🔄 GUIDE : Synchroniser avec GitHub

**Date :** 26 Février 2026  
**Problème :** Nouveaux commits sur GitHub non visibles localement  
**Cause :** Dépôt local non connecté à GitHub

---

## 🎯 DEUX SOLUTIONS POSSIBLES

### ✅ Solution 1 : CLONER le dépôt GitHub (RECOMMANDÉ)

**Si vous voulez repartir du dépôt GitHub existant avec tous les commits :**

```powershell
# 1. Sauvegarder votre travail actuel
cd C:\Users\Lenovo\StudioProjects
Rename-Item STV4 STV4_backup

# 2. Cloner le dépôt GitHub
git clone https://github.com/VOTRE_USERNAME/VOTRE_REPO.git STV4

# 3. Entrer dans le dossier cloné
cd STV4

# 4. Vérifier que vous avez les derniers commits
git log --oneline -10

# 5. Récupérer vos modifications depuis le backup si nécessaire
# (Copier manuellement les fichiers modifiés depuis STV4_backup)
```

**Avantages :**
- ✅ Historique complet des commits
- ✅ Branches distantes synchronisées
- ✅ Configuration automatique

---

### ✅ Solution 2 : CONNECTER le dépôt local à GitHub

**Si vous voulez garder ce dépôt local et le connecter à GitHub :**

```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# 1. Ajouter le dépôt distant (remplacer par votre URL GitHub)
git remote add origin https://github.com/VOTRE_USERNAME/VOTRE_REPO.git

# 2. Récupérer les commits distants
git fetch origin

# 3. Vérifier les branches distantes disponibles
git branch -r

# 4. Fusionner les commits distants avec votre branche locale
# Option A : Merge (conserve l'historique complet)
git merge origin/main --allow-unrelated-histories

# Option B : Rebase (historique linéaire plus propre)
git rebase origin/main

# 5. Pousser vos modifications locales
git push origin main
```

**⚠️ Si conflit lors du merge :**
```powershell
# Résoudre les conflits manuellement dans les fichiers
# Puis :
git add .
git commit -m "Merge remote changes"
git push origin main
```

---

## 📋 ÉTAPES DÉTAILLÉES (Solution 1 - Recommandée)

### Étape 1 : Trouver l'URL de votre dépôt GitHub

Allez sur GitHub.com → Votre dépôt → Bouton vert "Code"

**Exemple :**
```
https://github.com/username/STV4.git
```

---

### Étape 2 : Sauvegarder votre travail actuel

```powershell
# Aller dans le dossier parent
cd C:\Users\Lenovo\StudioProjects

# Renommer le dossier actuel
Rename-Item STV4 STV4_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')

# Vérifier la sauvegarde
dir
```

---

### Étape 3 : Cloner le dépôt GitHub

```powershell
# Remplacer par votre URL GitHub
git clone https://github.com/username/STV4.git STV4

# Entrer dans le nouveau dossier
cd STV4

# Vérifier les branches
git branch -a

# Vérifier les commits récents
git log --oneline -10
```

---

### Étape 4 : Récupérer vos modifications locales

**Fichiers à récupérer depuis le backup :**

```powershell
$backup = "C:\Users\Lenovo\StudioProjects\STV4_backup_*"
$current = "C:\Users\Lenovo\StudioProjects\STV4"

# Copier les fichiers de documentation créés aujourd'hui
Copy-Item "$backup\APK_RELEASE_READY.md" $current -Force
Copy-Item "$backup\install_release_apks.ps1" $current -Force

# Copier les APKs release si vous voulez les garder
Copy-Item "$backup\app\build\outputs\apk\prod\release\*" "$current\app\build\outputs\apk\prod\release\" -Force -ErrorAction SilentlyContinue
Copy-Item "$backup\soukitv\build\outputs\apk\release\*" "$current\soukitv\build\outputs\apk\release\" -Force -ErrorAction SilentlyContinue

# Vérifier les différences
git status
```

---

### Étape 5 : Commiter vos ajouts

```powershell
cd C:\Users\Lenovo\StudioProjects\STV4

# Ajouter vos fichiers
git add APK_RELEASE_READY.md install_release_apks.ps1

# Commiter
git commit -m "docs: Add release APK documentation and install script"

# Pousser vers GitHub
git push origin main
```

---

## 🔍 DIAGNOSTIC : Quelle solution choisir ?

### Utilisez Solution 1 (Cloner) si :
- ✅ Les commits distants sont plus importants
- ✅ Vous voulez l'historique complet
- ✅ Vous n'avez pas beaucoup de modifications locales non commitées

### Utilisez Solution 2 (Connecter) si :
- ✅ Vous avez beaucoup de travail local non commité
- ✅ Vous voulez fusionner manuellement les changements
- ✅ Vous comprenez bien Git merge/rebase

---

## 🚨 COMMANDES D'URGENCE

### Voir les fichiers modifiés localement
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4
git status
```

### Sauvegarder temporairement vos modifications
```powershell
# Stash (mettre de côté) vos modifications
git stash save "Work in progress"

# Puis faire le pull/merge
git pull origin main

# Récupérer vos modifications
git stash pop
```

### Annuler toutes les modifications locales (⚠️ DANGER)
```powershell
# ⚠️ Ceci SUPPRIME toutes vos modifications locales
git reset --hard HEAD
git clean -fd
```

---

## 📞 QUELLE EST VOTRE SITUATION ?

**Question 1 :** Avez-vous l'URL du dépôt GitHub ?
- Exemple : `https://github.com/username/STV4.git`

**Question 2 :** Quelles modifications locales voulez-vous garder ?
- Documentation (APK_RELEASE_READY.md, etc.) ?
- Code source modifié ?
- APKs buildés ?

**Question 3 :** Les commits distants sont-ils plus importants que votre travail local ?
- Si OUI → Solution 1 (Cloner)
- Si NON → Solution 2 (Connecter + Merge)

---

## 🎓 WORKFLOW RECOMMANDÉ (Après synchronisation)

### Avant de commencer à travailler
```powershell
cd C:\Users\Lenovo\StudioProjects\STV4
git pull origin main
```

### Pendant le travail
```powershell
# Vérifier les fichiers modifiés
git status

# Commiter régulièrement
git add .
git commit -m "feat: Description des changements"
```

### Après avoir terminé
```powershell
# Pousser vers GitHub
git push origin main
```

---

## 🔧 CONFIGURATION UTILE

### Configurer Git (si pas déjà fait)
```powershell
git config --global user.name "Votre Nom"
git config --global user.email "votre.email@example.com"

# Mémoriser les credentials GitHub
git config --global credential.helper wincred
```

### Voir la configuration actuelle
```powershell
git config --list
```

---

## 📝 PROCHAINES ÉTAPES

1. **Identifiez** l'URL de votre dépôt GitHub
2. **Choisissez** Solution 1 ou 2 selon votre situation
3. **Exécutez** les commandes correspondantes
4. **Vérifiez** que vous voyez les nouveaux commits : `git log --oneline -10`

---

**Besoin d'aide ?** Fournissez-moi :
- L'URL de votre dépôt GitHub
- La liste des fichiers que vous voulez absolument garder (`git status`)
- Je vous donnerai les commandes exactes à exécuter

---

**Auteur :** GitHub Copilot  
**Date :** 26 Février 2026

