import re

with open('app/src/main/java/com/example/shield/ScheduledTaskWorker.kt', 'r') as f:
    content = f.read()

log_insertion = """                    val appDb = (applicationContext as ShieldApplication).container.database
                    appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                        timestamp = System.currentTimeMillis(),
                        sender = "Schedule",
                        message = task.message ?: "",
                        targetNumber = task.target,
                        status = "SUCCESS"
                    ))"""

content = content.replace('smsManager.sendTextMessage(task.target, null, task.message ?: "", null, null)\n                    Log.d("ScheduledTaskWorker", "Sent scheduled SMS to ${task.target}")',
                          'smsManager.sendTextMessage(task.target, null, task.message ?: "", null, null)\n                    Log.d("ScheduledTaskWorker", "Sent scheduled SMS to ${task.target}")\n' + log_insertion)

with open('app/src/main/java/com/example/shield/ScheduledTaskWorker.kt', 'w') as f:
    f.write(content)
