import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

launcher_code = """    val notificationPolicyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            settingsViewModel.updateOverrideDnd(true)
        }
    }
"""

if "val notificationPolicyLauncher" not in content:
    content = content.replace('val requestPermissionLauncher =', launcher_code + '\n    val requestPermissionLauncher =')

switch_code = """                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        if (!nm.isNotificationPolicyAccessGranted) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            notificationPolicyLauncher.launch(intent)
                                        } else {
                                            settingsViewModel.updateOverrideDnd(true)
                                        }
                                    } else {
                                        settingsViewModel.updateOverrideDnd(false)
                                    }
                                }"""

content = re.sub(r'onCheckedChange = \{ settingsViewModel\.updateOverrideDnd\(it\) \}', switch_code, content)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
