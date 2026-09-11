package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.CallStatus
import com.example.service.EndlessCallSessionState
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.CallActiveGreen
import com.example.ui.theme.CallHangupRed
import com.example.ui.theme.DeepPurpleBorder
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.PurpleButtonGradient
import com.example.ui.theme.PurpleHeroGradient
import com.example.ui.theme.PurpleStopGradient
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

@Composable
fun ActiveCallDialog(
    session: EndlessCallSessionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRedialNow: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSimulateCallEnd: () -> Unit,
    onConnected: () -> Unit,
    onStop: () -> Unit
) {
    if (!session.isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = { /* Prevent accidental dismissal during active loop */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepPurpleDark.copy(alpha = 0.96f)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DeepPurpleCard, VioletElectric.copy(alpha = 0.3f))
                                )
                            )
                            .border(1.dp, MagentaNeon.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (session.maxAttempts > 0) "ENDLESS REDIAL RUNNING" else "INFINITE (∞) ENDLESS MODE",
                            color = MagentaNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (session.contactName.isNotBlank()) session.contactName else "Target Number",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = session.phoneNumber,
                        color = TextSecondary,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Middle: Glowing Radar Dial Visualizer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(220.dp)
                    ) {
                        // Outer pulse ring
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .scale(if (session.status == CallStatus.DIALING || session.status == CallStatus.IN_CALL) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            if (session.status == CallStatus.IN_CALL) CallActiveGreen.copy(alpha = 0.25f)
                                            else VioletElectric.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (session.status == CallStatus.IN_CALL) CallActiveGreen.copy(alpha = 0.4f)
                                    else MagentaNeon.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )

                        // Middle card ring
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(DeepPurpleCard)
                                .border(2.dp, DeepPurpleBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Active call",
                                    tint = if (session.status == CallStatus.IN_CALL) CallActiveGreen else MagentaNeon,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "#${session.currentAttempt}",
                                    color = TextPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status and attempt counter
                    Text(
                        text = if (session.maxAttempts > 0) {
                            "Attempt ${session.currentAttempt} of ${session.maxAttempts}"
                        } else {
                            "Attempt ${session.currentAttempt} of ∞ (Endless)"
                        },
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = session.statusMessage,
                        color = when (session.status) {
                            CallStatus.IN_CALL -> CallActiveGreen
                            CallStatus.WAITING_NEXT -> AlertOrange
                            CallStatus.PAUSED -> AlertOrange
                            else -> TextSecondary
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Cooldown Progress Bar
                    if (session.status == CallStatus.WAITING_NEXT && session.intervalSeconds > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = 1f - (session.remainingCountdown.toFloat() / session.intervalSeconds.toFloat())
                        Column(
                            modifier = Modifier.width(200.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = VioletElectric,
                                trackColor = DeepPurpleCard
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Next attempt in ${session.remainingCountdown}s",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Session stats badge card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepPurpleCard.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SESSION TIME", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatSeconds(session.totalSessionElapsedSeconds),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("INTERVAL", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${session.intervalSeconds}s",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SPEAKER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (session.autoSpeaker) "ON" else "OFF",
                                    color = if (session.autoSpeaker) CallActiveGreen else TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Bottom Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Action Row: Pause, Redial Now, Speaker, Sim Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume
                        FilledTonalButton(
                            onClick = if (session.isPaused) onResume else onPause,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DeepPurpleCard,
                                contentColor = TextPrimary
                            ),
                            shape = CircleShape,
                            modifier = Modifier.testTag("btn_pause_resume")
                        ) {
                            Icon(
                                imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (session.isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (session.isPaused) "Resume" else "Pause")
                        }

                        // Redial Immediately
                        FilledTonalButton(
                            onClick = onRedialNow,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DeepPurpleCard,
                                contentColor = VioletElectric
                            ),
                            shape = CircleShape,
                            modifier = Modifier.testTag("btn_redial_now")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Redial Now",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Redial Now")
                        }

                        // Toggle Speakerphone
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (session.autoSpeaker) VioletElectric.copy(alpha = 0.3f) else DeepPurpleCard)
                                .border(1.dp, DeepPurpleBorder, CircleShape)
                                .testTag("btn_toggle_speaker")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Toggle Speaker",
                                tint = if (session.autoSpeaker) MagentaNeon else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // "I'm Connected! (Stop Loop)" button
                    Button(
                        onClick = onConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_call_connected"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CallActiveGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Call Connected! Stop Endless Redial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Stop Button
                    Button(
                        onClick = onStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_stop_endless_call"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CallHangupRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Stop",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STOP ENDLESS CALL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Emulator / test helper button
                    OutlinedButton(
                        onClick = onSimulateCallEnd,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DeepPurpleBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("btn_simulate_call_end")
                    ) {
                        Text("Simulate Call Disconnect (Test Next Retry)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
