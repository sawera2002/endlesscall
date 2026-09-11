package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class QuickPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val phoneNumber: String,
    val isEndless: Boolean = true,
    val maxAttempts: Int = 20,
    val intervalSeconds: Int = 3,
    val callDurationLimitSeconds: Int = 0, // 0 = no limit
    val autoSpeaker: Boolean = true,
    val category: String = "Hotline",
    val iconIndex: Int = 0
)
