package com.orion.toolbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.orion.toolbox.logic.Checkin
import com.orion.toolbox.logic.CheckinErrors
import com.orion.toolbox.logic.FormLogic
import com.orion.toolbox.logic.FormSpec
import com.orion.toolbox.logic.Phase
import com.orion.toolbox.logic.SettingsField
import java.time.LocalDate

@Composable
fun CheckScreen(
    settings: AppSettings,
    history: Set<String>,
    onSubmitted: (PendingSubmission) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val needed = setOf(SettingsField.TOOLBOX_NO, SettingsField.HOLD_COUNT, SettingsField.FORM_URL)
    val ready = FormLogic.validateSettings(settings).keys.none { it in needed }

    // 오늘 Before 를 이미 제출했다면 기본 선택을 After 로
    val defaultPhase =
        if (FormLogic.historyKey(today, settings.shift, Phase.BEFORE) in history) Phase.AFTER else Phase.BEFORE
    var chosenPhase by remember { mutableStateOf<Phase?>(null) }
    val phase = chosenPhase ?: defaultPhase

    var abnormalText by remember { mutableStateOf("0") }
    var lossType by remember { mutableStateOf<String?>(null) }
    var remark by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(CheckinErrors()) }
    var confirmDuplicate by remember { mutableStateOf<Checkin?>(null) }

    val abnormal = abnormalText.toIntOrNull() ?: 0
    val checkin = Checkin(phase, abnormal, lossType, remark)

    fun launchForm(c: Checkin) {
        val url = FormLogic.buildPrefillUrl(settings, c) ?: return
        onSubmitted(PendingSubmission(FormLogic.historyKey(today, settings.shift, c.phase)))
        openUrl(context, url)
    }

    fun submit() {
        errors = FormLogic.validateCheckin(checkin)
        if (errors.hasError) return
        if (FormLogic.historyKey(today, settings.shift, phase) in history) {
            confirmDuplicate = checkin
        } else {
            launchForm(checkin)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tool Box 점검", style = MaterialTheme.typography.headlineSmall)

        if (!ready) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("먼저 설정이 필요합니다 (Tool Box No, Hold Count, 폼 주소).")
                    Button(onClick = onGoSettings) { Text("설정으로 이동") }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("자동 입력 값 (설정)", style = MaterialTheme.typography.labelLarge)
                    Text("Tool Box No: ${settings.toolBoxNo}")
                    Text("Hold Count: ${settings.holdCount}")
                    Text("Shift: ${settings.shift.label}")
                    if (settings.userName.isNotBlank()) Text("사용자: ${settings.userName}")
                    Text("날짜: $today")
                }
            }

            Text("Before / After", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Phase.entries.forEach { p ->
                    FilterChip(
                        selected = phase == p,
                        onClick = { chosenPhase = p },
                        label = { Text(p.label) },
                    )
                }
            }

            OutlinedTextField(
                value = abnormalText,
                onValueChange = { v -> abnormalText = v.filter(Char::isDigit).take(4) },
                label = { Text("Abnormal (분실/이상 수량, 없으면 0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (abnormal > 0) {
                Text("유형", style = MaterialTheme.typography.titleMedium)
                FormSpec.LOSS_TYPES.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = lossType == t, onClick = { lossType = t })
                        Text(t)
                    }
                }
                errors.lossType?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Remark (분실 내용 필수)") },
                    isError = errors.remark != null,
                    supportingText = { errors.remark?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (FormLogic.shouldSendMail(settings, checkin)) {
                Text("이 제출은 Tool 담당자에게 메일 전송이 체크됩니다.", style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = ::submit, modifier = Modifier.fillMaxWidth()) {
                Text("폼 열어서 제출하기")
            }
            Text(
                "폼이 열리면 값이 채워져 있습니다. 확인 후 '제출'만 누르세요.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { openUrl(context, settings.logSheetUrl) },
                enabled = settings.logSheetUrl.isNotBlank(),
            ) { Text("로그 시트") }
            OutlinedButton(
                onClick = { openUrl(context, settings.manualSheetUrl) },
                enabled = settings.manualSheetUrl.isNotBlank(),
            ) { Text("매뉴얼 입력 시트") }
        }
    }

    confirmDuplicate?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDuplicate = null },
            title = { Text("이미 제출한 기록이 있습니다") },
            text = { Text("오늘 ${settings.shift.label} ${c.phase.label} 점검은 이미 제출했습니다. 다시 제출할까요?") },
            confirmButton = {
                TextButton(onClick = { confirmDuplicate = null; launchForm(c) }) { Text("다시 제출") }
            },
            dismissButton = { TextButton(onClick = { confirmDuplicate = null }) { Text("취소") } },
        )
    }
}
