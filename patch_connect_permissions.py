import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

launcher = """    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true && permissions[Manifest.permission.ANSWER_PHONE_CALLS] == true) {
            settingsViewModel.updateAutoForwardCalls(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Call permissions are required to forward calls") }
        }
    }
"""

# Replace the previous callPermissionLauncher
content = re.sub(r'val callPermissionLauncher = rememberLauncherForActivityResult\(ActivityResultContracts\.RequestPermission\(\)\).*?\}\n    \}', launcher, content, flags=re.DOTALL)

toggle_logic = """                                        onCheckedChange = { isChecked -> 
                                            if (isChecked) {
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS))
                                                } else {
                                                    settingsViewModel.updateAutoForwardCalls(true)
                                                }
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(false)
                                            }
                                        }"""

content = re.sub(r'onCheckedChange = \{ isChecked -> .*?\}\n                                        \}', toggle_logic, content, flags=re.DOTALL)

row_clickable = """modifier = Modifier.clickable { 
                                        if (!settingsState.autoForwardCalls) {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS))
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(true)
                                            }
                                        } else {
                                            settingsViewModel.updateAutoForwardCalls(false)
                                        }
                                    }.padding(16.dp).fillMaxWidth()"""

content = re.sub(r'modifier = Modifier\.clickable \{ \n                                        if \(!settingsState\.autoForwardCalls\).*?\.padding\(16\.dp\)\.fillMaxWidth\(\)', row_clickable, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)

