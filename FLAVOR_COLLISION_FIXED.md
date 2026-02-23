# 🔧 CORRECTION : ProductFlavor collision fix

## ❌ Problème

```
ProductFlavor names cannot collide with BuildType names
```

**Cause** : Les flavors "debug" et "release" entraient en collision avec les buildTypes par défaut de Gradle.

---

## ✅ Solution

Renommer les flavors en noms distincts qui ne collisionnent pas avec les buildTypes.

### Avant
```kotlin
productFlavors {
    create("debug") { ... }     // ❌ Collide avec buildType "debug"
    create("release") { ... }   // ❌ Collide avec buildType "release"
}
```

### Après
```kotlin
productFlavors {
    create("dev") { ... }       // ✅ Pas de collision (nom unique)
    create("prod") { ... }      // ✅ Pas de collision (nom unique)
}
```

---

## 📋 Changements appliqués

| Flavor | Ancien | Nouveau | Raison |
|--------|--------|---------|--------|
| Dev | `"debug"` | `"dev"` | Évite collision avec buildType "debug" |
| Prod | `"release"` | `"prod"` | Évite collision avec buildType "release" |

---

## 🎯 Résultat final

```
Flavors (environment dimension) :
├─ dev (IDs test AdMob)
└─ prod (IDs production)

BuildTypes (par défaut) :
├─ debug
└─ release

Combinaisons possibles :
├─ devDebug (dev flavor + debug buildType)
├─ devRelease (dev flavor + release buildType)
├─ prodDebug (prod flavor + debug buildType)
└─ prodRelease (prod flavor + release buildType)
```

---

## 🚀 Prochaines étapes

1. Compiler : `./gradlew clean build`
2. Vérifier le succès
3. Continuer avec l'Optimisation 4️⃣

---

## 📝 Notes techniques

- **Flavors** : Variations d'app (dev vs prod, free vs paid, etc.)
- **BuildTypes** : Mode de build (debug vs release)
- **Collision** : Gradle empêche deux avec même nom dans même dimension
- **Solution** : Utiliser des noms uniques pour les flavors


