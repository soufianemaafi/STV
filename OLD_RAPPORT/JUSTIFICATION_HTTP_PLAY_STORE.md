# Justification technique - Usage HTTP dans STV Player

**Date** : 27 février 2026  
**Application concernée** : STV Player (`com.example.stv`)  
**Developer** : [Votre nom/entreprise]  
**Objet** : Justification de l'autorisation cleartext traffic (HTTP) pour publication Google Play Store

---

## Résumé exécutif

STV Player est un lecteur de flux vidéo générique permettant aux utilisateurs de lire du contenu multimédia streaming. L'autorisation du trafic HTTP (cleartext) est nécessaire pour garantir la compatibilité avec les flux vidéo existants qui ne sont disponibles qu'en HTTP, conformément aux standards de l'industrie des lecteurs vidéo (VLC, MX Player, Kodi, etc.).

**Conclusion** : L'usage HTTP est limité strictement à la lecture de contenu multimédia et ne présente aucun risque pour la sécurité des données utilisateur.

---

## 1. Contexte technique

### 1.1 Type d'application
- **STV Player** : Lecteur vidéo générique universel permettant la lecture de flux streaming fournis par l'utilisateur ou des applications tierces

### 1.2 Fonctionnalité concernée
STV Player permet la lecture de flux vidéo streaming dans les formats :
- HLS (HTTP Live Streaming - `.m3u8`)
- DASH (Dynamic Adaptive Streaming)
- MP4, MKV, et autres formats vidéo standards
- RTSP, RTMP (protocoles streaming)

L'application peut être lancée :
- Par l'utilisateur directement (saisie manuelle d'URL)
- Via des deep links (`stv://play?url=...`)
- Via des intents système (ouverture de fichiers vidéo)
- Par des applications catalogue tierces (signées avec la même clé de signature pour garantir la sécurité)

---

## 2. Justification de l'usage HTTP

### 2.1 Nécessité technique
Malgré la migration progressive de l'industrie vers HTTPS, **de nombreux flux vidéo légitimes restent disponibles uniquement en HTTP** pour les raisons suivantes :

1. **Infrastructure IPTV legacy** : De nombreux opérateurs IPTV et diffuseurs utilisent encore des serveurs HTTP
2. **CDN non sécurisés** : Certains réseaux de distribution de contenu (CDN) anciens ne supportent que HTTP
3. **Flux locaux et privés** : Serveurs domestiques, NAS, caméras IP qui ne disposent pas de certificats SSL
4. **Diffusions événementielles** : Streams temporaires sans infrastructure HTTPS

### 2.2 Compatibilité avec l'écosystème
Les lecteurs vidéo leaders du marché acceptent HTTP :
- **VLC Media Player** (Google Play)
- **MX Player** (Google Play)
- **Kodi** (distribué via Play Store)
- **mpv** et autres lecteurs open-source

**Refuser HTTP rendrait STV Player incompatible avec ces flux légitimes**, réduisant drastiquement l'utilité pour les utilisateurs.

---

## 3. Sécurité et protection des données

### 3.1 Données sensibles - AUCUNE transmission en HTTP
STV Player **n'utilise JAMAIS HTTP** pour :
- ❌ Authentification utilisateur (login/password)
- ❌ Tokens ou sessions
- ❌ Données personnelles (nom, email, adresse)
- ❌ Informations de paiement
- ❌ Localisation précise
- ❌ APIs privées ou services backend

### 3.2 Usage strict de HTTP
HTTP est **exclusivement utilisé pour** :
- ✅ Lecture de flux vidéo publics fournis par l'utilisateur
- ✅ Chargement d'images publiques (logos de chaînes, miniatures)
- ✅ Fichiers multimédias locaux ou réseau local

### 3.3 Validation et sécurité
Les URLs sont validées avant utilisation :
```
- Schéma accepté : http:// ou https://
- Format validé via PlayerController
- Pas d'injection de code possible
- Pas de redirection non contrôlée
```

---

## 4. Configuration technique implémentée

### 4.1 AndroidManifest.xml
```xml
<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### 4.2 Network Security Config
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**Raison** : Configuration standard pour lecteurs vidéo générique permettant la lecture de flux HTTP tout en maintenant la sécurité système.

---

## 5. Conformité Play Store

### 5.1 Politique Google respectée
Selon les [directives Google Play](https://support.google.com/googleplay/android-developer/answer/9888076) :
- ✅ Cleartext autorisé si justifié
- ✅ Pas de transmission de données sensibles
- ✅ Usage transparent pour l'utilisateur

### 5.2 Catégorie d'application
**Lecteur vidéo & multimédia** - Catégorie pour laquelle HTTP est une nécessité reconnue.

### 5.3 Permissions déclarées
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
Aucune permission sensible (localisation, contacts, stockage externe) n'est demandée.

---

## 6. Expérience utilisateur

### 6.1 Transparence
L'utilisateur saisit manuellement les URLs des flux ou les reçoit via des applications catalogue de confiance.

### 6.2 Avertissement (optionnel)
Un message informatif peut être affiché si un flux HTTP est détecté :
> "⚠️ Ce flux utilise HTTP (non sécurisé). Préférez HTTPS si disponible."

### 6.3 Aucune collecte de données
- Pas d'analytics tiers
- Pas de tracking utilisateur
- Pas de partage de données

---

## 7. Alternatives évaluées et écartées

### 7.1 Option : Bloquer HTTP complètement
❌ **Rejetée** : Rendrait l'application inutilisable pour de nombreux flux légitimes

### 7.2 Option : Whitelist de domaines HTTP
❌ **Rejetée** : Impossible de prévoir tous les domaines utilisateurs (lecteur générique)

### 7.3 Option : Proxy HTTPS automatique
❌ **Rejetée** : Complexité technique élevée, latence accrue, coûts infrastructure

### 7.4 Option retenue : HTTP global avec usage contrôlé
✅ **Adoptée** : Standard de l'industrie, compatible avec écosystème existant

---

## 8. Engagement développeur

Nous nous engageons à :
1. ✅ Ne jamais transmettre de données sensibles en HTTP
2. ✅ Maintenir la validation stricte des URLs
3. ✅ Migrer vers HTTPS dès que possible pour nos propres services
4. ✅ Informer les utilisateurs des risques HTTP si pertinent
5. ✅ Respecter toutes les politiques Google Play Store

---

## 9. Documentation de référence

### 9.1 Code source concerné
- `app/src/main/java/com/example/stv/PlayerActivity.kt` - Lecture vidéo
- `app/src/main/java/com/example/stv/player/PlayerController.kt` - Validation URL
- `app/src/main/AndroidManifest.xml` - Configuration cleartext
- `app/src/main/res/xml/network_security_config.xml` - Sécurité réseau

### 9.2 Standards de l'industrie
- RFC 8216 - HTTP Live Streaming (HLS)
- ISO/IEC 23009-1 - MPEG-DASH
- Compatibilité VLC, MX Player, Kodi (applications approuvées Play Store)

---

## 10. Tests de sécurité effectués

### 10.1 Audit statique
✅ Aucune transmission de données sensibles en HTTP détectée  
✅ Aucune API privée en HTTP  
✅ Validation URL stricte implémentée

### 10.2 Tests fonctionnels
✅ Flux HTTPS : Fonctionne  
✅ Flux HTTP : Fonctionne (nécessaire)  
✅ URLs malformées : Rejetées  
✅ Injection de code : Impossible

---

## 11. Contact et support

**Développeur** : [Votre nom]  
**Email** : [Votre email]  
**Date de soumission** : 27 février 2026  
**Version STV Player** : 1.0

---

## Conclusion

L'autorisation du trafic HTTP dans STV Player est une **nécessité technique justifiée** pour garantir la compatibilité avec l'écosystème existant des flux vidéo streaming. Cette configuration suit les **standards de l'industrie** et respecte strictement les **politiques Google Play Store** en matière de sécurité et de protection des données utilisateur.

**Aucune donnée sensible n'est transmise en HTTP**, et l'usage est strictement limité à la lecture de contenu multimédia public.

---

**Document préparé pour soumission Google Play Store - Prêt à l'emploi**

