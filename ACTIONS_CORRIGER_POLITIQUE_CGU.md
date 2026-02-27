# ✅ GUIDE D'ACTIONS - Corriger Politique & CGU

**Temps requis** : 20 minutes  
**Gravité** : 🔴 CRITIQUE (blocage publication sinon)

---

## 🚨 ACTIONS CRITIQUES (OBLIGATOIRES)

### Action 1 : Ajouter activities au manifest (2 min)
**Fichier** : `app/src/main/AndroidManifest.xml`

**Où** : Après `AddVideoActivity`, avant `</application>`

**Ajouter** :
```xml
<activity
    android:name=".PrivacyPolicyActivity"
    android:exported="false"
    android:label="@string/privacy_policy" />

<activity
    android:name=".TermsOfServiceActivity"
    android:exported="false"
    android:label="@string/terms_of_service" />
```

**Sans ça** : CRASH au clic sur liens ❌

---

### Action 2 : Remplacer contenu Politique (8 min)
**Fichier** : `app/src/main/java/com/example/stv/PrivacyPolicyActivity.kt`

**Contenu** : Voir `CONTENU_POLITIQUE_CONFIDENTIALITE_COMPLET.md`

**Remplacer** :
- Les 9 sections actuelles → 16 sections complètes
- Ajouter sections COPPA, CCPA, RGPD détails, etc.

---

### Action 3 : Remplacer contenu CGU (8 min)
**Fichier** : `app/src/main/java/com/example/stv/TermsOfServiceActivity.kt`

**Contenu** : Voir `CONTENU_CONDITIONS_UTILISATION_COMPLET.md`

**Remplacer** :
- Les 11 sections actuelles → 14 sections complètes
- Ajouter clause anti-piratage, indemnisation, etc.

---

### Action 4 : Personnaliser (2 min)
**Dans les 2 fichiers**, remplacer :

```
[VOTRE_EMAIL@example.com]  → ton_email@gmail.com
[VOTRE NOM/ENTREPRISE]     → Ton Nom ou Entreprise
[VOTRE PAYS]               → France/Algérie/Tunisie/etc.
[VOTRE VILLE/RÉGION]       → Ta ville
```

**Sans email valide** : Refus publication ❌

---

## 🧪 Action 5 : Tester (4 min)

```powershell
# 1. Build
.\gradlew.bat assembleProdRelease

# 2. Installer
adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk

# 3. Tester
# - Ouvrir drawer
# - Cliquer "Politique de Confidentialité"
# - Vérifier que ça s'affiche (pas de crash)
# - Retour
# - Cliquer "Conditions d'Utilisation"
# - Vérifier que ça s'affiche (pas de crash)
```

**Résultat attendu** : Pas de crash, contenu visible ✅

---

## 📋 CHECKLIST VALIDATION

Après corrections :

- [ ] Activities déclarées dans manifest
- [ ] Contenu Politique complet (16 sections)
- [ ] Contenu CGU complet (14 sections)
- [ ] Email contact ajouté partout
- [ ] Nom/entreprise personnalisé
- [ ] Pays/ville précisé
- [ ] Build réussi sans erreur
- [ ] Test drawer → Politique → OK
- [ ] Test drawer → CGU → OK
- [ ] Pas de crash

---

## ⚠️ POINTS CRITIQUES À VÉRIFIER

### 1. Email fonctionnel
- ✅ Doit être un email que tu consultes régulièrement
- ✅ Pas d'email jetable
- ✅ Gmail, Outlook ou domaine personnel OK

### 2. Sections COPPA
- ✅ "Pas destiné aux < 13 ans" doit être présent
- ✅ Procédure si parent découvre collecte

### 3. Droits RGPD/CCPA
- ✅ Tous les droits listés
- ✅ Délais de réponse précisés
- ✅ Procédure suppression claire

---

## 🎯 RÉSUMÉ

| Aspect | Avant | Après |
|--------|-------|-------|
| Activities dans manifest | ❌ Absent (CRASH) | ✅ Déclarées |
| Contenu Politique | 36% conforme | 100% conforme |
| Contenu CGU | 30% conforme | 100% conforme |
| Email contact | ❌ Absent | ✅ Visible |
| COPPA compliance | ❌ Absent | ✅ Présent |
| RGPD/CCPA | ⚠️ Basique | ✅ Complet |
| Conformité Play Store | ❌ NON | ✅ OUI |

---

## 📁 FICHIERS RÉFÉRENCE

- `CONTENU_POLITIQUE_CONFIDENTIALITE_COMPLET.md` - Contenu à copier
- `CONTENU_CONDITIONS_UTILISATION_COMPLET.md` - Contenu à copier
- `ANALYSE_POLITIQUE_CONFIDENTIALITE_CGU.md` - Analyse détaillée

---

**TEMPS TOTAL** : 20 minutes  
**EFFORT** : Faible  
**IMPACT** : CRITIQUE (blocage publication sinon)

**Veux-tu que j'applique ces corrections maintenant ?** ✅

