package com.example.ui.screens

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.DialerUiState
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.DeepPurpleBorder
import com.example.ui.theme.DeepPurpleCard
import com.example.ui.theme.DeepPurpleCardElevated
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.MagentaNeon
import com.example.ui.theme.PurpleButtonGradient
import com.example.ui.theme.PurpleHeroGradient
import com.example.ui.theme.PurpleVibrant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    state: DialerUiState,
    onPhoneNumberChange: (String) -> Unit,
    onContactSelected: (name: String, number: String) -> Unit,
    onToggleEndless: (Boolean) -> Unit,
    onMaxAttemptsChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onCallDurationLimitChange: (Int) -> Unit,
    onToggleSpeaker: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    onSimSlotChange: (Int) -> Unit,
    onScheduleChange: (Int) -> Unit,
    onCancelSchedule: () -> Unit,
    onStartCall: () -> Unit,
    onSaveAsPresetClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showDialpad by remember { mutableStateOf(false) }
    var showScheduleOptions by remember { mutableStateOf(false) }

    // Contact Picker
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        val id = if (idIndex >= 0) c.getString(idIndex) else null
                        val name = if (nameIndex >= 0) c.getString(nameIndex) else ""
                        val hasPhone = if (hasPhoneIndex >= 0) c.getInt(hasPhoneIndex) else 0

                        if (hasPhone > 0 && id != null) {
                            val pCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(id),
                                null
                            )
                            pCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIndex >= 0) {
                                        val number = pc.getString(numIndex)
                                        onContactSelected(name, number)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepPurpleDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Hero Card with Image & Purple Gradient Accent
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_call_hero),
                    contentDescription = "Endless Call banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentScale = ContentScale.Crop
                )
                // Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, DeepPurpleDark.copy(alpha = 0.85f))
                            )
                        )
                )

                // Title Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(MagentaNeon, VioletElectric))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Infinity loop",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENDLESS AUTO REDIAL",
                            color = MagentaNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Never miss a busy line again",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Target Phone Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET NUMBER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    Row {
                        // Pick Contact
                        TextButton(
                            onClick = { contactPicker.launch(null) },
                            modifier = Modifier.testTag("btn_pick_contact")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = "Contacts",
                                tint = VioletElectric,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Contacts", color = VioletElectric, fontSize = 13.sp)
                        }

                        // Save As Preset
                        if (state.phoneNumber.isNotBlank()) {
                            IconButton(
                                onClick = onSaveAsPresetClick,
                                modifier = Modifier.size(36.dp).testTag("btn_save_preset")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Save Preset",
                                    tint = MagentaNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (state.contactName.isNotBlank()) {
                    Text(
                        text = state.contactName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_phone_number"),
                    placeholder = { Text("e.g. +1 800 555 0199 or 911", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone icon",
                            tint = MagentaNeon
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.phoneNumber.isNotEmpty()) {
                                IconButton(onClick = { onPhoneNumberChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted
                                    )
                                }
                            }
                            IconButton(onClick = { showDialpad = !showDialpad }) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Toggle dialpad",
                                    tint = if (showDialpad) VioletElectric else TextMuted
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VioletElectric,
                        unfocusedBorderColor = DeepPurpleBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = MagentaNeon
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick Dialpad expansion
                AnimatedVisibility(visible = showDialpad) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        val keypadButtons = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("*", "0", "#")
                        )

                        for (row in keypadButtons) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (key in row) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(DeepPurpleCardElevated)
                                            .border(1.dp, DeepPurpleBorder, CircleShape)
                                            .clickable { onPhoneNumberChange(state.phoneNumber + key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Endless vs Limited Mode Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REDIAL MODE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Endless Mode Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (state.isEndless) Brush.horizontalGradient(listOf(PurpleVibrant, VioletElectric))
                                else Brush.horizontalGradient(listOf(DeepPurpleCardElevated, DeepPurpleCardElevated))
                            )
                            .border(
                                1.dp,
                                if (state.isEndless) MagentaNeon else DeepPurpleBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onToggleEndless(true) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Endless",
                                tint = if (state.isEndless) Color.White else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Endless (∞)",
                                color = if (state.isEndless) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Limited Target Attempts Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (!state.isEndless) Brush.horizontalGradient(listOf(PurpleVibrant, VioletElectric))
                                else Brush.horizontalGradient(listOf(DeepPurpleCardElevated, DeepPurpleCardElevated))
                            )
                            .border(
                                1.dp,
                                if (!state.isEndless) MagentaNeon else DeepPurpleBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onToggleEndless(false) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Limit",
                                tint = if (!state.isEndless) Color.White else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fixed Limit",
                                color = if (!state.isEndless) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // If Fixed Limit is selected, show attempt chips
                AnimatedVisibility(visible = !state.isEndless) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attempts: ${state.maxAttempts}",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 20, 50, 100).forEach { attempts ->
                                FilterChip(
                                    selected = state.maxAttempts == attempts,
                                    onClick = { onMaxAttemptsChange(attempts) },
                                    label = { Text("${attempts}x") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VioletElectric,
                                        selectedLabelColor = Color.White,
                                        containerColor = DeepPurpleCardElevated,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }

                        Slider(
                            value = state.maxAttempts.toFloat(),
                            onValueChange = { onMaxAttemptsChange(it.toInt()) },
                            valueRange = 1f..150f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = MagentaNeon,
                                activeTrackColor = VioletElectric,
                                inactiveTrackColor = DeepPurpleBorder
                            )
                        )
                    }
                }
            }
        }

        // Interval & Delay Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTERVAL BETWEEN REDIALS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    Text(
                        text = "${state.intervalSeconds} seconds",
                        color = MagentaNeon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Pause duration before triggering the next automatic redial",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                // Quick Interval Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 5, 10, 15).forEach { sec ->
                        FilterChip(
                            selected = state.intervalSeconds == sec,
                            onClick = { onIntervalChange(sec) },
                            label = { Text("${sec}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletElectric,
                                selectedLabelColor = Color.White,
                                containerColor = DeepPurpleCardElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Slider(
                    value = state.intervalSeconds.toFloat(),
                    onValueChange = { onIntervalChange(it.toInt()) },
                    valueRange = 1f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = MagentaNeon,
                        activeTrackColor = VioletElectric,
                        inactiveTrackColor = DeepPurpleBorder
                    )
                )
            }
        }

        // Advanced Options & Hardware Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CALL AUTOMATION OPTIONS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Speakerphone Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speakerphone",
                            tint = MagentaNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Auto Speakerphone", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Turn on speaker when call connects", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = state.autoSpeaker,
                        onCheckedChange = onToggleSpeaker,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VioletElectric
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vibrate on Dial Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibrate",
                            tint = MagentaNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Vibrate on Retry", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Haptic pulse before next call attempt", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = state.vibrateOnDial,
                        onCheckedChange = onToggleVibrate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VioletElectric
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dual SIM Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = "SIM card",
                            tint = MagentaNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SIM Slot Selection", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Target SIM for dialing", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Default" to 0, "SIM 1" to 1, "SIM 2" to 2).forEach { (label, slot) ->
                            FilterChip(
                                selected = state.simSlot == slot,
                                onClick = { onSimSlotChange(slot) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletElectric,
                                    selectedLabelColor = Color.White,
                                    containerColor = DeepPurpleCardElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Call Duration Auto Hangup Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Call timer",
                            tint = MagentaNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Call Duration Limit", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (state.callDurationLimit == 0) "Unlimited" else "${state.callDurationLimit} seconds",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Off" to 0, "30s" to 30, "60s" to 60).forEach { (label, limit) ->
                            FilterChip(
                                selected = state.callDurationLimit == limit,
                                onClick = { onCallDurationLimitChange(limit) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletElectric,
                                    selectedLabelColor = Color.White,
                                    containerColor = DeepPurpleCardElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Schedule Call Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepPurpleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Schedule",
                            tint = MagentaNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Schedule Call", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (state.isScheduled) "Starts in ${state.scheduledSecondsRemaining}s" else "Delayed start timer",
                                color = if (state.isScheduled) AlertOrange else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (state.isScheduled) {
                        TextButton(onClick = onCancelSchedule) {
                            Text("Cancel", color = AlertOrange)
                        }
                    } else {
                        IconButton(onClick = { showScheduleOptions = !showScheduleOptions }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Schedule options",
                                tint = TextMuted
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showScheduleOptions && !state.isScheduled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Now" to 0, "In 1m" to 1, "In 5m" to 5, "In 15m" to 15).forEach { (label, min) ->
                            FilterChip(
                                selected = state.scheduledMinutes == min,
                                onClick = { onScheduleChange(min) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletElectric,
                                    selectedLabelColor = Color.White,
                                    containerColor = DeepPurpleCardElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Primary START ENDLESS CALL Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (state.phoneNumber.isNotBlank()) PurpleButtonGradient
                    else Brush.horizontalGradient(listOf(DeepPurpleBorder, DeepPurpleBorder))
                )
                .clickable(enabled = state.phoneNumber.isNotBlank()) { onStartCall() }
                .testTag("btn_start_endless_call"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Start call",
                    tint = if (state.phoneNumber.isNotBlank()) Color.White else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state.isScheduled && state.scheduledMinutes > 0) "SCHEDULE ENDLESS CALL" else "START ENDLESS CALL",
                    color = if (state.phoneNumber.isNotBlank()) Color.White else TextMuted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Endless icon",
                    tint = if (state.phoneNumber.isNotBlank()) MagentaNeon else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
