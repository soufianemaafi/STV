package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import com.example.soukitv.R
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.ui.theme.DarkSurface
import com.example.soukitv.ui.theme.GrayText
import com.example.soukitv.ui.theme.RedPrimary

@Composable
fun CategoryCard(
    category: Category,
    onCategoryClick: (Category) -> Unit
) {
    val cardShape = RoundedCornerShape(6.dp)
    val channelCount = category.channels.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, cardShape)
            .border(1.dp, RedPrimary.copy(alpha = 0.6f), cardShape)
            .clickable { onCategoryClick(category) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pluralStringResource(R.plurals.channel_count, channelCount, channelCount),
            style = MaterialTheme.typography.labelMedium,
            color = GrayText
        )
    }
}

