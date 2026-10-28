package com.example.soukitv.features.home.presentation.components
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.ui.theme.SoukiRed
import kotlinx.coroutines.launch
/** Carte chaîne épurée : logo, nom en gras et bouton favori avec micro-animation d'échelle. */
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.3f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                ) {
                    SoukiLogoImage(
                        url = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = { onToggleFavorite(channel) },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
/**
 * Bouton cœur : AUCUN fond, AUCUNE forme (ni cercle ni carré).
 * Un fin halo sombre derrière l'icône la détache de n'importe quel logo.
 * Rebond d'échelle à chaque clic.
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
        modifier = modifier.size(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            // Halo : même icône, légèrement plus grande, sombre et translucide.
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FavoriteHalo,
                modifier = Modifier.size(25.dp)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFavorite) FavoriteActive else FavoriteInactive,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}
/** Contour sombre discret autour de l'icône (pas un fond). */
private val FavoriteHalo = Color.Black.copy(alpha = 0.45f)
/** Favori actif : rouge Cinema (#E50914). */
private val FavoriteActive = SoukiRed
/** Favori inactif : gris élégant (#A1A1AA). */
private val FavoriteInactive = Color(0xFFA1A1AA)