package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CallSessionLog::class, QuickPreset::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "endless_call_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialPresets(database.presetDao())
                    }
                }
            }

            suspend fun populateInitialPresets(presetDao: PresetDao) {
                val initialPresets = listOf(
                    QuickPreset(
                        title = "Emergency & Roadside Help",
                        phoneNumber = "911",
                        isEndless = true,
                        maxAttempts = -1,
                        intervalSeconds = 3,
                        callDurationLimitSeconds = 0,
                        autoSpeaker = true,
                        category = "Emergency",
                        iconIndex = 0
                    ),
                    QuickPreset(
                        title = "Telecom Customer Service",
                        phoneNumber = "121",
                        isEndless = true,
                        maxAttempts = 50,
                        intervalSeconds = 4,
                        callDurationLimitSeconds = 0,
                        autoSpeaker = true,
                        category = "Customer Care",
                        iconIndex = 1
                    ),
                    QuickPreset(
                        title = "Doctor & Clinic Appointment",
                        phoneNumber = "5550199",
                        isEndless = true,
                        maxAttempts = 30,
                        intervalSeconds = 3,
                        callDurationLimitSeconds = 0,
                        autoSpeaker = false,
                        category = "Medical",
                        iconIndex = 2
                    ),
                    QuickPreset(
                        title = "Radio Contest / Ticket Line",
                        phoneNumber = "5550144",
                        isEndless = true,
                        maxAttempts = 100,
                        intervalSeconds = 2,
                        callDurationLimitSeconds = 0,
                        autoSpeaker = true,
                        category = "Hotline",
                        iconIndex = 3
                    )
                )
                presetDao.insertPresets(initialPresets)
            }
        }
    }
}
