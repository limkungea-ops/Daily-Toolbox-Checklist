package com.orion.toolbox.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.orion.toolbox.logic.AppSettings
import com.orion.toolbox.logic.FormLogic
import com.orion.toolbox.logic.MailMode
import com.orion.toolbox.logic.SettingsField
import com.orion.toolbox.logic.Shift

@Composable
fun SettingsScreen(
    saved: AppSettings,
    onSave: (AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var draft by remember(saved) { mutableStateOf(saved) }
    var errors by remember { mutableStateOf<Map<SettingsField, String>>(emptyMap()) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineSmall)
        Text("한 번 저장하면 점검 화면에 계속 자동 입력됩니다.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = draft.toolBoxNo,
            onValueChange = { draft = draft.copy(toolBoxNo = it) },
            label = { Text("Tool Box No (폼 목록과 동일하게, 예: GMP617)") },
            isError = SettingsField.TOOLBOX_NO in errors,
            supportingText = { errors[SettingsField.TOOLBOX_NO]?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.holdCount,
            onValueChange = { v -> draft = draft.copy(holdCount = v.filter(Char::isDigit).take(4)) },
            label = { Text("Hold Count (등록 수량)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = SettingsField.HOLD_COUNT in errors,
            supportingText = { errors[SettingsField.HOLD_COUNT]?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Shift 기본값", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shift.entries.forEach { s ->
                FilterChip(
                    selected = draft.shift == s,
                    onClick = { draft = draft.copy(shift = s) },
                    label = { Text(s.label) },
                )
            }
        }

        OutlinedTextField(
            value = draft.userName,
            onValueChange = { draft = draft.copy(userName = it) },
            label = { Text("사용자 (한글 4자 이내, 비우면 이메일 이름)") },
            isError = SettingsField.USER_NAME in errors,
            supportingText = { errors[SettingsField.USER_NAME]?.let { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = draft.recordEmail,
                onCheckedChange = { draft = draft.copy(recordEmail = it) },
            )
            Text("  이메일을 응답에 포함", modifier = Modifier.padding(start = 8.dp))
        }

        Text("Tool 담당자 메일 전송 체크", style = MaterialTheme.typography.titleMedium)
        MailMode.entries.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = draft.mailMode == m,
                    onClick = { draft = draft.copy(mailMode = m) },
                )
                Text(m.label)
            }
        }

        OutlinedTextField(
            value = draft.formUrl,
            onValueChange = { draft = draft.copy(formUrl = it) },
            label = { Text("점검 폼 주소 (Google Form)") },
            isError = SettingsField.FORM_URL in errors,
            supportingText = { errors[SettingsField.FORM_URL]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.logSheetUrl,
            onValueChange = { draft = draft.copy(logSheetUrl = it) },
            label = { Text("Tool Box 로그 시트 주소") },
            isError = SettingsField.LOG_SHEET_URL in errors,
            supportingText = { errors[SettingsField.LOG_SHEET_URL]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.manualSheetUrl,
            onValueChange = { draft = draft.copy(manualSheetUrl = it) },
            label = { Text("매뉴얼 입력 시트 주소 (누락일 보충)") },
            isError = SettingsField.MANUAL_SHEET_URL in errors,
            supportingText = { errors[SettingsField.MANUAL_SHEET_URL]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                errors = FormLogic.validateSettings(draft)
                if (errors.isEmpty()) {
                    onSave(draft.copy(formUrl = FormLogic.normalizeFormUrl(draft.formUrl) ?: draft.formUrl))
                    Toast.makeText(context, "저장했습니다", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("저장") }
    }
}
