package com.example.stv.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.stv.R
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Overlay des contrôles du player (play/pause, seek, PiP, qualité, etc.).
 * Apparaît avec fadeIn/fadeOut et disparaît automatiquement après 3s.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    isLive: Boolean = false,
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    resizeMode: Int,
    onBackClick: () -> Unit,
    onRewindClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onPipClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // Bouton Retour en haut à gauche
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // ✅ Badge LIVE en haut à droite (visible uniquement pour les flux en direct)
            if (isLive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Red,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, shape = CircleShape)
                        )
                        Text(
                            text = stringResource(R.string.live_badge),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Zone du bas (Contrôles complets)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
            ) {
                // Boutons principaux alignés en bas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind -10s
                        IconButton(onClick = onRewindClick) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = stringResource(R.string.rewind_10s),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause_button) else stringResource(R.string.play_stream_button),
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Forward +10s
                        IconButton(onClick = onForwardClick) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = stringResource(R.string.forward_10s),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }


                        // Aspect Ratio (Redimensionnement)
                        IconButton(onClick = onResizeClick) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = stringResource(R.string.format_content_description),
                                tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) Color.Red else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Picture in Picture (PiP)
                        IconButton(onClick = onPipClick) {
                            Icon(
                                imageVector = Icons.Filled.PictureInPicture,
                                contentDescription = stringResource(R.string.pip_button),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Settings (Qualité)
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.quality_content_description),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Barre de progression et temps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                        // Barre de buffer (arrière-plan)
                        LinearProgressIndicator(
                            progress = { if (duration > 0) bufferedPosition.toFloat() / duration.toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color.White.copy(alpha = 0.5f),
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )

                        // Slider de lecture (avant-plan)
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { onSeek(it.toLong()) },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Red,
                                activeTrackColor = Color.Red,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = formatDuration(duration),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Formate une durée en millisecondes en chaîne HH:MM:SS ou MM:SS.
 */
fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

