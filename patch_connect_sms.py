import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

sms_launcher = """    val smsForwardPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            settingsViewModel.updateSmsForwardingEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("SMS sending permission is required to forward messages") }
        }
    }
"""

if "val smsForwardPermissionLauncher" not in content:
    content = content.replace('val contactsPermissionLauncher =', sms_launcher + '\n    val contactsPermissionLauncher =')

toggle_sms = """                        Switch(
                            checked = settingsState.smsForwardingEnabled,
                            onCheckedChange = { isChecked -> 
                                if (isChecked) {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        smsForwardPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    } else {
                                        settingsViewModel.updateSmsForwardingEnabled(true)
                                    }
                                } else {
                                    settingsViewModel.updateSmsForwardingEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )"""

content = re.sub(r'Switch\(\n\s*checked = settingsState\.smsForwardingEnabled,\n\s*onCheckedChange = \{ settingsViewModel\.updateSmsForwardingEnabled\(it\) \},\n\s*colors = SwitchDefaults\.colors\(checkedTrackColor = MaterialTheme\.colorScheme\.primary\)\n\s*\)', toggle_sms, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)

