package com.soufcom.soukitv.features.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import coil.request.ImageRequest

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

@Composable
fun SoukiLogoImage(
    url: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val transformation = remember { AutoCropTransparentTransformation() }
    val imageRequest = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .addHeader("User-Agent", BROWSER_USER_AGENT)
            .transformations(transformation)
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

