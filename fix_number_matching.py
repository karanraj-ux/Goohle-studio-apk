import re

# Fix SmsProcessor.kt
with open("app/src/main/java/com/example/SmsProcessor.kt", "r") as f:
    sms_content = f.read()

old_sender_matches = """val senderMatches = senders.isEmpty() || senders.any { sender.contains(it, ignoreCase = true) }"""
new_sender_matches = """val cleanIncomingSender = sender.replace(Regex("[^0-9+]"), "")
        val senderMatches = senders.isEmpty() || senders.any { 
            val cleanSender = it.replace(Regex("[^0-9+]"), "")
            cleanSender.isNotEmpty() && (cleanIncomingSender.contains(cleanSender) || cleanSender.contains(cleanIncomingSender) || sender.contains(it, ignoreCase = true))
        }"""
sms_content = sms_content.replace(old_sender_matches, new_sender_matches)

with open("app/src/main/java/com/example/SmsProcessor.kt", "w") as f:
    f.write(sms_content)

# Fix CallHandlingManager.kt
with open("app/src/main/java/com/example/calls/CallHandlingManager.kt", "r") as f:
    call_content = f.read()

old_vip_match = """return vips.any { number.contains(it) || it.contains(number) }"""
new_vip_match = """val cleanIncoming = number.replace(Regex("[^0-9+]"), "")
        return vips.any { 
            val cleanVip = it.replace(Regex("[^0-9+]"), "")
            cleanVip.isNotEmpty() && (cleanIncoming.contains(cleanVip) || cleanVip.contains(cleanIncoming) || number.contains(it) || it.contains(number))
        }"""
call_content = call_content.replace(old_vip_match, new_vip_match)

# Fix DND Bypass Audio handling
old_dnd_audio = """            if (notificationManager.isNotificationPolicyAccessGranted) {
                originalInterruptionFilter = notificationManager.currentInterruptionFilter
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0)
            
            val maxAlarmVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)"""

new_dnd_audio = """            // We bypass DND natively by playing the ringtone on the ALARM stream.
            // We do not change ringerMode or interruptionFilter to avoid SecurityExceptions if permissions are missing.
            val maxAlarmVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)"""

call_content = call_content.replace(old_dnd_audio, new_dnd_audio)

old_restore = """            if (notificationManager.isNotificationPolicyAccessGranted) {
                originalInterruptionFilter?.let { notificationManager.setInterruptionFilter(it) }
            }
            
            vipRingtone?.stop()
            vipRingtone = null
            
            originalVolume = null
            originalAlarmVolume = null
            originalRingerMode = null
            originalInterruptionFilter = null"""

new_restore = """            vipRingtone?.stop()
            vipRingtone = null
            
            originalAlarmVolume = null"""
call_content = call_content.replace(old_restore, new_restore)

# Remove the ringer setting in restoreAudioState
old_restore_2 = """            originalVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
            originalAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
            originalRingerMode?.let { audioManager.ringerMode = it }"""
new_restore_2 = """            originalAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }"""
call_content = call_content.replace(old_restore_2, new_restore_2)

with open("app/src/main/java/com/example/calls/CallHandlingManager.kt", "w") as f:
    f.write(call_content)

