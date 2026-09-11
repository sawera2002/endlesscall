package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallSessionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String = "",
    val attemptsMade: Int,
    val maxAttempts: Int, // -1 for endless
    val intervalSeconds: Int,
    val durationSeconds: Long,
    val status: String, // CONNECTED, STOPPED_BY_USER, COMPLETED, TIMEOUT
    val simSlot: Int = 0, // 0 = Default, 1 = SIM 1, 2 = SIM 2
    val timestamp: Long = System.currentTimeMillis()
)
