package com.example.shield

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.ShieldApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledTaskWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getInt("taskId", -1)
        if (taskId == -1) return@withContext Result.failure()

        val repo = (applicationContext as ShieldApplication).container.scheduledTaskRepository
        val task = repo.getTaskById(taskId) ?: return@withContext Result.failure()

        if (task.completed) return@withContext Result.success()

        try {
            when (task.type) {
                "SMS" -> {
                    val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        applicationContext.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION") SmsManager.getDefault()
                    }
                    smsManager.sendTextMessage(task.target, null, task.message ?: "", null, null)
                    Log.d("ScheduledTaskWorker", "Sent scheduled SMS to ${task.target}")
                }
                "Call" -> {
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = Uri.parse("tel:${task.target}")
                    showTapToLaunchNotification(applicationContext, "Scheduled Call", "Tap to call ${task.target}", intent, taskId)
                    Log.d("ScheduledTaskWorker", "Posted call notification for ${task.target}")
                }
                "WhatsApp" -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://wa.me/${task.target}?text=${Uri.encode(task.message ?: "")}")
                    showTapToLaunchNotification(applicationContext, "Scheduled WhatsApp", "Tap to send message to ${task.target}", intent, taskId)
                    Log.d("ScheduledTaskWorker", "Posted WhatsApp notification for ${task.target}")
                }
            }

            repo.markCompleted(taskId)
            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduledTaskWorker", "Failed to execute task", e)
            Result.failure()
        }
    }

    private fun showTapToLaunchNotification(context: Context, title: String, content: String, intent: Intent, notificationId: Int) {
        val channelId = "scheduled_tasks"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Scheduled Tasks", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }
}
