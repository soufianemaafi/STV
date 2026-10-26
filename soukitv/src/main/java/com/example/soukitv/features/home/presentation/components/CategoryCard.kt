package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
        Icon(
            imageVector = categoryIcon(category.name),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** Icône vectorielle épurée selon le genre de la catégorie. */
internal fun categoryIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        "maghreb" in lower || "maroc" in lower || "égypte" in lower || "egypte" in lower ||
            "émirat" in lower || "emirat" in lower || "uae" in lower || "afghanistan" in lower -> Icons.Filled.Flag
        "actualité" in lower || "actualite" in lower || "news" in lower || "bfm" in lower ||
            "أخبار" in lower -> Icons.Filled.Newspaper
        "sport" in lower || "رياض" in lower -> Icons.Filled.SportsSoccer
        "cinéma" in lower || "cinema" in lower || "série" in lower || "film" in lower ||
            "أفلام" in lower -> Icons.Filled.Movie
        "jeunesse" in lower || "kid" in lower || "enfant" in lower || "أطفال" in lower -> Icons.Filled.ChildCare
        "religion" in lower || "spirituel" in lower || "culture" in lower || "quran" in lower ||
            "قرآن" in lower -> Icons.Filled.MenuBook
        "musique" in lower || "radio" in lower || "music" in lower -> Icons.Filled.MusicNote
        "science" in lower || "docu" in lower -> Icons.Filled.Science
        "international" in lower -> Icons.Filled.Public
        else -> Icons.Filled.Tv
    }
}

