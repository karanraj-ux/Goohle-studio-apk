import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

bad_block = """    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {"""

good_block = """    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            targetPhonePickerLauncher.launch(null)
        } else {"""

content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)
