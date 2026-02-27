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

class TermsOfServiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            STVTheme {
                TermsOfServiceScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conditions d'Utilisation") },
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
                    "Conditions d'Utilisation de STV",
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

                SectionTitle("1. Acceptation des Conditions")
                SectionText(
                    "En utilisant STV, vous acceptez ces conditions d'utilisation. " +
                    "Si vous n'acceptez pas ces conditions, veuillez ne pas utiliser l'application."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("2. Description du Service")
                SectionText(
                    "STV est un lecteur vidéo permettant de lire des flux streaming " +
                    "provenant de diverses sources. L'application agit uniquement comme " +
                    "un lecteur et ne fournit pas de contenu vidéo."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("3. Utilisation Autorisée")
                SectionText(
                    "Vous vous engagez à :\n" +
                    "• Utiliser l'application légalement\n" +
                    "• Ne pas contourner les mesures de sécurité\n" +
                    "• Respecter les droits d'auteur du contenu visionné\n" +
                    "• Ne pas utiliser l'app pour des activités illégales"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("4. Contenu Tiers et Clause Anti-Piratage")
                SectionText(
                    "**4.1 Responsabilité :**\n" +
                    "STV lit du contenu externe. Nous ne sommes PAS responsables de :\n" +
                    "• Disponibilité du contenu\n" +
                    "• Légalité du contenu\n" +
                    "• Qualité du contenu\n\n" +
                    "**4.2 Votre responsabilité :**\n" +
                    "VOUS êtes responsable du contenu que vous choisissez de visionner.\n\n" +
                    "**4.3 Anti-Piratage :**\n" +
                    "STV est un outil neutre. Utilisation pour contenu illégal :\n" +
                    "• Vous violez ces conditions\n" +
                    "• Vous êtes passible de poursuites\n" +
                    "• Nous coopérerons avec autorités\n" +
                    "• Nous ne facilitons PAS le piratage"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("5. Publicité et Monétisation")
                SectionText(
                    "**5.1 Financement :**\n" +
                    "L'app est gratuite, financée par publicités Google AdMob.\n\n" +
                    "**En utilisant l'app, vous acceptez :**\n" +
                    "• De voir des pubs interstitielles\n" +
                    "• De voir des pubs bannière\n" +
                    "• Que ces pubs peuvent être personnalisées\n\n" +
                    "**5.2 Blocage publicités :**\n" +
                    "Tentatives de bloquer les pubs peuvent entraîner :\n" +
                    "• Limitation de l'accès\n" +
                    "• Avertissement\n" +
                    "• Suspension du service"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("6. Propriété Intellectuelle")
                SectionText(
                    "STV Player, son code, design et logo sont protégés par droits d'auteur.\n\n" +
                    "© 2026. Tous droits réservés.\n\n" +
                    "Toute copie, modification ou distribution est interdite sans autorisation écrite."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("7. Limitation de Responsabilité")
                SectionText(
                    "**7.1 \"EN L'ÉTAT\" :**\n" +
                    "L'application est fournie \"EN L'ÉTAT\".\n\n" +
                    "**Nous ne garantissons PAS :**\n" +
                    "• Disponibilité continue\n" +
                    "• Absence d'erreurs\n" +
                    "• Compatibilité complète\n\n" +
                    "**7.2 Exclusion dommages :**\n" +
                    "Nous ne sommes PAS responsables des dommages :\n" +
                    "• Directs\n" +
                    "• Indirects\n" +
                    "• Punitifs\n\n" +
                    "**Montant max : 0 €** (service gratuit)"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("8. Indemnisation")
                SectionText(
                    "Vous acceptez d'indemniser le développeur contre toute réclamation :\n\n" +
                    "• Résultant de votre utilisation\n" +
                    "• Violation de conditions par vous\n" +
                    "• Violation de droits tiers\n\n" +
                    "Cela inclut frais juridiques."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("9. Suspension et Résiliation")
                SectionText(
                    "Nous pouvons suspendre votre accès si :\n\n" +
                    "• Violation de ces conditions\n" +
                    "• Utilisation abusive de l'app\n" +
                    "• Obligation légale\n\n" +
                    "Vous pouvez cesser l'utilisation à tout moment en désinstallant l'app."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("10. Modifications des Conditions")
                SectionText(
                    "Nous pouvons modifier ces conditions à tout moment.\n\n" +
                    "Changements majeurs seront notifiés dans l'app.\n\n" +
                    "Continuer à utiliser = acceptation des modifications."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("11. Loi Applicable et Contact")
                SectionText(
                    "**Loi applicable :**\n" +
                    "Ces conditions sont régies par votre juridiction.\n\n" +
                    "**Contact :**\n" +
                    "Email : support@example.com\n\n" +
                    "**Effective depuis :**\n" +
                    "27 février 2026"
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
