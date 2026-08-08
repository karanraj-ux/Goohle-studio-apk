import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

launcher = """    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            settingsViewModel.updateAutoForwardCalls(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Call permission is required to forward calls") }
        }
    }
"""

if "val callPermissionLauncher" not in content:
    content = content.replace('val contactsPermissionLauncher =', launcher + '\n    val contactsPermissionLauncher =')

toggle_logic = """                                        onCheckedChange = { isChecked -> 
                                            if (isChecked) {
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                                } else {
                                                    settingsViewModel.updateAutoForwardCalls(true)
                                                }
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(false)
                                            }
                                        }"""

content = re.sub(r'onCheckedChange = \{ settingsViewModel\.updateAutoForwardCalls\(it\) \}', toggle_logic, content)

row_clickable = """modifier = Modifier.clickable { 
                                        if (!settingsState.autoForwardCalls) {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(true)
                                            }
                                        } else {
                                            settingsViewModel.updateAutoForwardCalls(false)
                                        }
                                    }.padding(16.dp).fillMaxWidth()"""

content = re.sub(r'modifier = Modifier\.clickable \{ settingsViewModel\.updateAutoForwardCalls\(!settingsState\.autoForwardCalls\) \}\.padding\(16\.dp\)\.fillMaxWidth\(\)', row_clickable, content)

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)

