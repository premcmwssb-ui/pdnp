package com.pdnp.dailydigest

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdnp.dailydigest.ui.DigestTheme
import com.pdnp.dailydigest.ui.screens.DashboardScreen
import com.pdnp.dailydigest.ui.screens.MeetingsScreen
import com.pdnp.dailydigest.ui.screens.MessagesScreen
import com.pdnp.dailydigest.ui.screens.SettingsScreen
import com.pdnp.dailydigest.ui.screens.TasksScreen

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()

        setContent {
            DigestTheme {
                val vm: AppViewModel = viewModel()
                var tab by rememberSaveable { mutableIntStateOf(0) }

                val tabs = listOf(
                    "Digest" to Icons.Filled.Home,
                    "Messages" to Icons.Filled.Email,
                    "Tasks" to Icons.Filled.CheckCircle,
                    "Meetings" to Icons.Filled.Mic,
                    "Settings" to Icons.Filled.Settings
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { index, (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    val modifier = Modifier.padding(padding)
                    when (tab) {
                        0 -> DashboardScreen(vm, modifier)
                        1 -> MessagesScreen(vm, modifier)
                        2 -> TasksScreen(vm, modifier)
                        3 -> MeetingsScreen(vm, modifier)
                        else -> SettingsScreen(modifier)
                    }
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
