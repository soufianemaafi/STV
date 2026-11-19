package com.soufcom.soukitv.features.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soufcom.soukitv.R
import com.soufcom.soukitv.features.home.domain.model.Category
import com.soufcom.soukitv.features.home.domain.model.Channel

@Composable
fun ChannelsTab(
    visibleCategories: List<Category>,
    currentCategoryPath: String?,
    isFlatMode: Boolean,
    flatChannels: List<Channel>,
    favorites: Set<String>,
    onCategoryClick: (Category) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit
) {
    if (isFlatMode && currentCategoryPath == null) {
        ChannelsGridContent(
            channels = flatChannels,
            favorites = favorites,
            onChannelClick = onChannelClick,
            onToggleFavorite = onToggleFavorite
        )
        return
    }

    if (currentCategoryPath == null) {
        CategoriesGrid(categories = visibleCategories, onCategoryClick = onCategoryClick)
    } else if (isFlatMode) {
        ChannelsGridContent(
            channels = flatChannels,
            favorites = favorites,
            onChannelClick = onChannelClick,
            onToggleFavorite = onToggleFavorite
        )
    } else {
        CategoriesGrid(categories = visibleCategories, onCategoryClick = onCategoryClick)
    }
}

@Composable
private fun CategoriesGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit
) {
    if (categories.isEmpty()) {
        EmptyMessage(stringResource(R.string.empty_channels))
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.name }) { category ->
            CategoryCard(category = category, onCategoryClick = onCategoryClick)
        }
    }
}

@Composable
private fun ChannelsGridContent(
    channels: List<Channel>,
    favorites: Set<String>,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit
) {
    if (channels.isEmpty()) {
        EmptyMessage(stringResource(R.string.empty_search_results))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(channels, key = { it.id }) { channel ->
            ChannelTile(
                channel = channel,
                isFavorite = channel.id in favorites,
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}

