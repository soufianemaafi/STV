package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val code = countryCode(country)

    SoukiInfoCard(
        title = country,
        channelCount = channelCount,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            if (code != null) {
                SoukiLogoImage(
                    url = "https://flagcdn.com/w80/${code}.png",
                    contentDescription = country,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = getCountryFlag(country),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun countryCode(country: String): String? {
    val lower = country.lowercase()
    return when {
        "maroc" in lower || "morocco" in lower -> "ma"
        "egypte" in lower || "égypte" in lower || "egypt" in lower -> "eg"
        "algerie" in lower || "algérie" in lower || "algeria" in lower -> "dz"
        "tunisie" in lower || "tunisia" in lower -> "tn"
        "arabie" in lower || "saudi" in lower -> "sa"
        "emirat" in lower || "émirat" in lower || "uae" in lower -> "ae"
        "qatar" in lower -> "qa"
        "france" in lower -> "fr"
        "allemagne" in lower || "germany" in lower -> "de"
        "russie" in lower || "russia" in lower -> "ru"
        "afghanistan" in lower -> "af"
        "jordanie" in lower || "jordan" in lower -> "jo"
        else -> null
    }
}

