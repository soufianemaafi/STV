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
                title = { Text("Terms of Service") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                    "STV Terms of Service",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Last updated: ${java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("1. Acceptance of Terms")
                SectionText(
                    "By using STV, you accept these terms of use. " +
                    "If you do not accept these terms, please do not use the application."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("2. Service Description")
                SectionText(
                    "STV is a video player for playing streaming feeds " +
                    "from various sources. The application only acts as " +
                    "a player and does not provide video content."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("3. Authorized Use")
                SectionText(
                    "You agree to:\n" +
                    "• Use the application legally\n" +
                    "• Not bypass security measures\n" +
                    "• Respect the copyright of viewed content\n" +
                    "• Not use the app for illegal activities"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("4. Third-Party Content and Anti-Piracy Clause")
                SectionText(
                    "**4.1 Responsibility:**\n" +
                    "STV plays external content. We are NOT responsible for:\n" +
                    "• Content availability\n" +
                    "• Content legality\n" +
                    "• Content quality\n\n" +
                    "**4.2 Your responsibility:**\n" +
                    "YOU are responsible for the content you choose to view.\n\n" +
                    "**4.3 Anti-Piracy:**\n" +
                    "STV is a neutral tool. Use for illegal content:\n" +
                    "• You violate these terms\n" +
                    "• You may face legal action\n" +
                    "• We will cooperate with authorities\n" +
                    "• We do NOT facilitate piracy"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("5. Advertising and Monetization")
                SectionText(
                    "**5.1 Funding:**\n" +
                    "The app is free, funded by Google AdMob ads.\n\n" +
                    "**By using the app, you accept:**\n" +
                    "• Seeing interstitial ads\n" +
                    "• Seeing banner ads\n" +
                    "• That these ads may be personalized\n\n" +
                    "**5.2 Ad blocking:**\n" +
                    "Attempts to block ads may result in:\n" +
                    "• Access limitation\n" +
                    "• Warning\n" +
                    "• Service suspension"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("6. Intellectual Property")
                SectionText(
                    "STV Player, its code, design and logo are protected by copyright.\n\n" +
                    "© 2026. All rights reserved.\n\n" +
                    "Any copying, modification or distribution is prohibited without written permission."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("7. Limitation of Liability")
                SectionText(
                    "**7.1 \"AS IS\":**\n" +
                    "The application is provided \"AS IS\".\n\n" +
                    "**We do NOT guarantee:**\n" +
                    "• Continuous availability\n" +
                    "• Absence of errors\n" +
                    "• Full compatibility\n\n" +
                    "**7.2 Exclusion of damages:**\n" +
                    "We are NOT responsible for damages:\n" +
                    "• Direct\n" +
                    "• Indirect\n" +
                    "• Punitive\n\n" +
                    "**Max amount: €0** (free service)"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("8. Indemnification")
                SectionText(
                    "You agree to indemnify the developer against any claims:\n\n" +
                    "• Resulting from your use\n" +
                    "• Your breach of these terms\n" +
                    "• Violation of third-party rights\n\n" +
                    "This includes legal fees."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("9. Suspension and Termination")
                SectionText(
                    "We may suspend your access if:\n\n" +
                    "• Breach of these terms\n" +
                    "• Abusive use of the app\n" +
                    "• Legal obligation\n\n" +
                    "You may cease use at any time by uninstalling the app."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("10. Modifications to Terms")
                SectionText(
                    "We may modify these terms at any time.\n\n" +
                    "Major changes will be notified in the app.\n\n" +
                    "Continuing to use = acceptance of modifications."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("11. Applicable Law and Contact")
                SectionText(
                    "**Applicable Law:**\n" +
                    "These terms are governed by your jurisdiction.\n\n" +
                    "**Contact:**\n" +
                    "Email: support@example.com\n\n" +
                    "**Effective since:**\n" +
                    "February 27, 2026"
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
