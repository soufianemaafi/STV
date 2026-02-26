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
                    "Nous ne vendons pas vos données personnelles. Nous partageons uniquement :\n" +
                    "• Avec Google AdMob pour la diffusion de publicités\n" +
                    "• Données anonymisées pour analyses statistiques\n" +
                    "• Si requis par la loi"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("5. Sécurité")
                SectionText(
                    "Nous mettons en œuvre des mesures de sécurité pour protéger vos données " +
                    "contre tout accès non autorisé, altération ou destruction."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("6. Vos Droits")
                SectionText(
                    "Vous avez le droit de :\n" +
                    "• Accéder à vos données personnelles\n" +
                    "• Demander la suppression de vos données\n" +
                    "• Refuser certaines collectes de données\n" +
                    "• Désactiver les publicités personnalisées"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("7. Cookies et Technologies Similaires")
                SectionText(
                    "Nous utilisons des technologies de suivi pour améliorer votre expérience " +
                    "et diffuser des publicités pertinentes."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("8. Modifications")
                SectionText(
                    "Nous pouvons modifier cette politique à tout moment. Les changements " +
                    "seront publiés dans cette page."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("9. Contact")
                SectionText(
                    "Pour toute question concernant cette politique, contactez-nous via les paramètres de l'application."
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

