package com.example.stv

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _streamUrl = MutableStateFlow("https://stream.skynewsarabia.com/hls/sna_720.m3u8")
    val streamUrl: StateFlow<String> = _streamUrl.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    fun updateUrl(url: String) {
        _streamUrl.value = url
        if (url.isNotBlank()) {
            _isError.value = false
        }
    }

    fun validateUrl(): Boolean {
        return if (_streamUrl.value.isBlank()) {
            _isError.value = true
            false
        } else {
            _isError.value = false
            true
        }
    }
}

