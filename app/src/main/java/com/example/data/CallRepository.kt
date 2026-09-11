package com.example.data

import kotlinx.coroutines.flow.Flow

class CallRepository(
    private val callDao: CallDao,
    private val presetDao: PresetDao
) {
    val allLogs: Flow<List<CallSessionLog>> = callDao.getAllLogs()
    val allPresets: Flow<List<QuickPreset>> = presetDao.getAllPresets()

    suspend fun insertLog(log: CallSessionLog): Long = callDao.insertLog(log)
    suspend fun deleteLog(log: CallSessionLog) = callDao.deleteLog(log)
    suspend fun clearAllLogs() = callDao.clearAllLogs()

    suspend fun insertPreset(preset: QuickPreset): Long = presetDao.insertPreset(preset)
    suspend fun updatePreset(preset: QuickPreset) = presetDao.updatePreset(preset)
    suspend fun deletePreset(preset: QuickPreset) = presetDao.deletePreset(preset)
    suspend fun getPresetCount(): Int = presetDao.getCount()
}
