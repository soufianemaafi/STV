package com.example.soukitv.features.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.core.player.StvPlayerLauncher.LaunchResult
import com.example.soukitv.features.home.data.ChannelRepositoryImpl
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ChannelRepository,
    private val stvPlayerLauncher: StvPlayerLauncher
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var lastSuccessCategories: List<Category> = emptyList()

    init {
        loadChannels()
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.SelectChannel -> handleSelectChannel(action.channel)
            HomeUiAction.DismissInstallDialog -> restoreSuccessState()
            HomeUiAction.InstallStv -> {
                stvPlayerLauncher.openPlayStore()
                restoreSuccessState()
            }
            HomeUiAction.Retry -> loadChannels()
        }
    }

    private fun loadChannels() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            runCatching {
                repository.getCategories().collect { categories ->
                    lastSuccessCategories = categories
                    _uiState.value = HomeUiState.Success(categories)
                }
            }.onFailure { throwable ->
                _uiState.value = HomeUiState.Error(
                    throwable.message ?: "Unable to load channels"
                )
            }
        }
    }

    private fun handleSelectChannel(channel: Channel) {
        when (val result = stvPlayerLauncher.launchChannel(channel)) {
            LaunchResult.Launched -> Unit
            LaunchResult.NotInstalled -> {
                _uiState.value = HomeUiState.Error(StvPlayerLauncher.NOT_INSTALLED_MESSAGE)
            }
            is LaunchResult.Failed -> {
                _uiState.value = HomeUiState.Error(
                    result.message
                )
            }
        }
    }

    private fun restoreSuccessState() {
        _uiState.value = if (lastSuccessCategories.isNotEmpty()) {
            HomeUiState.Success(lastSuccessCategories)
        } else {
            HomeUiState.Loading
        }
    }
}

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                repository = ChannelRepositoryImpl(),
                stvPlayerLauncher = StvPlayerLauncher(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

