package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.soukitv.R
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel

@Composable
fun ChannelsTab(
    categories: List<Category>,
    activeCategory: Category?,
    activeCountry: String?,
    isFlatMode: Boolean,
    flatChannels: List<Channel>,
    favorites: Set<String>,
    onCategoryClick: (Category) -> Unit,
    onCountryClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit
) {
    if (isFlatMode) {
        ChannelsGridContent(
            channels = flatChannels,
            favorites = favorites,
            onChannelClick = onChannelClick,
            onToggleFavorite = onToggleFavorite
        )
        return
    }

    if (activeCategory == null) {
        CategoriesGrid(categories = categories, onCategoryClick = onCategoryClick)
    } else {
        val countries = activeCategory.channels.mapNotNull { it.country }.distinct()
        if (countries.isNotEmpty() && activeCountry == null) {
            CountriesGrid(
                category = activeCategory,
                countries = countries,
                onBackClick = onBackClick,
                onCountryClick = onCountryClick
            )
        } else {
            val channels = if (activeCountry != null) {
                activeCategory.channels.filter { it.country == activeCountry }
            } else {
                activeCategory.channels
            }
            Column(modifier = Modifier.fillMaxSize()) {
                BackHeader(title = activeCountry ?: activeCategory.name, onBackClick = onBackClick)
                ChannelsGridContent(
                    channels = channels.sortedByDescending { it.id in favorites },
                    favorites = favorites,
                    onChannelClick = onChannelClick,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
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
private fun CountriesGrid(
    category: Category,
    countries: List<String>,
    onBackClick: () -> Unit,
    onCountryClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(title = category.name, onBackClick = onBackClick)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(countries, key = { it }) { country ->
                CountryCard(
                    country = country,
                    channelCount = category.channels.count { it.country == country },
                    onClick = { onCountryClick(country) }
                )
            }
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
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun BackHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

