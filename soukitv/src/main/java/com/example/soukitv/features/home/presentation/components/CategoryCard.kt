package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import com.example.soukitv.R
import com.example.soukitv.features.home.domain.model.Category

@Composable
fun CategoryCard(
    category: Category,
    onCategoryClick: (Category) -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    val channelCount = category.channels.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clickable { onCategoryClick(category) },
        shape = cardShape,
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = getCategoryEmoji(category.name),
                    fontSize = 24.sp
                )
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
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getCategoryEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "maghreb" in lower || "maroc" in lower -> "🇲🇦"
        "égypte" in lower || "egypte" in lower -> "🇪🇬"
        "émirat" in lower || "emirat" in lower || "uae" in lower -> "🇦🇪"
        "afghanistan" in lower -> "🇦🇫"
        "actualité" in lower || "news" in lower || "bfm" in lower -> "📰"
        "sport" in lower -> "⚽"
        "cinéma" in lower || "cinema" in lower || "série" in lower || "film" in lower -> "🎬"
        "jeunesse" in lower || "kid" in lower || "enfant" in lower -> "👶"
        "religion" in lower || "spirituel" in lower || "culture" in lower || "quran" in lower -> "🕌"
        "musique" in lower || "radio" in lower -> "📻"
        "science" in lower || "docu" in lower -> "🔬"
        else -> "📺"
    }
}

