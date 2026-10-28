package com.example.soukitv.features.home.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.ui.theme.SoukiCrimson
import com.example.soukitv.ui.theme.SoukiDarkBorder
import com.example.soukitv.ui.theme.SoukiDarkSurface
import com.example.soukitv.ui.theme.SoukiDarkSurfaceVariant

/**
 * Carte "Scoreboard" : équipes, heure, badge EN DIRECT pulsant et une ligne d'infos unique
 * (commentateur à gauche, compétition au centre, chaîne à droite).
 */
@Composable
fun MatchCard(
    match: MatchItem,
    isLive: Boolean,
    onMatchClick: (MatchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isLive) SoukiCrimson.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline

    Card(
        onClick = { onMatchClick(match) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(SoukiDarkSurfaceVariant, SoukiDarkSurface)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamBadge(
                    name = match.teamA.orEmpty(),
                    logoUrl = match.teamALogo.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
                ScoreboardCenter(
                    time = match.time ?: "--:--",
                    isLive = isLive,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TeamBadge(
                    name = match.teamB.orEmpty(),
                    logoUrl = match.teamBLogo.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }

            MatchInfoRow(match)
        }
    }
}

/** Ligne unique : commentateur (start) · compétition (center) · chaîne (end). */
@Composable
private fun MatchInfoRow(match: MatchItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            InfoText(Icons.Default.Mic, match.commentator, TextAlign.Start)
        }
        val tournament = match.tournament
        if (!tournament.isNullOrBlank()) {
            Text(
                text = tournament,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SoukiDarkSurface.copy(alpha = 0.85f))
                    .border(0.5.dp, SoukiDarkBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            InfoText(Icons.Default.Tv, match.channelName, TextAlign.End)
        }
    }
}

@Composable
private fun InfoText(icon: ImageVector, text: String?, align: TextAlign) {
    if (text.isNullOrBlank()) return
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Badge "EN DIRECT" avec point rouge pulsant via [rememberInfiniteTransition]. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SoukiCrimson.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(pulse)
                .background(SoukiCrimson, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "EN DIRECT",
            color = SoukiCrimson,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun ScoreboardCenter(time: String, isLive: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLive) LiveBadge()
        Text(
            text = time,
            color = if (isLive) SoukiCrimson else MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "VS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun TeamBadge(
    name: String,
    logoUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            SoukiLogoImage(
                url = logoUrl,
                contentDescription = name,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

