package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.soukitv.R
import com.example.soukitv.ui.theme.SoukiDarkSurface
import com.example.soukitv.ui.theme.SoukiDarkSurfaceVariant
import com.example.soukitv.ui.theme.SoukiDarkBorder

private val CardSurfaceColor = Color(0xFF16161C)
private val TitleColor = Color(0xFFF2F2F8)
private val CountTextColor = Color(0xFFB5B5C2)

/**
 * Carte commune categorie/pays en format bandeau cinema dark.
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
            .height(80.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
        border = BorderStroke(0.5.dp, SoukiDarkBorder)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SoukiDarkSurfaceVariant.copy(alpha = 0.45f), SoukiDarkSurface)
                    )
                )
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(R.plurals.channel_count, channelCount, channelCount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = CountTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = CountTextColor,
                modifier = Modifier.width(10.dp)
            )
        }
    }
}

