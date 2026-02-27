package com.example.stv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stv.ui.theme.STVTheme

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            STVTheme {
                PrivacyPolicyScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Politique de Confidentialité") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Politique de Confidentialité de STV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Dernière mise à jour : ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("1. Introduction")
                SectionText(
                    "STV (\"nous\", \"notre\" ou \"l'application\") respecte votre vie privée. " +
                    "Cette politique explique comment nous collectons, utilisons et protégeons vos informations."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("2. Informations Collectées")
                SectionText(
                    "• Données d'utilisation : URLs de vidéos lues, historique de lecture\n" +
                    "• Données techniques : type d'appareil, version Android, performances\n" +
                    "• Publicités : identifiants publicitaires pour personnaliser les annonces"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("3. Utilisation des Données")
                SectionText(
                    "Nous utilisons vos données pour :\n" +
                    "• Améliorer l'expérience utilisateur\n" +
                    "• Afficher des publicités pertinentes via Google AdMob\n" +
                    "• Analyser les performances de l'application\n" +
                    "• Résoudre les problèmes techniques"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("4. Partage des Données")
                SectionText(
                    "**Nous ne vendons PAS vos données personnelles.**\n\n" +
                    "Nous partageons uniquement avec :\n\n" +
                    "**4.1 Google AdMob (Publicités) :**\n" +
                    "• Identifiant publicitaire, appareil, IP\n" +
                    "• Raison : Afficher publicités\n" +
                    "• Contrôle : Désactivable (paramètres Android)\n\n" +
                    "**4.2 Google Play Services :**\n" +
                    "• Données techniques de l'app\n" +
                    "• Raison : Distribution et mises à jour\n\n" +
                    "**4.3 Autorités légales :**\n" +
                    "• Si requis par la loi\n\n" +
                    "**Aucun autre partage commercial.**"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("5. Stockage et Sécurité")
                SectionText(
                    "**5.1 Où sont vos données :**\n" +
                    "• Localement : Liste vidéos (appareil Android)\n" +
                    "• Cloud Google : Données AdMob (serveurs Google)\n" +
                    "• Backup Android : Optionnel (désactivable)\n\n" +
                    "**5.2 Durée de conservation :**\n" +
                    "• Liste vidéos : Jusqu'à désinstallation\n" +
                    "• Données AdMob : 60-90 jours (politique Google)\n\n" +
                    "**5.3 Mesures de sécurité :**\n" +
                    "• Transmission HTTPS (quand disponible)\n" +
                    "• Stockage local sécurisé Android\n" +
                    "• Validation stricte URLs\n" +
                    "• Pas de transmission données sensibles\n\n" +
                    "**Note** : STV accepte HTTP pour flux vidéo (compatibilité). Aucune donnée sensible via HTTP."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("6. Vos Droits (RGPD et CCPA)")
                SectionText(
                    "**6.1 Droit d'accès :**\n" +
                    "Savoir quelles données nous avons sur vous.\n\n" +
                    "**6.2 Droit de rectification :**\n" +
                    "Corriger données inexactes.\n\n" +
                    "**6.3 Droit de suppression (\"oubli\") :**\n" +
                    "Supprimer vos données.\n\n" +
                    "**6.4 Droit d'opposition :**\n" +
                    "Refuser certaines collectes.\n\n" +
                    "**6.5 Droit à la portabilité :**\n" +
                    "Copie de vos données format lisible.\n\n" +
                    "**6.6 CCPA (Californie) :**\n" +
                    "Nous ne vendons PAS vos données.\n\n" +
                    "**Délais de réponse :**\n" +
                    "• RGPD (UE) : 30 jours max\n" +
                    "• CCPA (CA) : 45 jours max"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("7. Données des Enfants (COPPA Compliance)")
                SectionText(
                    "**7.1 Âge minimum :**\n" +
                    "STV Player n'est PAS destiné aux enfants de moins de 13 ans.\n\n" +
                    "**7.2 Collecte de données d'enfants :**\n" +
                    "Nous ne collectons PAS sciemment de données d'enfants de moins de 13 ans.\n\n" +
                    "**7.3 Si vous êtes parent :**\n" +
                    "Si votre enfant nous a fourni des données sans votre consentement :\n" +
                    "• Contactez immédiatement : support@example.com\n" +
                    "• Nous supprimerons ces données sous 48 heures"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("8. Transferts Internationaux de Données")
                SectionText(
                    "**8.1 Localisation serveurs :**\n" +
                    "• Données locales : Sur votre appareil\n" +
                    "• Données AdMob : Serveurs Google (États-Unis, UE)\n\n" +
                    "**8.2 Protection transferts :**\n" +
                    "Les transferts vers États-Unis sont protégés par :\n" +
                    "• Standard Contractual Clauses (SCC)\n" +
                    "• Politique de confidentialité Google\n" +
                    "• Conformité RGPD"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("9. Cookies et Technologies de Suivi")
                SectionText(
                    "**9.1 Technologies utilisées :**\n" +
                    "• Identifiant publicitaire Google (GAID)\n" +
                    "• SharedPreferences Android (stockage local)\n" +
                    "• Pas de cookies web (app native)\n\n" +
                    "**9.2 Désactiver le suivi :**\n" +
                    "Paramètres Android → Google → Publicités → Désactiver personnalisation"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("10. Backup et Sauvegarde Automatique")
                SectionText(
                    "**10.1 Backup automatique Android :**\n" +
                    "Android peut sauvegarder vos données (liste vidéos) sur Google Drive.\n\n" +
                    "**Ce qui est sauvegardé :**\n" +
                    "• Liste de vos flux vidéo\n" +
                    "• Préférences de l'application\n\n" +
                    "**10.2 Contrôler le backup :**\n" +
                    "Paramètres Android → Google → Backup (désactivable)"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("11. Suppression de Vos Données")
                SectionText(
                    "**11.1 Suppression données locales :**\n" +
                    "Désinstaller STV Player supprime :\n" +
                    "• Toute la liste de vidéos\n" +
                    "• Toutes les préférences\n" +
                    "• Tout le cache\n\n" +
                    "**11.2 Suppression données AdMob :**\n" +
                    "Contactez-nous : support@example.com\n" +
                    "• Délai : 30-60 jours (politique Google)"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("12. Modifications de cette Politique")
                SectionText(
                    "**12.1 Droit de modification :**\n" +
                    "Nous pouvons modifier cette politique à tout moment.\n\n" +
                    "**12.2 Notification changements :**\n" +
                    "• Mise à jour de la date en haut\n" +
                    "• Notification dans l'app (si majeur)\n\n" +
                    "**12.3 Acceptation :**\n" +
                    "Continuer à utiliser l'app après modification = acceptation."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("13. Liens vers Sites Tiers")
                SectionText(
                    "STV Player peut afficher du contenu provenant de sites web tiers.\n\n" +
                    "**Important :**\n" +
                    "• Nous ne contrôlons PAS ces sites tiers\n" +
                    "• Nous ne sommes PAS responsables de leur politique\n" +
                    "• Consultez la politique de chaque site"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("14. Contact et Questions")
                SectionText(
                    "**Pour toute question concernant cette politique :**\n\n" +
                    "Email : support@example.com\n" +
                    "Application : STV Player\n" +
                    "Package : com.example.stv\n" +
                    "Version : 1.0\n\n" +
                    "**Délais de réponse :**\n" +
                    "• RGPD (UE) : 30 jours maximum\n" +
                    "• CCPA (CA) : 45 jours maximum\n" +
                    "• Autres : 60 jours maximum"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("15. Loi Applicable et Juridiction")
                SectionText(
                    "Cette politique est régie par :\n\n" +
                    "• RGPD (Union Européenne)\n" +
                    "• CCPA (Californie, États-Unis)\n" +
                    "• COPPA (Protection enfants, États-Unis)\n\n" +
                    "En cas de litige, les tribunaux compétents seront determinés selon votre juridiction."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("16. Consentement Explicite")
                SectionText(
                    "**En utilisant STV Player, vous consentez à :**\n\n" +
                    "• La collecte de données décrite ici\n" +
                    "• L'utilisation pour les finalités décrites\n" +
                    "• Le partage avec Google AdMob\n\n" +
                    "**Retrait du consentement :**\n\n" +
                    "• Publicités personnalisées : Paramètres Android\n" +
                    "• Toutes données : Désinstaller l'app\n" +
                    "• Demande spécifique : support@example.com\n\n" +
                    "**Effective depuis : 27 février 2026**"
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

