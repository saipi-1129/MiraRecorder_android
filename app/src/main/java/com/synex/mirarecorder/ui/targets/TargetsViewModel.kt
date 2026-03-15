package com.synex.mirarecorder.ui.targets

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class TargetsUiState(
    val targets: List<Target> = emptyList(),
    val searchText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val isUnlocked: Boolean = false,
) {
    val filteredTargets: List<Target>
        get() {
            if (searchText.isBlank()) return targets
            val query = searchText.lowercase()
            return targets.filter {
                it.name.lowercase().contains(query) || it.userId.lowercase().contains(query)
            }
        }
}

@HiltViewModel
class TargetsViewModel @Inject constructor(
    private val repository: RecorderRepository,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TargetsUiState())
    val uiState: StateFlow<TargetsUiState> = _uiState.asStateFlow()

    init {
        loadTargets()
        startPolling()
    }

    fun isUnlocked(): Boolean =
        prefs.getBoolean(AppModule.KEY_UNLOCKED, false)

    private fun filterTargets(targets: List<Target>): List<Target> {
        if (isUnlocked()) return targets
        return targets.filter { it.userId == FILTER_USER_ID }
    }

    fun loadTargets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val targets = filterTargets(repository.getTargets())
                _uiState.update { it.copy(targets = targets, isLoading = false, isUnlocked = isUnlocked()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun addTarget(userId: String) {
        viewModelScope.launch {
            try {
                repository.addTarget(userId)
                _uiState.update { it.copy(showAddDialog = false) }
                loadTargets()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleTarget(target: Target, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleTarget(target.userId, enabled)
                _uiState.update { state ->
                    state.copy(
                        targets = state.targets.map {
                            if (it.userId == target.userId) it.copy(enabled = enabled) else it
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteTarget(target: Target) {
        viewModelScope.launch {
            try {
                repository.deleteTarget(target.userId)
                _uiState.update { state ->
                    state.copy(targets = state.targets.filter { it.userId != target.userId })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun setShowAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun avatarUrl(target: Target): String? {
        val avatar = target.avatar ?: return null
        if (avatar.startsWith("http")) return avatar
        val baseUrl = prefs.getString(AppModule.KEY_SERVER_URL, AppModule.DEFAULT_SERVER_URL)
            ?: AppModule.DEFAULT_SERVER_URL
        val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$base$avatar"
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                try {
                    val targets = filterTargets(repository.getTargets())
                    _uiState.update { it.copy(targets = targets, isUnlocked = isUnlocked()) }
                } catch (_: Exception) {
                    // Silently ignore polling errors
                }
            }
        }
    }

    companion object {
        private const val FILTER_USER_ID = "126246308"
    }
}
