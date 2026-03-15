package com.synex.mirarecorder.ui.recordings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synex.mirarecorder.data.model.Recording
import com.synex.mirarecorder.data.model.Target
import com.synex.mirarecorder.data.repository.RecorderRepository
import com.synex.mirarecorder.di.AppModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordingsUiState(
    val recordings: List<Recording> = emptyList(),
    val targets: List<Target> = emptyList(),
    val searchText: String = "",
    val selectedStreamer: String? = null,
    val isLoading: Boolean = false,
) {
    val liveUserIds: Set<String>
        get() = targets.filter { it.isLiveRecording }.map { it.userId }.toSet()

    val avatarsByName: Map<String, String?>
        get() = targets.associate { it.name to it.avatar }

    val uniqueStreamers: List<String>
        get() = recordings.map { it.displayName }.distinct().sorted()

    val filteredRecordings: List<Recording>
        get() {
            var result = recordings
            if (!selectedStreamer.isNullOrBlank()) {
                result = result.filter { it.displayName == selectedStreamer }
            }
            if (searchText.isNotBlank()) {
                val query = searchText.lowercase()
                result = result.filter {
                    it.displayName.lowercase().contains(query) ||
                        it.filename.lowercase().contains(query) ||
                        (it.title?.lowercase()?.contains(query) == true)
                }
            }
            // Sort: live recordings first, then by date descending
            return result.sortedWith(
                compareByDescending<Recording> { isLive(it) }
                    .thenByDescending { it.startTime }
            )
        }

    val totalVideos: Int get() = recordings.size

    val totalHours: Double
        get() = recordings.sumOf { it.duration } / 3600.0

    fun isLive(recording: Recording): Boolean {
        return liveUserIds.any { userId ->
            targets.any { it.userId == userId && it.name == recording.displayName }
        }
    }
}

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val repository: RecorderRepository,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingsUiState())
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    init {
        loadRecordings()
        startPolling()
    }

    private fun isUnlocked(): Boolean =
        prefs.getBoolean(AppModule.KEY_UNLOCKED, false)

    private fun filterRecordings(
        recordings: List<Recording>,
        targets: List<Target>,
    ): List<Recording> {
        if (isUnlocked()) return recordings
        val filterTarget = targets.find { it.userId == FILTER_USER_ID } ?: return recordings
        return recordings.filter { it.displayName == filterTarget.name }
    }

    fun loadRecordings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val targets = repository.getTargets()
                val recordings = filterRecordings(repository.getRecordings(), targets)
                _uiState.update {
                    it.copy(recordings = recordings, targets = targets, isLoading = false)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun setSelectedStreamer(streamer: String?) {
        _uiState.update { it.copy(selectedStreamer = streamer) }
    }

    fun videoUrl(filename: String): String {
        val baseUrl = prefs.getString(AppModule.KEY_SERVER_URL, AppModule.DEFAULT_SERVER_URL)
            ?: AppModule.DEFAULT_SERVER_URL
        val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$base/video/$filename"
    }

    fun avatarUrl(name: String): String? {
        val avatar = _uiState.value.avatarsByName[name] ?: return null
        if (avatar.startsWith("http")) return avatar
        val baseUrl = prefs.getString(AppModule.KEY_SERVER_URL, AppModule.DEFAULT_SERVER_URL)
            ?: AppModule.DEFAULT_SERVER_URL
        val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$base$avatar"
    }

    fun thumbnailUrl(filename: String): String {
        val baseUrl = prefs.getString(AppModule.KEY_SERVER_URL, AppModule.DEFAULT_SERVER_URL)
            ?: AppModule.DEFAULT_SERVER_URL
        val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$base/api/thumbnails/$filename"
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                try {
                    val targets = repository.getTargets()
                    val recordings = filterRecordings(repository.getRecordings(), targets)
                    _uiState.update { it.copy(recordings = recordings, targets = targets) }
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        private const val FILTER_USER_ID = "126246308"
    }
}
