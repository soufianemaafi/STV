package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.soukitv.features.home.domain.model.Category

@Composable
fun CategoryCard(
    category: Category,
    onCategoryClick: (Category) -> Unit
) {
    SoukiInfoCard(
        title = category.name,
        channelCount = category.channels.size,
        onClick = { onCategoryClick(category) }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(category.name),
                contentDescription = null,
                tint = Color(0xFFF2F2F8),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/** Icône vectorielle épurée selon le genre de la catégorie. */
internal fun categoryIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        "actualité" in lower || "actualite" in lower || "news" in lower || "bfm" in lower ||
            "أخبار" in lower -> Icons.Rounded.Newspaper
        "sport" in lower || "رياض" in lower -> Icons.Rounded.SportsSoccer
        "cinéma" in lower || "cinema" in lower || "série" in lower || "film" in lower ||
            "أفلام" in lower -> Icons.Rounded.Movie
        "jeunesse" in lower || "kid" in lower || "enfant" in lower || "أطفال" in lower -> Icons.Rounded.ChildCare
        "religion" in lower || "spirituel" in lower || "culture" in lower || "quran" in lower ||
            "قرآن" in lower -> Icons.AutoMirrored.Rounded.MenuBook
        "musique" in lower || "radio" in lower || "music" in lower -> Icons.Rounded.MusicNote
        "science" in lower || "docu" in lower -> Icons.Rounded.Science
        "international" in lower -> Icons.Rounded.Public
        "tv" in lower || "live" in lower -> Icons.Rounded.PlayArrow
        "maghreb" in lower || "maroc" in lower || "égypte" in lower || "egypte" in lower ||
            "émirat" in lower || "emirat" in lower || "uae" in lower || "afghanistan" in lower -> Icons.Rounded.Flag
        else -> Icons.Rounded.Category
    }
}

