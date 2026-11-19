package com.soufcom.soukitv.features.home.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soufcom.soukitv.R
import com.soufcom.soukitv.features.home.domain.model.Category
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val CategoryFallbackColor = Color(0xFF1E1E1E)
private val CategoryBorderColor = Color(0xFF333333)
private val CategorySubtitleColor = Color(0xFFA0A0A0)
private val CategoryFooterColor = Color(0xFF181818)
private val CategoryShape = RoundedCornerShape(10.dp)

@Composable
fun CategoryCard(
    category: Category,
    onCategoryClick: (Category) -> Unit
) {
    val context = LocalContext.current
    var imageLoadFailed by remember(category.imageUrl) {
        mutableStateOf(category.imageUrl == null)
    }
    val imageRequest = remember(category.imageUrl) {
        ImageRequest.Builder(context)
            .data(category.imageUrl)
            .crossfade(true)
            .build()
    }
    Card(
        onClick = { onCategoryClick(category) },
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = CategoryShape,
        colors = CardDefaults.cardColors(containerColor = CategoryFallbackColor),
        border = BorderStroke(1.dp, CategoryBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!imageLoadFailed && category.imageUrl != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { imageLoadFailed = true }
                    )
                }
                if (imageLoadFailed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF242424), Color(0xFF141414))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Color(0xFFC41E3A).copy(alpha = 0.18f),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                tint = Color(0xFFC41E3A).copy(alpha = 0.72f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CategoryFooterColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.channel_count,
                        category.channelCount,
                        category.channelCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = CategorySubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

