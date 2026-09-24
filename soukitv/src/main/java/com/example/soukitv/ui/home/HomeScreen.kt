package com.example.soukitv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val uiState by viewModel.uiState.collectAsState()
    val currentState = uiState
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var lastCategories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(currentState) {
        if (currentState is HomeUiState.Success) {
            lastCategories = currentState.categories
        }
    }

    val categories = when (val state = currentState) {
        HomeUiState.Loading -> lastCategories
        is HomeUiState.Success -> state.categories
        is HomeUiState.Error -> lastCategories
    }

    val isLoading = currentState is HomeUiState.Loading && lastCategories.isEmpty()
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
                        IconButton(onClick = { /* TODO: Implement Search */ }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground
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
            when {
                isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            FeaturedSection()
                        }

                        items(categories) { category ->
                            CategorySection(category = category, onChannelClick = { channel ->
                                viewModel.onAction(HomeUiAction.SelectChannel(channel))
                            })
                        }
                    }
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
fun CategorySection(category: Category, onChannelClick: (Channel) -> Unit) {
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
                modifier = Modifier.clickable { /* TODO */ }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.channels) { channel ->
                ChannelItem(channel = channel, onClick = { onChannelClick(channel) })
            }
        }
    }
}

@Composable
fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
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
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}