package com.example.soukitv.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.presentation.HomeUiAction
import com.example.soukitv.features.home.presentation.HomeUiState
import com.example.soukitv.features.home.presentation.HomeViewModel
import com.example.soukitv.features.home.presentation.HomeViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val currentState = viewModel.uiState.collectAsState().value
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var lastSuccess by remember { mutableStateOf<HomeUiState.Success?>(null) }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentState) {
        if (currentState is HomeUiState.Success) {
            lastSuccess = currentState
        }
    }

    // Le dernier catalogue connu reste affiché pendant Loading / Error.
    val success = (currentState as? HomeUiState.Success) ?: lastSuccess
    val errorState = currentState as? HomeUiState.Error

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Movies") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("TV Shows") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "SoukiTV",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchVisible = !isSearchVisible
                            // Fermer la recherche réinitialise la requête.
                            if (!isSearchVisible) viewModel.onAction(HomeUiAction.UpdateSearchQuery(""))
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = if (isSearchVisible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedVisibility(visible = isSearchVisible) {
                    SearchField(
                        query = success?.searchQuery.orEmpty(),
                        onQueryChange = { viewModel.onAction(HomeUiAction.UpdateSearchQuery(it)) },
                        onClear = { viewModel.onAction(HomeUiAction.UpdateSearchQuery("")) }
                    )
                }

                if (success != null) {
                    CategoryFilterRow(
                        categories = success.allCategories,
                        selectedCategoryName = success.selectedCategoryName,
                        onSelect = { viewModel.onAction(HomeUiAction.SelectCategory(it)) }
                    )
                }

                when {
                    success == null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (errorState == null) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    success.selectedCategoryName != null || success.searchQuery.isNotBlank() -> ChannelGrid(
                        modifier = Modifier.weight(1f),
                        title = success.selectedCategoryName ?: "Résultats de recherche",
                        channels = success.displayedCategories.flatMap { it.channels },
                        onChannelClick = { viewModel.onAction(HomeUiAction.SelectChannel(it)) }
                    )

                    else -> CategoryOverview(
                        modifier = Modifier.weight(1f),
                        categories = success.displayedCategories,
                        onSeeAll = { category ->
                            viewModel.onAction(HomeUiAction.SelectCategory(category.name))
                        },
                        onChannelClick = { viewModel.onAction(HomeUiAction.SelectChannel(it)) }
                    )
                }
            }

            if (errorState != null) {
                val installRequired = errorState.message == StvPlayerLauncher.NOT_INSTALLED_MESSAGE
                AlertDialog(
                    onDismissRequest = { viewModel.onAction(HomeUiAction.DismissInstallDialog) },
                    title = {
                        Text(
                            text = if (installRequired) "STV Required" else "Error"
                        )
                    },
                    text = { Text(text = errorState.message) },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (installRequired) {
                                    viewModel.onAction(HomeUiAction.InstallStv)
                                } else {
                                    viewModel.onAction(HomeUiAction.Retry)
                                }
                            }
                        ) {
                            Text(text = if (installRequired) "Install STV" else "Retry")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { viewModel.onAction(HomeUiAction.DismissInstallDialog) }) {
                            Text(text = "Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Rechercher une chaîne") },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer")
                }
            }
        }
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryName: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryName == null,
                onClick = { onSelect(null) },
                label = { Text("Tous") }
            )
        }
        items(categories, key = { it.name }) { category ->
            FilterChip(
                selected = selectedCategoryName == category.name,
                onClick = { onSelect(category.name) },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
private fun CategoryOverview(
    modifier: Modifier,
    categories: List<Category>,
    onSeeAll: (Category) -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            FeaturedSection()
        }

        items(categories, key = { it.name }) { category ->
            CategorySection(
                category = category,
                onSeeAll = { onSeeAll(category) },
                onChannelClick = onChannelClick
            )
        }
    }
}

@Composable
private fun ChannelGrid(
    modifier: Modifier,
    title: String,
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (channels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Aucune chaîne trouvée",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(channels) { channel ->
                ChannelItem(
                    channel = channel,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
fun FeaturedSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Gray)
    ) {
        AsyncImage(
            model = "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg",
            contentDescription = "Featured",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        Text(
            text = "Featured Content",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

@Composable
fun CategorySection(
    category: Category,
    onSeeAll: () -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "See All",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onSeeAll)
                    .padding(4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.channels) { channel ->
                ChannelItem(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(160.dp)
) {
    Column(
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(1.dp)
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

