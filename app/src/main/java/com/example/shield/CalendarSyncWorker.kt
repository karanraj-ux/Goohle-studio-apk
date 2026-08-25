package com.example.shield

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ShieldApplication
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CalendarSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
        val isSyncEnabled = settingsRepo.calendarSync.first()
        
        if (!isSyncEnabled) {
            return@withContext Result.success()
        }
        
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CalendarSyncWorker", "READ_CALENDAR permission not granted.")
            return@withContext Result.success() // Can't do anything without permission
        }

        try {
            val now = System.currentTimeMillis()
            
            // Build URI for instances happening RIGHT NOW (between now-1min and now+1min)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, now - 60000)
            android.content.ContentUris.appendId(builder, now + 60000)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
            )
            
            // Exclude "available" (transparent) events if possible. In some cases AVAILABILITY is on Events, not Instances.
            // For simplicity, we just check if any instance is active.
            val cursor = applicationContext.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )
            
            var hasActiveMeeting = false
            
            cursor?.use {
                while (it.moveToNext()) {
                    // Check if it's actually happening right now
                    val begin = it.getLong(1)
                    val end = it.getLong(2)
                    if (now in begin..end) {
                        hasActiveMeeting = true
                        break
                    }
                }
            }

            val isGhostModeActive = settingsRepo.ghostMode.first()
            val wasActivatedByCalendar = settingsRepo.calendarGhostModeActive.first()

            if (hasActiveMeeting) {
                if (!isGhostModeActive) {
                    Log.d("CalendarSyncWorker", "Active meeting found. Activating Ghost Mode.")
                    settingsRepo.updateBoolean(SettingsRepository.GHOST_MODE, true)
                    settingsRepo.updateBoolean(SettingsRepository.CALENDAR_GHOST_MODE_ACTIVE, true)
                }
            } else {
                // No active meeting. If WE turned it on, turn it off.
                if (wasActivatedByCalendar) {
                    Log.d("CalendarSyncWorker", "Meeting ended. Deactivating Ghost Mode.")
                    settingsRepo.updateBoolean(SettingsRepository.GHOST_MODE, false)
                    settingsRepo.updateBoolean(SettingsRepository.CALENDAR_GHOST_MODE_ACTIVE, false)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CalendarSyncWorker", "Error syncing calendar", e)
            Result.failure()
        }
    }
}
