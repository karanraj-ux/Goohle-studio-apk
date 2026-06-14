package com.example.calls

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.CallJobEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CallManager {

    fun scheduleNextCall(context: Context, job: CallJobEntity) {
        if (!job.isActive || job.callsMade >= job.totalCalls) {
            updateWidget(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CallReceiver::class.java).apply {
            action = CallReceiver.ACTION_MAKE_CALL
            putExtra(CallReceiver.EXTRA_JOB_ID, job.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            job.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        job.nextCallTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        job.nextCallTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    job.nextCallTime,
                    pendingIntent
                )
            }
            updateWidget(context)
        } catch (e: SecurityException) {
            Log.e("CallManager", "Cannot schedule exact alarm", e)
        }
    }

    private fun updateWidget(context: Context) {
        // Trigger widget update that shows the upcoming calls
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val activeJobs = db.callJobDao().getActiveJobs()
            if (activeJobs.isNotEmpty()) {
                val nextJob = activeJobs.first()
                com.example.widget.WidgetUpdater.updateWidgetState(
                    context, 
                    "DEFAULT", 
                    "Next Call: ${nextJob.phoneNumber} at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(nextJob.nextCallTime))}"
                )
            } else {
                com.example.widget.WidgetUpdater.updateWidgetState(context, "DEFAULT", "No upcoming calls")
            }
        }
    }

    fun cancelJobAalrm(context: Context, jobId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CallReceiver::class.java).apply {
            action = CallReceiver.ACTION_MAKE_CALL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            jobId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        updateWidget(context)
    }

    @SuppressLint("MissingPermission")
    fun makeCallNow(context: Context, job: CallJobEntity) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Ensure channel exists
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "call_channel",
                    "Scheduled Calls",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for scheduled auto-calls"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${job.phoneNumber}")
            }
            val callPendingIntent = PendingIntent.getActivity(
                context,
                job.id.toInt(),
                callIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Using simple text without description if blank
            val descText = if (job.description.isNotBlank()) job.description else "Auto-call to ${job.phoneNumber}"

            val builder = androidx.core.app.NotificationCompat.Builder(context, "call_channel")
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("Scheduled Call Ready")
                .setContentText(descText)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .addAction(android.R.drawable.sym_action_call, "Call Now", callPendingIntent)
                .setFullScreenIntent(callPendingIntent, true)

            notificationManager.notify(job.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("CallManager", "Missing CALL_PHONE permission", e)
        } catch (e: Exception) {
            Log.e("CallManager", "Error making call", e)
        }
    }
}
