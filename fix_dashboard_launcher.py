import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

launcher_code = """    val notificationPolicyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            settingsViewModel.updateOverrideDnd(true)
        }
    }
"""

content = content.replace('    val contactPickerLauncher =', launcher_code + '    val contactPickerLauncher =')

# Replace the Unresolved reference 'Context' with android.content.Context
content = content.replace('context.getSystemService(Context.NOTIFICATION_SERVICE)', 'context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)')

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
