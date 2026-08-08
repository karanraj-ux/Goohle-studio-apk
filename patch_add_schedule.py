import re

with open('app/src/main/java/com/example/ui/screens/AddScheduleScreen.kt', 'r') as f:
    content = f.read()

launcher = """    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val selectedTime = Calendar.getInstance().apply {
                timeInMillis = dateMillis!!
                set(Calendar.HOUR_OF_DAY, timeState.hour)
                set(Calendar.MINUTE, timeState.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onSave(type, target, message, selectedTime)
        } else {
            // Can't save without permission
        }
    }
"""

content = content.replace('var showSmsPicker by remember { mutableStateOf(false) }', 'var showSmsPicker by remember { mutableStateOf(false) }\n' + launcher)

save_logic = """                                    val selectedTime = Calendar.getInstance().apply {
                                        timeInMillis = dateMillis!!
                                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                                        set(Calendar.MINUTE, timeState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    
                                    if (type == "SMS") {
                                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS)
                                        } else {
                                            onSave(type, target, message, selectedTime)
                                        }
                                    } else {
                                        onSave(type, target, message, selectedTime)
                                    }"""

content = re.sub(r'val selectedTime = Calendar\.getInstance\(\)\.apply \{.*?timeInMillis\n\s*onSave\(type, target, message, selectedTime\)', save_logic, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/AddScheduleScreen.kt', 'w') as f:
    f.write(content)

