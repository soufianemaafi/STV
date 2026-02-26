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

                SectionTitle("4. Contenu Tiers")
                SectionText(
                    "STV lit du contenu provenant de sources externes. Nous ne sommes pas " +
                    "responsables du contenu, de sa disponibilité ou de sa légalité. " +
                    "Vous êtes responsable du contenu que vous choisissez de visionner."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("5. Publicité")
                SectionText(
                    "L'application affiche des publicités via Google AdMob. " +
                    "En utilisant l'app, vous acceptez de voir ces publicités. " +
                    "Toute tentative de bloquer les publicités peut entraîner " +
                    "une limitation de l'accès au service."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("6. Propriété Intellectuelle")
                SectionText(
                    "STV, son code source, son design et ses fonctionnalités sont " +
                    "protégés par les droits d'auteur. Toute copie, modification ou " +
                    "distribution non autorisée est interdite."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("7. Limitation de Responsabilité")
                SectionText(
                    "L'application est fournie \"en l'état\". Nous ne garantissons pas :\n" +
                    "• La disponibilité continue du service\n" +
                    "• L'absence d'erreurs ou de bugs\n" +
                    "• La compatibilité avec tous les appareils\n" +
                    "• La qualité du contenu tiers"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("8. Suspension et Résiliation")
                SectionText(
                    "Nous nous réservons le droit de suspendre ou de résilier votre accès " +
                    "si vous violez ces conditions ou utilisez l'application de manière abusive."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("9. Modifications des Conditions")
                SectionText(
                    "Nous pouvons modifier ces conditions à tout moment. Les changements " +
                    "seront effectifs dès leur publication dans l'application."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("10. Loi Applicable")
                SectionText(
                    "Ces conditions sont régies par les lois en vigueur dans votre juridiction."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("11. Contact")
                SectionText(
                    "Pour toute question concernant ces conditions, contactez-nous via les paramètres de l'application."
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

