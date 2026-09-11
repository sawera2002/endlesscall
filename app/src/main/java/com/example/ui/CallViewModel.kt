package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CallRepository
import com.example.data.CallSessionLog
import com.example.data.QuickPreset
import com.example.service.CallManager
import com.example.service.EndlessCallSessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DialerUiState(
    val phoneNumber: String = "",
    val contactName: String = "",
    val isEndless: Boolean = true,
    val maxAttempts: Int = 20,
    val intervalSeconds: Int = 3,
    val callDurationLimit: Int = 0, // 0 = unlimited
    val autoSpeaker: Boolean = true,
    val vibrateOnDial: Boolean = true,
    val simSlot: Int = 0, // 0 = Default, 1 = SIM 1, 2 = SIM 2
    val isScheduled: Boolean = false,
    val scheduledMinutes: Int = 0,
    val scheduledSecondsRemaining: Int = 0
)

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = CallRepository(database.callDao(), database.presetDao())
    val callManager = CallManager(application, repository, viewModelScope)

    val activeSession: StateFlow<EndlessCallSessionState> = callManager.sessionState

    val callLogs: StateFlow<List<CallSessionLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presets: StateFlow<List<QuickPreset>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dialerState = MutableStateFlow(DialerUiState())
    val dialerState: StateFlow<DialerUiState> = _dialerState.asStateFlow()

    private var scheduleJob: Job? = null

    fun updatePhoneNumber(number: String) {
        _dialerState.update { it.copy(phoneNumber = number) }
    }

    fun setContact(name: String, number: String) {
        _dialerState.update {
            it.copy(
                contactName = name,
                phoneNumber = number
            )
        }
    }

    fun setEndlessMode(endless: Boolean) {
        _dialerState.update { it.copy(isEndless = endless) }
    }

    fun setMaxAttempts(attempts: Int) {
        _dialerState.update { it.copy(maxAttempts = attempts.coerceIn(1, 999)) }
    }

    fun setIntervalSeconds(seconds: Int) {
        _dialerState.update { it.copy(intervalSeconds = seconds.coerceIn(1, 60)) }
    }

    fun setCallDurationLimit(seconds: Int) {
        _dialerState.update { it.copy(callDurationLimit = seconds.coerceIn(0, 300)) }
    }

    fun toggleAutoSpeaker(enabled: Boolean) {
        _dialerState.update { it.copy(autoSpeaker = enabled) }
    }

    fun toggleVibrate(enabled: Boolean) {
        _dialerState.update { it.copy(vibrateOnDial = enabled) }
    }

    fun setSimSlot(slot: Int) {
        _dialerState.update { it.copy(simSlot = slot) }
    }

    fun startEndlessCall() {
        val state = _dialerState.value
        if (state.phoneNumber.isBlank()) return

        if (state.isScheduled && state.scheduledMinutes > 0) {
            startScheduledCountdown()
            return
        }

        callManager.startEndlessCall(
            phoneNumber = state.phoneNumber,
            contactName = state.contactName,
            maxAttempts = if (state.isEndless) -1 else state.maxAttempts,
            intervalSeconds = state.intervalSeconds,
            callDurationLimit = state.callDurationLimit,
            autoSpeaker = state.autoSpeaker,
            vibrateOnDial = state.vibrateOnDial,
            simSlot = state.simSlot
        )
    }

    private fun startScheduledCountdown() {
        scheduleJob?.cancel()
        val totalSeconds = _dialerState.value.scheduledMinutes * 60
        _dialerState.update {
            it.copy(
                isScheduled = true,
                scheduledSecondsRemaining = totalSeconds
            )
        }

        scheduleJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _dialerState.update { it.copy(scheduledSecondsRemaining = remaining) }
            }
            _dialerState.update { it.copy(isScheduled = false, scheduledSecondsRemaining = 0) }

            // Trigger call now
            val current = _dialerState.value
            callManager.startEndlessCall(
                phoneNumber = current.phoneNumber,
                contactName = current.contactName,
                maxAttempts = if (current.isEndless) -1 else current.maxAttempts,
                intervalSeconds = current.intervalSeconds,
                callDurationLimit = current.callDurationLimit,
                autoSpeaker = current.autoSpeaker,
                vibrateOnDial = current.vibrateOnDial,
                simSlot = current.simSlot
            )
        }
    }

    fun cancelSchedule() {
        scheduleJob?.cancel()
        _dialerState.update {
            it.copy(
                isScheduled = false,
                scheduledMinutes = 0,
                scheduledSecondsRemaining = 0
            )
        }
    }

    fun setScheduleMinutes(minutes: Int) {
        _dialerState.update {
            it.copy(
                scheduledMinutes = minutes,
                isScheduled = minutes > 0
            )
        }
    }

    fun stopEndlessCall(connected: Boolean = false) {
        callManager.stopEndlessCall(userCancelled = !connected, connected = connected)
    }

    fun pauseSession() = callManager.pauseSession()
    fun resumeSession() = callManager.resumeSession()
    fun redialImmediately() = callManager.redialImmediately()
    fun toggleSpeakerphone() = callManager.toggleSpeakerphone()
    fun simulateCallEnd() = callManager.simulateCallEndForTesting()

    fun applyPreset(preset: QuickPreset) {
        _dialerState.update {
            it.copy(
                contactName = preset.title,
                phoneNumber = preset.phoneNumber,
                isEndless = preset.isEndless,
                maxAttempts = if (preset.maxAttempts > 0) preset.maxAttempts else 20,
                intervalSeconds = preset.intervalSeconds,
                callDurationLimit = preset.callDurationLimitSeconds,
                autoSpeaker = preset.autoSpeaker
            )
        }
    }

    fun saveCurrentAsPreset(title: String, category: String) {
        val current = _dialerState.value
        if (current.phoneNumber.isBlank() || title.isBlank()) return

        viewModelScope.launch {
            repository.insertPreset(
                QuickPreset(
                    title = title,
                    phoneNumber = current.phoneNumber,
                    isEndless = current.isEndless,
                    maxAttempts = if (current.isEndless) -1 else current.maxAttempts,
                    intervalSeconds = current.intervalSeconds,
                    callDurationLimitSeconds = current.callDurationLimit,
                    autoSpeaker = current.autoSpeaker,
                    category = category
                )
            )
        }
    }

    fun deletePreset(preset: QuickPreset) {
        viewModelScope.launch { repository.deletePreset(preset) }
    }

    fun deleteLog(log: CallSessionLog) {
        viewModelScope.launch { repository.deleteLog(log) }
    }

    fun clearAllLogs() {
        viewModelScope.launch { repository.clearAllLogs() }
    }
}
