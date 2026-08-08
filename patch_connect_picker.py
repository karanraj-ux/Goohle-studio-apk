import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

# Add a permission launcher for READ_CONTACTS
permission_launcher = """
    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            targetPhonePickerLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Contacts permission is required to pick a number") }
        }
    }
"""

if "val contactsPermissionLauncher" not in content:
    # insert before targetPhonePickerLauncher
    content = content.replace('val targetPhonePickerLauncher =', permission_launcher + '\n    val targetPhonePickerLauncher =')

# replace targetPhonePickerLauncher.launch(null) with contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
content = content.replace('targetPhonePickerLauncher.launch(null)', 'contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)')

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)
