package com.orion.toolbox.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orion.toolbox.logic.AppSettings
import com.orion.toolbox.logic.MailMode
import com.orion.toolbox.logic.Shift
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "toolbox_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val toolBoxNo = stringPreferencesKey("tool_box_no")
        val holdCount = stringPreferencesKey("hold_count")
        val shift = stringPreferencesKey("shift")
        val userName = stringPreferencesKey("user_name")
        val recordEmail = booleanPreferencesKey("record_email")
        val mailMode = stringPreferencesKey("mail_mode")
        val formUrl = stringPreferencesKey("form_url")
        val logSheetUrl = stringPreferencesKey("log_sheet_url")
        val manualSheetUrl = stringPreferencesKey("manual_sheet_url")
        val history = stringSetPreferencesKey("history")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p -> p.toSettings() }

    /** 제출 완료로 기록한 항목 키 (날짜|Shift|Phase) */
    val history: Flow<Set<String>> = context.dataStore.data.map { it[Keys.history] ?: emptySet() }

    suspend fun save(s: AppSettings) {
        context.dataStore.edit { p ->
            p[Keys.toolBoxNo] = s.toolBoxNo.trim()
            p[Keys.holdCount] = s.holdCount.trim()
            p[Keys.shift] = s.shift.name
            p[Keys.userName] = s.userName.trim()
            p[Keys.recordEmail] = s.recordEmail
            p[Keys.mailMode] = s.mailMode.name
            p[Keys.formUrl] = s.formUrl.trim()
            p[Keys.logSheetUrl] = s.logSheetUrl.trim()
            p[Keys.manualSheetUrl] = s.manualSheetUrl.trim()
        }
    }

    suspend fun addHistory(key: String) {
        context.dataStore.edit { p ->
            val cutoff = LocalDate.now().minusDays(90).toString()
            // 키가 날짜(ISO)로 시작하므로 문자열 비교로 오래된 기록을 정리한다
            val kept = (p[Keys.history] ?: emptySet()).filter { it.substringBefore('|') >= cutoff }
            p[Keys.history] = (kept + key).toSet()
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        toolBoxNo = this[Keys.toolBoxNo].orEmpty(),
        holdCount = this[Keys.holdCount].orEmpty(),
        shift = runCatching { Shift.valueOf(this[Keys.shift].orEmpty()) }.getOrDefault(Shift.DAY),
        userName = this[Keys.userName].orEmpty(),
        recordEmail = this[Keys.recordEmail] ?: true,
        mailMode = runCatching { MailMode.valueOf(this[Keys.mailMode].orEmpty()) }
            .getOrDefault(MailMode.ON_ABNORMAL),
        formUrl = this[Keys.formUrl].orEmpty(),
        logSheetUrl = this[Keys.logSheetUrl].orEmpty(),
        manualSheetUrl = this[Keys.manualSheetUrl].orEmpty(),
    )
}
