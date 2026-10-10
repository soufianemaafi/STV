package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.soukitv.R
import com.example.soukitv.features.home.domain.model.MatchItem

@Composable
fun MatchesTab(
    matches: List<MatchItem>,
    onMatchClick: (MatchItem) -> Unit
) {
    if (matches.isEmpty()) {
        Text(
            text = stringResource(R.string.empty_matches_today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .wrapContentSize(Alignment.Center)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(matches, key = { it.id }) { match ->
            MatchCard(match = match, onMatchClick = onMatchClick)
        }
    }
}

