package com.soufcom.soukitv.features.home.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soufcom.soukitv.features.home.domain.model.Channel
import kotlinx.coroutines.launch

private val ChannelCardColor = Color(0xFF161616)
private val ChannelLogoColor = Color(0xFF202020)
private val ChannelFooterColor = Color(0xFF161616)
private val ChannelBorderColor = Color(0xFF333333)
private val ChannelCardShape = RoundedCornerShape(10.dp)
private val FavoriteAccent = Color(0xFFC41E3A)
private val FavoriteInactive = Color(0xFF808080)

/** Carte chaîne premium : logo dégagé, titre lisible et favori dans le footer. */
@Composable
fun ChannelTile(
    channel: Channel,
    isFavorite: Boolean,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onChannelClick(channel) },
        modifier = modifier
            .fillMaxWidth()
            .height(136.dp),
        shape = ChannelCardShape,
        colors = CardDefaults.cardColors(containerColor = ChannelCardColor),
        border = BorderStroke(1.dp, ChannelBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ChannelLogoColor)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                SoukiLogoImage(
                    url = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFF2E2E2E)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChannelFooterColor)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channel.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = { onToggleFavorite(channel) },
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
/**
 * Bouton cœur compact du footer avec rebond d'échelle à chaque clic.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    IconButton(
        onClick = {
            onClick()
            scope.launch {
                scale.snapTo(0.6f)
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        },
        modifier = modifier.size(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFavorite) FavoriteAccent else FavoriteInactive,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
