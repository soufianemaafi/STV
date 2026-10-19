package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.pluralStringResource
import com.example.soukitv.R

fun getCountryFlag(country: String): String {
    val lower = country.lowercase()
    return when {
        "maroc" in lower || "morocco" in lower -> "🇲🇦"
        "égypte" in lower || "egypt" in lower -> "🇪🇬"
        "algérie" in lower || "algeria" in lower -> "🇩🇿"
        "tunisie" in lower || "tunisia" in lower -> "🇹🇳"
        "arabie" in lower || "saudi" in lower -> "🇸🇦"
        "émirat" in lower || "uae" in lower -> "🇦🇪"
        "qatar" in lower -> "🇶🇦"
        "france" in lower -> "🇫🇷"
        "allemagne" in lower || "germany" in lower -> "🇩🇪"
        "russie" in lower || "russia" in lower -> "🇷🇺"
        "afghanistan" in lower -> "🇦🇫"
        "jordanie" in lower || "jordan" in lower -> "🇯🇴"
        else -> "🌍"
    }
}

@Composable
fun CountryCard(
    country: String,
    channelCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = getCountryFlag(country), fontSize = 24.sp)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.channel_count,
                            channelCount,
                            channelCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = country,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

