package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.soukitv.R
import com.example.soukitv.ui.theme.SoukiDarkBorder
import com.example.soukitv.ui.theme.SoukiRed

private val IconContainerColor = Color(0xFF1E1E26)

/** Conteneur squircle 12dp, fond surélevé, bordure fine et lueur rouge très douce. */
@Composable
fun GlowIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(IconContainerColor)
            .background(
                Brush.radialGradient(
                    colors = listOf(SoukiRed.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            .border(0.5.dp, SoukiDarkBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Carte commune Catégorie / Pays : même langage visuel que [ChannelTile]
 * (surface charbon, coins 16dp, bordure 0.5dp, ripple Material).
 */
@Composable
fun SoukiInfoCard(
    title: String,
    channelCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowIconContainer { leading() }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.channel_count,
                            channelCount,
                            channelCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

