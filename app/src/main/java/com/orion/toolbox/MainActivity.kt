package com.orion.toolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.orion.toolbox.ui.AppViewModel
import com.orion.toolbox.ui.CheckScreen
import com.orion.toolbox.ui.SettingsScreen
import com.orion.toolbox.ui.openUrl

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { AppRoot(viewModel) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppRoot(vm: AppViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val history by vm.history.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var leftApp by remember { mutableStateOf(false) }
    var askDone by remember { mutableStateOf(false) }

    // 폼(브라우저)을 연 뒤 앱으로 돌아오면 제출 완료 여부를 묻는다
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { leftApp = true }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (leftApp && vm.pending != null) askDone = true
        leftApp = false
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("점검") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("설정") },
                )
            }
        },
    ) { padding ->
        val s = settings
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tab == 0) {
            CheckScreen(
                settings = s,
                history = history,
                onSubmitted = { vm.pending = it },
                onGoSettings = { tab = 1 },
                modifier = Modifier.padding(padding),
            )
        } else {
            SettingsScreen(saved = s, onSave = vm::saveSettings, modifier = Modifier.padding(padding))
        }
    }

    if (askDone && vm.pending != null) {
        AlertDialog(
            onDismissRequest = { askDone = false },
            title = { Text("제출을 완료하셨나요?") },
            text = { Text("완료하면 기록에 저장하고 Tool Box 로그 시트를 보여드립니다.") },
            confirmButton = {
                TextButton(onClick = {
                    askDone = false
                    vm.confirmSubmitted()
                    settings?.logSheetUrl?.takeIf { it.isNotBlank() }?.let { openUrl(context, it) }
                }) { Text("제출 완료") }
            },
            dismissButton = {
                TextButton(onClick = { askDone = false; vm.pending = null }) { Text("아직 안 함") }
            },
        )
    }
}
