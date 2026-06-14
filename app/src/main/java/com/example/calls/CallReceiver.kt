package com.example.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MAKE_CALL = "com.example.ACTION_MAKE_CALL"
        const val EXTRA_JOB_ID = "extra_job_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restore alarms
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val activeJobs = db.callJobDao().getActiveJobs()
                    activeJobs.forEach { job ->
                        CallManager.scheduleNextCall(context, job)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action == ACTION_MAKE_CALL) {
            val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
            if (jobId != -1L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val job = db.callJobDao().getJobById(jobId)
                        
                        if (job != null && job.isActive) {
                            // Execute the call
                            CallManager.makeCallNow(context, job)

                            // Update job state
                            val newCallsMade = job.callsMade + 1
                            if (newCallsMade >= job.totalCalls) {
                                // Job finished
                                db.callJobDao().update(job.copy(callsMade = newCallsMade, isActive = false))
                            } else {
                                // Schedule next iteration
                                val nextTime = System.currentTimeMillis() + (job.intervalMinutes * 60 * 1000L)
                                val nextJob = job.copy(callsMade = newCallsMade, nextCallTime = nextTime)
                                db.callJobDao().update(nextJob)
                                CallManager.scheduleNextCall(context, nextJob)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
