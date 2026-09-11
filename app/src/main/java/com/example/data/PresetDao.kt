package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<QuickPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: QuickPreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<QuickPreset>)

    @Update
    suspend fun updatePreset(preset: QuickPreset)

    @Delete
    suspend fun deletePreset(preset: QuickPreset)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getCount(): Int
}
