package com.orion.toolbox.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orion.toolbox.data.SettingsRepository
import com.orion.toolbox.logic.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 폼을 연 뒤 아직 "제출 완료"를 확인하지 않은 상태 */
data class PendingSubmission(val historyKey: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    /** null 이면 아직 저장소에서 읽는 중 */
    val settings: StateFlow<AppSettings?> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<Set<String>> =
        repo.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    var pending by mutableStateOf<PendingSubmission?>(null)

    fun saveSettings(s: AppSettings) {
        viewModelScope.launch { repo.save(s) }
    }

    fun confirmSubmitted() {
        val key = pending?.historyKey ?: return
        pending = null
        viewModelScope.launch { repo.addHistory(key) }
    }
}
