package com.example.soukitv.features.home.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

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
    SoukiInfoCard(
        title = country,
        channelCount = channelCount,
        onClick = onClick
    ) {
        // Le drapeau (emoji) est rendu à taille fixe : jamais déformé.
        Text(text = getCountryFlag(country), fontSize = 26.sp)
    }
}

