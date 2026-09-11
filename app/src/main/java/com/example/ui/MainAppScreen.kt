package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.ActiveCallDialog
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DeepPurpleBorder
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleCardElevated
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.PurpleButtonGradient
import com.example.ui.theme.PurpleHeroGradient
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

enum class AppTab(val title: String) {
    DIALER("Dialer"),
    PRESETS("Presets"),
    HISTORY("History"),
    SETTINGS("Settings")
}

@Composable
fun MainAppScreen(
    viewModel: CallViewModel = viewModel()
) {
    val dialerState by viewModel.dialerState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val presets by viewModel.presets.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.DIALER) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetTitleInput by remember { mutableStateOf("") }
    var presetCategoryInput by remember { mutableStateOf("Hotline") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepPurpleDark,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF281148), DeepPurpleDark)
                        )
                    )
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PurpleHeroGradient)
                                .border(1.5.dp, MagentaNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "Smart Auto Redial",
                                color = MagentaNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Active badge if dialing
                    if (activeSession.isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MagentaNeon.copy(alpha = 0.2f))
                                .border(1.dp, MagentaNeon, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "REDIAL ACTIVE",
                                color = MagentaNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepPurpleCard,
                contentColor = TextPrimary,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.DIALER,
                    onClick = { currentTab = AppTab.DIALER },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Dialer"
                        )
                    },
                    label = { Text("Dialer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MagentaNeon,
                        indicatorColor = VioletElectric,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_dialer")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.PRESETS,
                    onClick = { currentTab = AppTab.PRESETS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Presets"
                        )
                    },
                    label = { Text("Presets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MagentaNeon,
                        indicatorColor = VioletElectric,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_presets")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { currentTab = AppTab.HISTORY },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MagentaNeon,
                        indicatorColor = VioletElectric,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_history")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { currentTab = AppTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MagentaNeon,
                        indicatorColor = VioletElectric,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    AppTab.DIALER -> {
                        DialerScreen(
                            state = dialerState,
                            onPhoneNumberChange = viewModel::updatePhoneNumber,
                            onContactSelected = viewModel::setContact,
                            onToggleEndless = viewModel::setEndlessMode,
                            onMaxAttemptsChange = viewModel::setMaxAttempts,
                            onIntervalChange = viewModel::setIntervalSeconds,
                            onCallDurationLimitChange = viewModel::setCallDurationLimit,
                            onToggleSpeaker = viewModel::toggleAutoSpeaker,
                            onToggleVibrate = viewModel::toggleVibrate,
                            onSimSlotChange = viewModel::setSimSlot,
                            onScheduleChange = viewModel::setScheduleMinutes,
                            onCancelSchedule = viewModel::cancelSchedule,
                            onStartCall = viewModel::startEndlessCall,
                            onSaveAsPresetClick = {
                                presetTitleInput = dialerState.contactName.ifBlank { "Preset for ${dialerState.phoneNumber}" }
                                showSavePresetDialog = true
                            }
                        )
                    }

                    AppTab.PRESETS -> {
                        PresetsScreen(
                            presets = presets,
                            onSelectPreset = { preset ->
                                viewModel.applyPreset(preset)
                                currentTab = AppTab.DIALER
                            },
                            onDirectDialPreset = { preset ->
                                viewModel.applyPreset(preset)
                                viewModel.startEndlessCall()
                            },
                            onDeletePreset = viewModel::deletePreset,
                            onAddPreset = { title, number, cat ->
                                viewModel.saveCurrentAsPreset(title, cat)
                            }
                        )
                    }

                    AppTab.HISTORY -> {
                        HistoryScreen(
                            logs = callLogs,
                            onRedialLog = { log ->
                                viewModel.updatePhoneNumber(log.phoneNumber)
                                viewModel.setContact(log.contactName, log.phoneNumber)
                                viewModel.setIntervalSeconds(log.intervalSeconds)
                                viewModel.startEndlessCall()
                            },
                            onDeleteLog = viewModel::deleteLog,
                            onClearAllLogs = viewModel::clearAllLogs
                        )
                    }

                    AppTab.SETTINGS -> {
                        SettingsScreen()
                    }
                }
            }

            // Save Preset Dialog
            if (showSavePresetDialog) {
                AlertDialog(
                    onDismissRequest = { showSavePresetDialog = false },
                    containerColor = DeepPurpleCardElevated,
                    title = { Text("Save as Quick Preset", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = presetTitleInput,
                                onValueChange = { presetTitleInput = it },
                                label = { Text("Preset Title", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VioletElectric,
                                    unfocusedBorderColor = DeepPurpleBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = presetCategoryInput,
                                onValueChange = { presetCategoryInput = it },
                                label = { Text("Category", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VioletElectric,
                                    unfocusedBorderColor = DeepPurpleBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (presetTitleInput.isNotBlank()) {
                                    viewModel.saveCurrentAsPreset(presetTitleInput.trim(), presetCategoryInput.trim())
                                    showSavePresetDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletElectric)
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSavePresetDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }

            // Active Calling HUD Modal Dialog
            ActiveCallDialog(
                session = activeSession,
                onPause = viewModel::pauseSession,
                onResume = viewModel::resumeSession,
                onRedialNow = viewModel::redialImmediately,
                onToggleSpeaker = { viewModel.toggleSpeakerphone() },
                onSimulateCallEnd = viewModel::simulateCallEnd,
                onConnected = { viewModel.stopEndlessCall(connected = true) },
                onStop = { viewModel.stopEndlessCall(connected = false) }
            )
        }
    }
}
