package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.CallRepository
import com.example.data.CallSessionLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CallStatus {
    IDLE,
    DIALING,
    IN_CALL,
    WAITING_NEXT,
    PAUSED,
    FINISHED
}

data class EndlessCallSessionState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val phoneNumber: String = "",
    val contactName: String = "",
    val currentAttempt: Int = 0,
    val maxAttempts: Int = -1, // -1 means infinite/endless
    val intervalSeconds: Int = 3,
    val remainingCountdown: Int = 0,
    val callDurationLimitSeconds: Int = 0,
    val currentCallElapsedSeconds: Int = 0,
    val totalSessionElapsedSeconds: Long = 0L,
    val status: CallStatus = CallStatus.IDLE,
    val autoSpeaker: Boolean = true,
    val vibrateOnDial: Boolean = true,
    val simSlot: Int = 0,
    val statusMessage: String = "Ready"
)

class CallManager(
    private val context: Context,
    private val repository: CallRepository,
    private val scope: CoroutineScope
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _sessionState = MutableStateFlow(EndlessCallSessionState())
    val sessionState: StateFlow<EndlessCallSessionState> = _sessionState.asStateFlow()

    private var countdownJob: Job? = null
    private var sessionTimerJob: Job? = null
    private var callDurationJob: Job? = null

    private var telephonyCallback: Any? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var lastObservedCallState: Int = TelephonyManager.CALL_STATE_IDLE
    private var hasObservedOffhookForCurrentAttempt: Boolean = false

    init {
        registerCallStateListener()
    }

    private fun registerCallStateListener() {
        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChange(state)
                    }
                }
                telephonyManager?.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallStateChange(state)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                phoneStateListener = listener
            }
        } catch (_: SecurityException) {
            // Permission not yet granted
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    private fun handleCallStateChange(newState: Int) {
        val prevState = lastObservedCallState
        lastObservedCallState = newState

        if (!_sessionState.value.isActive || _sessionState.value.isPaused) return

        when (newState) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                hasObservedOffhookForCurrentAttempt = true
                _sessionState.update { it.copy(status = CallStatus.IN_CALL, statusMessage = "Call in progress") }
                startCallDurationMonitor()
                if (_sessionState.value.autoSpeaker) {
                    enableSpeakerphone()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                callDurationJob?.cancel()
                if (hasObservedOffhookForCurrentAttempt || prevState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Call has ended
                    hasObservedOffhookForCurrentAttempt = false
                    onCallEnded()
                }
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                _sessionState.update { it.copy(statusMessage = "Ringing...") }
            }
        }
    }

    fun startEndlessCall(
        phoneNumber: String,
        contactName: String = "",
        maxAttempts: Int = -1,
        intervalSeconds: Int = 3,
        callDurationLimit: Int = 0,
        autoSpeaker: Boolean = true,
        vibrateOnDial: Boolean = true,
        simSlot: Int = 0
    ) {
        if (phoneNumber.isBlank()) return

        stopEndlessCall(userCancelled = false)

        _sessionState.update {
            EndlessCallSessionState(
                isActive = true,
                isPaused = false,
                phoneNumber = phoneNumber.trim(),
                contactName = contactName.trim(),
                currentAttempt = 0,
                maxAttempts = maxAttempts,
                intervalSeconds = intervalSeconds.coerceAtLeast(1),
                remainingCountdown = 0,
                callDurationLimitSeconds = callDurationLimit,
                totalSessionElapsedSeconds = 0L,
                status = CallStatus.DIALING,
                autoSpeaker = autoSpeaker,
                vibrateOnDial = vibrateOnDial,
                simSlot = simSlot,
                statusMessage = "Starting Endless Call..."
            )
        }

        // Start session elapsed time ticker
        startSessionTimer()

        // Execute first call
        triggerDialAttempt()
    }

    private fun triggerDialAttempt() {
        val current = _sessionState.value
        if (!current.isActive || current.isPaused) return

        val nextAttempt = current.currentAttempt + 1

        // Check if max attempts reached (when not endless)
        if (current.maxAttempts > 0 && nextAttempt > current.maxAttempts) {
            finishSession("Completed target of ${current.maxAttempts} attempts")
            return
        }

        hasObservedOffhookForCurrentAttempt = false
        _sessionState.update {
            it.copy(
                currentAttempt = nextAttempt,
                status = CallStatus.DIALING,
                remainingCountdown = 0,
                currentCallElapsedSeconds = 0,
                statusMessage = if (it.maxAttempts > 0) "Dialing attempt $nextAttempt of ${it.maxAttempts}" else "Dialing attempt $nextAttempt (∞ Endless)"
            )
        }

        if (current.vibrateOnDial) {
            triggerVibration(120L)
        }

        // Place the actual call
        executePhoneCall(current.phoneNumber, current.simSlot)

        // Safety fallback: if no telephony event arrives within 25 seconds or if auto duration is set
        startCallDurationMonitor()
    }

    private fun executePhoneCall(number: String, simSlot: Int) {
        val sanitized = number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (sanitized.isBlank()) return

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$sanitized"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized"))
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Dual SIM hint parameters commonly recognized by OEM dialers
        if (simSlot > 0) {
            intent.putExtra("com.android.phone.extra.slot", simSlot - 1)
            intent.putExtra("simSlot", simSlot - 1)
            intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", simSlot - 1)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to dial intent
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(dialIntent)
            } catch (_: Exception) {}
        }
    }

    private fun onCallEnded() {
        val state = _sessionState.value
        if (!state.isActive || state.isPaused) return

        // Check if reached max attempts
        if (state.maxAttempts > 0 && state.currentAttempt >= state.maxAttempts) {
            finishSession("Completed target of ${state.maxAttempts} attempts")
            return
        }

        // Start countdown to next attempt
        startCooldownCountdown(state.intervalSeconds)
    }

    private fun startCooldownCountdown(seconds: Int) {
        countdownJob?.cancel()
        _sessionState.update {
            it.copy(
                status = CallStatus.WAITING_NEXT,
                remainingCountdown = seconds,
                statusMessage = "Next retry in ${seconds}s..."
            )
        }

        countdownJob = scope.launch(Dispatchers.Main) {
            var remaining = seconds
            while (remaining > 0 && _sessionState.value.isActive && !_sessionState.value.isPaused) {
                _sessionState.update {
                    it.copy(
                        remainingCountdown = remaining,
                        statusMessage = "Next retry in ${remaining}s..."
                    )
                }
                delay(1000L)
                remaining--
            }

            if (_sessionState.value.isActive && !_sessionState.value.isPaused) {
                triggerDialAttempt()
            }
        }
    }

    private fun startCallDurationMonitor() {
        callDurationJob?.cancel()
        callDurationJob = scope.launch(Dispatchers.Main) {
            var elapsed = 0
            val limit = _sessionState.value.callDurationLimitSeconds
            while (_sessionState.value.isActive && !_sessionState.value.isPaused) {
                delay(1000L)
                elapsed++
                _sessionState.update { it.copy(currentCallElapsedSeconds = elapsed) }

                if (limit > 0 && elapsed >= limit) {
                    // Call duration limit reached
                    triggerVibration(250L)
                    onCallEnded()
                    break
                }
            }
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = scope.launch(Dispatchers.Default) {
            while (_sessionState.value.isActive) {
                delay(1000L)
                if (!_sessionState.value.isPaused) {
                    _sessionState.update {
                        it.copy(totalSessionElapsedSeconds = it.totalSessionElapsedSeconds + 1)
                    }
                }
            }
        }
    }

    fun pauseSession() {
        if (!_sessionState.value.isActive) return
        countdownJob?.cancel()
        callDurationJob?.cancel()
        _sessionState.update {
            it.copy(
                isPaused = true,
                status = CallStatus.PAUSED,
                statusMessage = "Endless Call paused"
            )
        }
    }

    fun resumeSession() {
        if (!_sessionState.value.isActive || !_sessionState.value.isPaused) return
        _sessionState.update {
            it.copy(
                isPaused = false,
                status = CallStatus.WAITING_NEXT,
                statusMessage = "Resuming..."
            )
        }
        startCooldownCountdown(_sessionState.value.remainingCountdown.coerceAtLeast(1))
    }

    fun redialImmediately() {
        if (!_sessionState.value.isActive) return
        countdownJob?.cancel()
        triggerDialAttempt()
    }

    fun simulateCallEndForTesting() {
        // Handy for emulator testing where cellular state doesn't transition
        if (_sessionState.value.isActive) {
            onCallEnded()
        }
    }

    fun stopEndlessCall(userCancelled: Boolean = true, connected: Boolean = false) {
        val current = _sessionState.value
        countdownJob?.cancel()
        sessionTimerJob?.cancel()
        callDurationJob?.cancel()

        if (current.isActive && current.currentAttempt > 0) {
            val finalStatus = when {
                connected -> "CONNECTED"
                userCancelled -> "STOPPED_BY_USER"
                current.maxAttempts > 0 && current.currentAttempt >= current.maxAttempts -> "COMPLETED"
                else -> "FINISHED"
            }
            saveSessionLog(current, finalStatus)
        }

        _sessionState.update {
            EndlessCallSessionState(
                isActive = false,
                status = CallStatus.IDLE,
                statusMessage = if (connected) "Connected! Endless loop stopped." else "Endless Call stopped"
            )
        }
    }

    private fun finishSession(reason: String) {
        val current = _sessionState.value
        countdownJob?.cancel()
        sessionTimerJob?.cancel()
        callDurationJob?.cancel()

        if (current.isActive && current.currentAttempt > 0) {
            saveSessionLog(current, "COMPLETED")
        }

        _sessionState.update {
            it.copy(
                isActive = false,
                status = CallStatus.FINISHED,
                statusMessage = reason
            )
        }
    }

    private fun saveSessionLog(session: EndlessCallSessionState, status: String) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.insertLog(
                    CallSessionLog(
                        phoneNumber = session.phoneNumber,
                        contactName = session.contactName,
                        attemptsMade = session.currentAttempt,
                        maxAttempts = session.maxAttempts,
                        intervalSeconds = session.intervalSeconds,
                        durationSeconds = session.totalSessionElapsedSeconds,
                        status = status,
                        simSlot = session.simSlot
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun toggleSpeakerphone(): Boolean {
        return try {
            val audio = audioManager ?: return false
            audio.mode = AudioManager.MODE_IN_COMMUNICATION
            val nextState = !audio.isSpeakerphoneOn
            audio.isSpeakerphoneOn = nextState
            _sessionState.update { it.copy(autoSpeaker = nextState) }
            nextState
        } catch (_: Exception) {
            false
        }
    }

    private fun enableSpeakerphone() {
        try {
            audioManager?.let { audio ->
                audio.mode = AudioManager.MODE_IN_COMMUNICATION
                audio.isSpeakerphoneOn = true
            }
        } catch (_: Exception) {}
    }

    private fun triggerVibration(milliseconds: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(milliseconds)
            }
        } catch (_: Exception) {}
    }
}
