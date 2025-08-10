package com.example.wedsnap20.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeepLinkViewModel : ViewModel() {
    private val _pendingEventId = MutableStateFlow<String?>(null)
    val pendingEventId = _pendingEventId.asStateFlow()

    fun setPending(eventId: String?) { _pendingEventId.value = eventId }
    fun consumePending(): String? = _pendingEventId.value.also { _pendingEventId.value = null }
}
