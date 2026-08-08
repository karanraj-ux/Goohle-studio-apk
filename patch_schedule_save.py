import re

with open('app/src/main/java/com/example/ui/screens/AddScheduleScreen.kt', 'r') as f:
    content = f.read()

launcher = """    val sendSmsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            if (target.isNotBlank() && selectedDateMillis != null && selectedHour != null) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis!!
                    set(Calendar.HOUR_OF_DAY, selectedHour!!)
                    set(Calendar.MINUTE, selectedMinute!!)
                    set(Calendar.SECOND, 0)
                }
                onSave(type, target, if (type != "Call") message else null, cal.timeInMillis)
            }
        }
    }
"""

content = content.replace('    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->', launcher + '    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->')


save_logic = """                        onClick = {
                            if (target.isNotBlank() && selectedDateMillis != null && selectedHour != null) {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis!!
                                    set(Calendar.HOUR_OF_DAY, selectedHour!!)
                                    set(Calendar.MINUTE, selectedMinute!!)
                                    set(Calendar.SECOND, 0)
                                }
                                
                                if (type == "SMS") {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                        sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    } else {
                                        onSave(type, target, message, cal.timeInMillis)
                                    }
                                } else {
                                    onSave(type, target, if (type != "Call") message else null, cal.timeInMillis)
                                }
                            }
                        },"""

content = re.sub(r'onClick = \{\s*if \(target\.isNotBlank\(\).*?onSave.*?cal\.timeInMillis\)\s*\}\s*\},', save_logic, content, flags=re.DOTALL)


with open('app/src/main/java/com/example/ui/screens/AddScheduleScreen.kt', 'w') as f:
    f.write(content)

