package com.example.soukitv.features.home.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.soukitv.ui.theme.SoukiDarkBorder
import com.example.soukitv.ui.theme.SoukiDarkSurface
import com.example.soukitv.ui.theme.SoukiDarkSurfaceVariant

private val ShimmerBase = SoukiDarkSurfaceVariant
private val ShimmerHighlight = SoukiDarkBorder

/** Applique un balayage lumineux animé (gradient linéaire) sur le composant. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    val travel = 1600f
    val start = -travel + progress * travel * 2f
    val brush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(start, 0f),
        end = Offset(start + travel, travel / 2f)
    )
    this
        .clip(shape)
        .background(brush)
}

/** Bloc squelette générique. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.shimmer(shape))
}

@Composable
private fun MatchCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoukiDarkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(Modifier.size(width = 96.dp, height = 14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(Modifier.size(52.dp), CircleShape)
            ShimmerBox(Modifier.size(width = 64.dp, height = 28.dp), RoundedCornerShape(14.dp))
            ShimmerBox(Modifier.size(52.dp), CircleShape)
        }
        ShimmerBox(Modifier.fillMaxWidth().height(12.dp))
    }
}

@Composable
private fun ChannelCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoukiDarkSurface)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1.3f), RoundedCornerShape(10.dp))
        Spacer(Modifier.height(8.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.7f).height(12.dp))
    }
}

/** Écran de chargement : fausses cartes de matchs + grille de chaînes. */
@Composable
fun HomeShimmerSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(52.dp),
            RoundedCornerShape(26.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(List(5) { it }) {
                ShimmerBox(Modifier.size(width = 72.dp, height = 32.dp), RoundedCornerShape(16.dp))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(2) { MatchCardSkeleton() }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(List(6) { it }) { ChannelCardSkeleton() }
                }
            }
        }
    }
}

